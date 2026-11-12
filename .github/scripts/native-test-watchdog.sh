#!/bin/bash
#
# Prints diagnostics into the job log when a Kotlin/Native test binary runs longer than expected, and periodically
# prints thread dumps of Gradle's JVMs, so a hung CI job leaves evidence of where it's stuck. Run it in the background
# of a build step on Linux or macOS and kill it when the step finishes.
#
# Environment overrides (all values in seconds):
#   WATCHDOG_POLL_SECS          how often to look for long-running test binaries (default 60)
#   WATCHDOG_TEST_MAX_SECS      age after which a test binary is reported (default 300)
#   WATCHDOG_GRADLE_FIRST_SECS  delay before the first scheduled Gradle thread dump (default 2700)
#   WATCHDOG_GRADLE_EVERY_SECS  interval between later scheduled Gradle thread dumps (default 3600)

set -u

poll_secs=${WATCHDOG_POLL_SECS:-60}
test_max_secs=${WATCHDOG_TEST_MAX_SECS:-300}
gradle_first_secs=${WATCHDOG_GRADLE_FIRST_SECS:-2700}
gradle_every_secs=${WATCHDOG_GRADLE_EVERY_SECS:-3600}

os=$(uname -s)

log() {
  echo "[native-test-watchdog] $*"
}

# Converts `ps -o etime` output ([[dd-]hh:]mm:ss) to seconds
etime_to_secs() {
  local etime=$1 days=0 hours=0 minutes=0 seconds=0
  if [[ $etime == *-* ]]; then
    days=${etime%%-*}
    etime=${etime#*-}
  fi
  IFS=: read -r -a parts <<< "$etime"
  case ${#parts[@]} in
    3) hours=${parts[0]}; minutes=${parts[1]}; seconds=${parts[2]} ;;
    2) minutes=${parts[0]}; seconds=${parts[1]} ;;
    *) seconds=${parts[0]} ;;
  esac
  echo $(( 10#$days * 86400 + 10#$hours * 3600 + 10#$minutes * 60 + 10#$seconds ))
}

process_age_secs() {
  local etime
  etime=$(ps -o etime= -p "$1" 2>/dev/null | tr -d ' ')
  [[ -n $etime ]] || return 1
  etime_to_secs "$etime"
}

print_process_tree() {
  local pid=$1 indent=${2:-} child
  ps -o pid=,ppid=,etime=,stat=,command= -p "$pid" 2>/dev/null | sed "s/^/$indent/"
  for child in $(pgrep -P "$pid" 2>/dev/null); do
    print_process_tree "$child" "$indent  "
  done
}

print_native_stacks() {
  local pid=$1
  if [[ $os == Linux ]]; then
    sudo -n gdb -p "$pid" -batch -ex "set pagination off" -ex "thread apply all bt" 2>&1 ||
      gdb -p "$pid" -batch -ex "set pagination off" -ex "thread apply all bt" 2>&1
  else
    local report
    report=$(mktemp)
    if sample "$pid" 5 -mayDie -file "$report" > /dev/null 2>&1; then
      cat "$report"
    else
      sudo -n lldb --batch -p "$pid" -o "thread backtrace all" -o "detach" 2>&1
    fi
    rm -f "$report"
  fi
}

dump_test_binary() {
  local pid=$1 age=$2
  echo "::group::[native-test-watchdog] test binary $pid running for ${age}s"
  log "Process tree:"
  print_process_tree "$pid"
  log "Open file descriptors:"
  lsof -p "$pid" 2>&1 | awk 'NR == 1 || $4 ~ /^[0-9]/'
  log "Native stacks:"
  print_native_stacks "$pid"
  echo "::endgroup::"
}

dump_gradle() {
  local reason=$1 jstack=jstack pid
  [[ -n ${JAVA_HOME:-} && -x $JAVA_HOME/bin/jstack ]] && jstack=$JAVA_HOME/bin/jstack
  for pid in $(pgrep -f 'GradleDaemon|GradleWorkerMain|GradleMain|KotlinCompileDaemon' 2>/dev/null); do
    echo "::group::[native-test-watchdog] Gradle JVM $pid thread dump ($reason)"
    ps -o pid=,etime=,command= -p "$pid" 2>/dev/null | cut -c1-300
    "$jstack" "$pid" 2>&1
    echo "::endgroup::"
  done
}

if [[ $os == Linux ]] && ! command -v gdb > /dev/null; then
  log "Installing gdb"
  sudo -n apt-get install -y -qq gdb > /dev/null 2>&1 || log "Failed to install gdb; native stacks will be unavailable"
fi

log "Started (poll ${poll_secs}s, test binary limit ${test_max_secs}s, first Gradle dump after ${gradle_first_secs}s)"

next_gradle_dump=$(( $(date +%s) + gradle_first_secs ))
reported=" "

while true; do
  sleep "$poll_secs"

  for pid in $(pgrep -x test.kexe 2>/dev/null); do
    case $reported in *" $pid "*) continue ;; esac
    age=$(process_age_secs "$pid") || continue
    if (( age >= test_max_secs )); then
      dump_test_binary "$pid" "$age"
      dump_gradle "test binary $pid exceeded ${test_max_secs}s"
      reported+="$pid "
    fi
  done

  if (( $(date +%s) >= next_gradle_dump )); then
    dump_gradle "scheduled"
    next_gradle_dump=$(( $(date +%s) + gradle_every_secs ))
  fi
done
