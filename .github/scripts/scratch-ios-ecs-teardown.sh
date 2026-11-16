#!/bin/bash
#
# Scratch diagnostics for the aws-config iOS simulator test binary hanging at exit. Runs each test class in its own
# process on a booted simulator, then the whole binary once. Any run still alive after $HANG_LIMIT_SECS is attached with
# lldb to print the CRT's unjoined managed thread count and every thread's stack. Output goes under $OUT_DIR.
# Run from the aws-sdk-kotlin root after linking :aws-runtime:aws-config:linkDebugTestIosSimulatorArm64. Boots the
# simulator itself. Written for macOS's bash 3.2.

set -u

kexe=$PWD/aws-runtime/aws-config/build/bin/iosSimulatorArm64/debugTest/test.kexe
device=
out=${OUT_DIR:-$PWD/build/scratch-ios}
limit=${HANG_LIMIT_SECS:-90}

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

# The count is a file-static C variable without debug info, so read it through its symbol's load address
read_count_py="script s = lldb.target.FindSymbols('s_unjoined_thread_count')[0].GetSymbol(); \
a = s.GetStartAddress().GetLoadAddress(lldb.target); e = lldb.SBError(); \
print('s_unjoined_thread_count =', lldb.process.ReadUnsignedFromMemory(a, 4, e), e)"

dump_hung_binary() {
  local name=$1 pid
  pid=$(pgrep -nx test.kexe)
  if [[ -z $pid ]]; then
    log "$name: no test.kexe process found"
    return
  fi
  sudo lldb --batch -p "$pid" \
    -O "settings set interpreter.stop-command-source-on-error false" \
    -o "image lookup -s s_unjoined_thread_count" \
    -o "$read_count_py" \
    -o "thread backtrace all" \
    -o "detach" > "$out/$name-lldb.txt" 2>&1
  log "$name: $(grep -m1 "s_unjoined_thread_count =" "$out/$name-lldb.txt" || echo "count not read")"
  grep -E "frame #[0-9]+: .*(aws_|kfun:aws\.sdk\.kotlin\.crt)" "$out/$name-lldb.txt" | sed -E 's/^ *//' | cut -c1-200 | head -40
  kill -9 "$pid" 2>/dev/null
}

# Runs the test binary on the simulator: run_spawn <name> <ktest args...>. Returns 1 if it hung.
run_spawn() {
  local name=$1 pid rc
  shift
  xcrun simctl spawn "$device" "$kexe" --ktest_logger=SIMPLE "$@" > "$out/$name.log" 2>&1 &
  pid=$!
  if wait_for_exit "$pid" "$limit"; then
    wait "$pid"
    rc=$?
    log "$name: exited rc=$rc ($(grep -c "^Passed" "$out/$name.log") passed, $(grep -c "^Failed" "$out/$name.log") failed)"
    return 0
  fi
  echo "::group::[scratch-ios] $name HUNG after ${limit}s"
  tail -3 "$out/$name.log"
  dump_hung_binary "$name"
  echo "::endgroup::"
  return 1
}

sudo DevToolsSecurity -enable > /dev/null 2>&1 || log "DevToolsSecurity -enable failed"

xcrun simctl spawn "$device" "$kexe" --ktest_list_tests > "$out/tests.txt" 2>&1
classes=$(grep -E '^[A-Za-z0-9_.]+\.$' "$out/tests.txt" | sed 's/\.$//')
log "$(echo "$classes" | grep -c .) test classes"

hung=""
for class in $classes; do
  short=${class##*.}
  run_spawn "class-$short" "--ktest_filter=$class.*" || hung="$hung $short"
done

log "classes that hung:${hung:- none}"

run_spawn all || true
