#!/usr/bin/env bash
# profile.sh - record the memory and CPU profile of a running OpenSILEX server and of its RDF4J/MongoDB containers.
#
#   profile.sh find                          JVM on $OPENSILEX_PORT (8666): main class, start time, checkout
#   profile.sh start [PID] [LABEL]           JFR (settings=profile) + GC log + samplers, one directory per run
#   profile.sh mark LABEL                    close the current phase (own JFR file) and start phase LABEL
#   profile.sh stop [--histogram] [--pmap]   dump JFR, stop samplers; --histogram forces a FULL GC (live objects)
#   profile.sh status                        active run, last sample
#
# Runs go to ${OPENSILEX_PROFILE_DIR:-~/memprofile}/<date>_<time>_<label>/, outside the repository.
# Analyse: python3 analyze.py <run> [--compare <run>]. JDK tools come from the server's own JDK, else $JAVA_HOME,
# else PATH. The JVM command line is never printed: it may carry passwords (-D...).
set -euo pipefail

ROOT=${OPENSILEX_PROFILE_DIR:-$HOME/memprofile}
PORT=${OPENSILEX_PORT:-8666}
CONTAINER_PORTS=${OPENSILEX_CONTAINER_PORTS:-8667 8668}   # RDF4J and MongoDB ports of the dev configuration
CONTAINERS=${OPENSILEX_CONTAINERS:-rdf4j|mongo}           # fallback name pattern when no container publishes them
INTERVAL=${OPENSILEX_SAMPLE_SECONDS:-2}
RECORDING=opensilex-profile
ACTIVE="$ROOT/.active"
SELF=$(readlink -f "$0")

die() { echo "profile.sh: $*" >&2; exit 1; }
now() { date +%Y-%m-%dT%H:%M:%S; }
field() { sed -n "s/^$2=//p" "$1/run.txt" | head -1; }   # field RUN KEY

server_pid() {  # empty when nothing listens on $PORT
  ss -ltnpH "sport = :$PORT" 2>/dev/null | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2 || true
}

jdk_bin() {  # directory holding jcmd/jstat/jfr that match the JVM $1
  local home dir
  home=$(dirname "$(dirname "$(readlink -f "/proc/$1/exe")")")
  for dir in "$home/bin" "${JAVA_HOME:+$JAVA_HOME/bin}" "$(dirname "$(command -v jcmd || echo /none/jcmd)")"; do
    if [[ -n "$dir" && -x "$dir/jcmd" && -x "$dir/jstat" ]]; then echo "$dir"; return; fi
  done
  die "no jcmd/jstat next to the server's java, in \$JAVA_HOME or in PATH: set JAVA_HOME to the server's JDK"
}

main_class() {  # first argument that is not an option or an option value; never the whole command line
  tr '\0' '\n' < "/proc/$1/cmdline" | awk '
    NR == 1 || skip { skip = 0; next }
    /^(-cp|-classpath|--class-path|-p|--module-path|--add-opens|--add-exports|--add-modules)$/ { skip = 1; next }
    /^-jar$/ { jar = 1; next }
    /^-/ { next }
    { n = split($0, part, "/"); print (jar ? "jar " part[n] : $0); exit }'
}

describe() {  # key=value lines about the JVM $1
  local cwd checkout
  cwd=$(readlink "/proc/$1/cwd" 2>/dev/null || echo "?")
  checkout=$(git -C "$cwd" rev-parse --abbrev-ref HEAD 2>/dev/null)@$(git -C "$cwd" rev-parse --short HEAD 2>/dev/null) \
    || checkout="not a git checkout"
  echo "main=$(main_class "$1")"
  echo "process_started=$(date -d "$(LC_ALL=C ps -o lstart= -p "$1")" +%Y-%m-%dT%H:%M:%S)"
  echo "checkout=$checkout ($cwd)"
}

warn_dev_launcher() {
  if [[ "$1" == *org.opensilex.dev.* ]]; then
    echo "warning: dev launcher ($1): DevModule forces DEBUG=true, so the root logger is in DEBUG and logging costs"
    echo "         are part of every figure; compare only runs started the same way."
  fi
}

cmd_find() {
  local pid
  pid=$(server_pid)
  if [[ -z "$pid" ]]; then
    echo "nothing listens on port $PORT (OPENSILEX_PORT); Java processes of $USER:"
    for pid in $(pgrep -u "$USER" -x java || true); do echo "pid=$pid"; describe "$pid" | sed 's/^/  /'; done
    return
  fi
  echo "pid=$pid (port $PORT)"
  describe "$pid"
  warn_dev_launcher "$(main_class "$pid")"
  echo "Check that this checkout is the code the server runs (branch, rebuilt classes) before measuring."
}

cmd_start() {
  local pid=${1:-$(server_pid)} label=${2:-run} bin run
  [[ -n "$pid" ]] || die "no JVM on port $PORT: pass its PID (see: profile.sh find)"
  kill -0 "$pid" 2>/dev/null || die "process $pid is not running"
  [[ -f "$ACTIVE" ]] && die "a run is already active: $(cat "$ACTIVE") (profile.sh stop)"
  bin=$(jdk_bin "$pid")
  run="$ROOT/$(date +%Y-%m-%d_%H%M%S)_${label//[^A-Za-z0-9_-]/_}"
  mkdir -p "$run/jfr"
  { echo "pid=$pid"; echo "jdk=$bin"; echo "started=$(now)"; describe "$pid"; } > "$run/run.txt"
  # dumponexit: the recording survives a server restart in the middle of the run
  "$bin/jcmd" "$pid" JFR.start name="$RECORDING" settings=profile dumponexit=true filename="$run/run.jfr" >/dev/null
  "$bin/jcmd" "$pid" VM.log output="$run/gc.log" what=gc decorators=time,uptime >/dev/null
  printf 'time,label\n%s,%s\n' "$(now)" "$label" > "$run/phases.csv"
  nohup "$SELF" _sample-jvm "$pid" "$run" "$bin" >/dev/null 2>&1 &
  echo $! > "$run/samplers.pid"
  if command -v docker >/dev/null; then
    nohup "$SELF" _sample-containers "$pid" "$run" >/dev/null 2>&1 &
    echo $! >> "$run/samplers.pid"
  fi
  mkdir -p "$ROOT" && echo "$run" > "$ACTIVE"
  echo "recording $pid into $run"
  warn_dev_launcher "$(field "$run" main)"
  echo "next: run the scenario; 'profile.sh mark <phase>' between phases; 'profile.sh stop' at the end"
}

sample_jvm() {  # every $INTERVAL s until the JVM exits: process, heap, metaspace, GC, classes
  local pid=$1 run=$2 bin=$3 hz prev prevt cur curt cpu rss threads gc classes
  hz=$(getconf CLK_TCK)
  echo "time,rss_mb,threads,cpu_pct,heap_used_mb,heap_commit_mb,old_used_mb,meta_used_mb,ygc,fgc,cgc,gct_s,classes_loaded,classes_unloaded" \
    > "$run/sample.csv"
  prev=$(awk '{print $14 + $15}' "/proc/$pid/stat"); prevt=$(date +%s.%N)
  while kill -0 "$pid" 2>/dev/null; do
    sleep "$INTERVAL"
    cur=$(awk '{print $14 + $15}' "/proc/$pid/stat" 2>/dev/null) || break
    curt=$(date +%s.%N)
    cpu=$(awk -v a="$prev" -v b="$cur" -v t0="$prevt" -v t1="$curt" -v hz="$hz" \
      'BEGIN { printf "%.0f", (b - a) / hz / (t1 - t0) * 100 }')
    prev=$cur; prevt=$curt
    rss=$(awk '/VmRSS/ { printf "%.0f", $2 / 1024 }' "/proc/$pid/status")
    threads=$(awk '/Threads/ { print $2 }' "/proc/$pid/status")
    gc=$(LC_ALL=C "$bin/jstat" -gc "$pid" 2>/dev/null | tail -1) || continue
    classes=$(LC_ALL=C "$bin/jstat" -class "$pid" 2>/dev/null | tail -1 | awk '{ print $1 "," $3 }')
    # jstat -gc: S0C S1C S0U S1U EC EU OC OU MC MU CCSC CCSU YGC YGCT FGC FGCT CGC CGCT GCT (KB)
    echo "$gc" | awk -v t="$(now)" -v rss="$rss" -v thr="$threads" -v cpu="$cpu" -v cls="$classes" '{
      printf "%s,%s,%s,%s,%.0f,%.0f,%.0f,%.0f,%s,%s,%s,%s,%s\n", t, rss, thr, cpu,
        ($3 + $4 + $6 + $8) / 1024, ($1 + $2 + $5 + $7) / 1024, $8 / 1024, $10 / 1024, $13, $15, $17, $19, cls }' \
      >> "$run/sample.csv"
  done
}

server_containers() {  # regex of the containers publishing $CONTAINER_PORTS, else $CONTAINERS
  local port names=""
  for port in $CONTAINER_PORTS; do
    names+=$(docker ps --filter "publish=$port" --format '{{.Names}}' 2>/dev/null | head -1)"|"
  done
  names=${names//||/|}; names=${names#|}; names=${names%|}
  if [[ -n "$names" ]]; then echo "^($names)$"; else echo "$CONTAINERS"; fi
}

sample_containers() {  # docker memory and CPU of the server's RDF4J/MongoDB containers while the JVM is alive
  local pid=$1 run=$2 pattern
  pattern=$(server_containers)
  echo "time,name,mem,cpu" > "$run/containers.csv"
  while kill -0 "$pid" 2>/dev/null; do
    docker stats --no-stream --format '{{.Name}},{{.MemUsage}},{{.CPUPerc}}' 2>/dev/null \
      | awk -F, -v p="$pattern" 'tolower($1) ~ tolower(p)' | sed "s/^/$(now),/" >> "$run/containers.csv" || true
    sleep 4
  done
}

active_run() {
  [[ -f "$ACTIVE" ]] || die "no active run (profile.sh start)"
  cat "$ACTIVE"
}

dump_phase() {  # JFR of the phase that ends now, from its start mark
  local run=$1 start label number
  IFS=, read -r start label < <(tail -1 "$run/phases.csv")
  number=$(printf %02d $(( $(wc -l < "$run/phases.csv") - 1 )))
  "$(field "$run" jdk)/jcmd" "$(field "$run" pid)" JFR.dump name="$RECORDING" begin="$start" \
    filename="$run/jfr/${number}_${label}.jfr" >/dev/null || echo "warning: JFR dump of phase $label failed" >&2
}

cmd_mark() {
  local label=${1:?usage: profile.sh mark LABEL} run
  run=$(active_run)
  kill -0 "$(field "$run" pid)" 2>/dev/null || die "the JVM of $run has stopped: profile.sh stop"
  dump_phase "$run"
  echo "$(now),${label//[^A-Za-z0-9_-]/_}" >> "$run/phases.csv"
  echo "phase $label started"
}

cmd_stop() {
  local histogram=false pmap=false run pid bin arg
  for arg in "$@"; do
    case $arg in
      --histogram) histogram=true ;;
      --pmap) pmap=true ;;
      *) die "unknown option $arg" ;;
    esac
  done
  run=$(active_run); pid=$(field "$run" pid); bin=$(field "$run" jdk)
  if kill -0 "$pid" 2>/dev/null; then
    dump_phase "$run"
    if $pmap; then pmap -x "$pid" > "$run/pmap.txt"; fi
    # begin: without it the dump also holds repository chunks recorded before this run (earlier runs, same JVM)
    "$bin/jcmd" "$pid" JFR.dump name="$RECORDING" begin="$(field "$run" started)" filename="$run/run.jfr" >/dev/null
    "$bin/jcmd" "$pid" JFR.stop name="$RECORDING" >/dev/null
    "$bin/jcmd" "$pid" VM.log output="$run/gc.log" what=all=off >/dev/null || true
    if $histogram; then "$bin/jcmd" "$pid" GC.class_histogram > "$run/histogram_after_full_gc.txt"; fi
  else
    echo "the JVM has stopped: run.jfr was written on exit (dumponexit), phase files may be missing"
  fi
  xargs -r kill < "$run/samplers.pid" 2>/dev/null || true
  echo "stopped=$(now)" >> "$run/run.txt"
  rm -f "$ACTIVE"
  echo "analyse: python3 $(dirname "$SELF")/analyze.py $run"
}

cmd_status() {
  local run pid
  [[ -f "$ACTIVE" ]] || { echo "no active run; last runs:"; ls -1dt "$ROOT"/*/ 2>/dev/null | head -5; return; }
  run=$(cat "$ACTIVE"); pid=$(field "$run" pid)
  echo "run: $run"
  if kill -0 "$pid" 2>/dev/null; then echo "JVM $pid alive"; else echo "JVM $pid STOPPED: profile.sh stop"; fi
  echo "phases: $(tail -n +2 "$run/phases.csv" | cut -d, -f2 | paste -sd' ')"
  echo "last sample: $(tail -1 "$run/sample.csv" 2>/dev/null)"
}

case ${1:-} in
  find) cmd_find ;;
  start) shift; cmd_start "$@" ;;
  mark) shift; cmd_mark "$@" ;;
  stop) shift; cmd_stop "$@" ;;
  status) cmd_status ;;
  _sample-jvm) sample_jvm "$2" "$3" "$4" ;;
  _sample-containers) sample_containers "$2" "$3" ;;
  *) sed -n '2,12p' "$SELF" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
