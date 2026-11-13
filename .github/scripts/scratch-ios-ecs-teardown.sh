#!/bin/bash
#
# Scratch diagnostics for the aws-config iOS simulator test binary hanging at exit. Runs the linked test binary on a
# booted simulator three ways and saves everything under $OUT_DIR:
#   ecs-only     the two ECS tests that do real DNS, with CRT trace logs; dumps the managed thread count if it hangs
#   all-but-ecs  every other test, as a control
#   ecs-lldb     the two ECS tests under lldb, logging each event loop group create/shutdown/destroy step
# Run from the aws-sdk-kotlin root after linking :aws-runtime:aws-config:linkDebugTestIosSimulatorArm64. Boots the
# simulator itself. Written for macOS's bash 3.2.

set -u

kexe=$PWD/aws-runtime/aws-config/build/bin/iosSimulatorArm64/debugTest/test.kexe
device=
out=${OUT_DIR:-$PWD/build/scratch-ios}
limit=${HANG_LIMIT_SECS:-180}
ecs_filter='*.EcsCredentialsProviderTest.testNonLocalFullUri:*.EcsCredentialsProviderTest.testNonexistentFullUri'

mkdir -p "$out"

log() {
  echo "[scratch-ios] $*"
}

# Boot the named simulator, or the first available iPhone, and address it by UDID from here on
uuid_pattern='[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}'
if [[ -n ${IOS_SIMULATOR_DEVICE:-} ]]; then
  device=$(xcrun simctl list devices available | grep -m1 "$IOS_SIMULATOR_DEVICE (" | grep -oE "$uuid_pattern")
else
  device=$(xcrun simctl list devices available | grep -m1 "iPhone" | grep -oE "$uuid_pattern")
fi
if [[ -z $device ]]; then
  log "no matching available simulator"
  xcrun simctl list devices available
  exit 1
fi
log "using simulator: $(xcrun simctl list devices | grep -m1 "$device")"
xcrun simctl boot "$device" 2>/dev/null
xcrun simctl bootstatus "$device" -b > /dev/null 2>&1

# Returns 0 if process $1 exits within $2 seconds
wait_for_exit() {
  local pid=$1 secs=$2 i
  for ((i = 0; i < secs; i++)); do
    kill -0 "$pid" 2>/dev/null || return 0
    sleep 1
  done
  return 1
}

dump_hung_binary() {
  local name=$1 pid
  pid=$(pgrep -nx test.kexe)
  if [[ -z $pid ]]; then
    log "$name: no test.kexe process found"
    return
  fi
  log "$name: managed thread count and stacks of pid $pid"
  sudo lldb --batch -p "$pid" \
    -o "expr (unsigned int)s_unjoined_thread_count" \
    -o "memory read --size 4 --format u --count 1 &s_unjoined_thread_count" \
    -o "thread backtrace all" \
    -o "detach" > "$out/$name-lldb.txt" 2>&1
  grep -A2 "s_unjoined_thread_count" "$out/$name-lldb.txt"
  kill -9 "$pid" 2>/dev/null
}

# Runs the test binary on the simulator: run_spawn <name> <crt log level or empty> <ktest args...>
run_spawn() {
  local name=$1 level=$2 pid rc
  shift 2
  echo "::group::[scratch-ios] $name"
  if [[ -n $level ]]; then
    SIMCTL_CHILD_CRT_LOG_LEVEL=$level xcrun simctl spawn "$device" "$kexe" --ktest_logger=SIMPLE "$@" > "$out/$name.log" 2>&1 &
  else
    xcrun simctl spawn "$device" "$kexe" --ktest_logger=SIMPLE "$@" > "$out/$name.log" 2>&1 &
  fi
  pid=$!
  if wait_for_exit "$pid" "$limit"; then
    wait "$pid"
    rc=$?
    log "$name: exited rc=$rc"
  else
    log "$name: HUNG after ${limit}s"
    dump_hung_binary "$name"
  fi
  grep -E "^(Starting test case|Passed|Failed)" "$out/$name.log"
  log "$name: event loop and host resolver teardown lines"
  grep -E "\[(event-loop|dns|common-thread)\]" "$out/$name.log" |
    grep -iE "destroy|shut ?down|clean|join|terminat|killing" | sed -E 's/^.*\] - //' | head -200
  echo "::endgroup::"
}

run_lldb() {
  local name=ecs-lldb udid=$device pid
  echo "::group::[scratch-ios] $name"

  cat > "$out/$name.cmds" <<EOF
platform select ios-simulator
platform connect $udid
target create "$kexe"
breakpoint set --name aws_event_loop_group_new_internal --auto-continue true --command "bt 12"
breakpoint set --name s_aws_event_loop_group_shutdown_async --auto-continue true --command "bt 12"
breakpoint set --name s_complete_destroy --auto-continue true --command "bt 12"
breakpoint set --name s_dispatch_event_loop_final_destroy --auto-continue true --command "bt 8"
breakpoint set --name aws_thread_increment_unjoined_count --auto-continue true --command "bt 6"
breakpoint set --name aws_thread_decrement_unjoined_count --auto-continue true --command "bt 6"
breakpoint set --name aws_thread_join_all_managed --auto-continue true --command "expr (unsigned int)s_unjoined_thread_count"
process launch --shell-expand-args false -- --ktest_logger=SIMPLE '--ktest_filter=$ecs_filter'
EOF

  lldb --batch -s "$out/$name.cmds" > "$out/$name.log" 2>&1 &
  pid=$!
  if wait_for_exit "$pid" "$limit"; then
    wait "$pid"
    log "$name: lldb exited rc=$?"
  else
    log "$name: HUNG after ${limit}s"
    kill -9 "$pid" 2>/dev/null
    pkill -9 -x test.kexe 2>/dev/null
  fi
  grep -E "^\(lldb\)|stop reason|frame #[0-9]+:|s_unjoined_thread_count|\(unsigned int\)|exited with status|error:" "$out/$name.log" |
    cut -c1-200 | head -400
  echo "::endgroup::"
}

sudo DevToolsSecurity -enable > /dev/null 2>&1 || log "DevToolsSecurity -enable failed"

run_spawn ecs-only Trace "--ktest_filter=$ecs_filter"
run_spawn all-but-ecs "" "--ktest_filter=*-$ecs_filter"
run_lldb
