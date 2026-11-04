#!/usr/bin/env bash
# Speed sweep: for each line in speed_configs.txt, start qwen-server with those flag changes,
# run mtp_bench.py (3 short prompts + 2 long-context prompts), stop the server, repeat.
# Then build one report: speed_report.md.
#
# Usage:  bash speed_sweep.sh            (from the folder holding the other files)
# Options (environment variables):
#   QWEN_SERVER=/path/to/qwen-server   default: found on PATH
#   LONG_FILE=/path/to/SettingsDialogManager.kt   default: searched under ~/AndroidStudioProjects
#   RUNS=3 LONG_RUNS=5 MAX_TOKENS=1024
# Long-context runs: run 1 reads the prompt cold (prompt-processing speed), runs 2+ reuse the cached prompt. CONFIGS=speed_configs.txt
#
# Your qwen-server script is NOT modified: each config runs from a temporary copy in the output folder.
# No server is left running at the end; start qwen-server as usual.
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCH="$HERE/mtp_bench.py"; TOOLS="$HERE/speed_tools.py"; CONFIGS="${CONFIGS:-$HERE/speed_configs.txt}"
QWEN_SERVER="${QWEN_SERVER:-$(command -v qwen-server || true)}"
RUNS="${RUNS:-3}"; LONG_RUNS="${LONG_RUNS:-5}"; MAX_TOKENS="${MAX_TOKENS:-1024}"
PORT=8080; URL="http://127.0.0.1:$PORT"
OUT="$HERE/speed_sweep_$(date +%Y%m%d_%H%M%S)"

for f in "$BENCH" "$TOOLS" "$CONFIGS"; do [ -f "$f" ] || { echo "Missing $f (keep it next to this script)"; exit 1; }; done
[ -n "$QWEN_SERVER" ] && [ -f "$QWEN_SERVER" ] || { echo "qwen-server not found; run with QWEN_SERVER=/path/to/qwen-server bash speed_sweep.sh"; exit 1; }
if [ -z "${LONG_FILE:-}" ]; then
  SDM=$(find "$HOME/AndroidStudioProjects" "$HOME/Projects" -name SettingsDialogManager.kt -path '*app/src/main*' 2>/dev/null | head -1)
  if [ -n "$SDM" ]; then
    # longctx = SettingsDialogManager.kt (~22k tokens); xlctx = it plus four more large files (~55k tokens)
    LONG_FILE="$SDM"
    for f in WeatherRepository.kt LocationPreferences.kt AstronomyDisplayManager.kt WidgetSyncWorker.kt; do
      [ -f "$(dirname "$SDM")/$f" ] && LONG_FILE="$LONG_FILE:$(dirname "$SDM")/$f"
    done
  fi
fi
mkdir -p "$OUT"

vram_mib() {
  local best_t=0 best_u=""
  for c in /sys/class/drm/card*/device; do
    [ -r "$c/mem_info_vram_total" ] || continue
    local t u; t=$(cat "$c/mem_info_vram_total"); u=$(cat "$c/mem_info_vram_used")
    if [ "$t" -gt "$best_t" ]; then best_t=$t; best_u=$u; fi
  done
  [ -n "$best_u" ] && echo $(( best_u / 1048576 )) || echo "n/a"
}

stop_server() {
  [ -n "${SPID:-}" ] && kill "$SPID" 2>/dev/null
  pkill -f -- 'llama[ -]server' 2>/dev/null
  for _ in $(seq 1 60); do
    curl -s -o /dev/null "$URL/health" || return 0
    sleep 1
  done
  pkill -9 -f -- 'llama[ -]server' 2>/dev/null; sleep 2
}

stop_server; sleep 5   # let the driver release VRAM before the idle reading
HELP="$(llama server --help 2>&1)"
echo "$HELP" | grep -iE -- 'spec|draft|mtp|ngram|lookup|p-min|n-min|min-p|cache-type|ubatch|batch-size' > "$OUT/help_spec.txt"
{
  echo "- Date: $(date '+%Y-%m-%d %H:%M %Z')"
  echo "- Script: $QWEN_SERVER"
  echo "- llama version: $(llama server --version 2>&1 | grep -iE 'version|build' | head -2 | tr '\n' ' ')"
  echo "- Short prompts: refactor / codegen / prose × $RUNS runs; long-context files: ${LONG_FILE:-NOT FOUND (skipped)} × $LONG_RUNS runs; max_tokens $MAX_TOKENS"
  echo "- Idle VRAM before sweep: $(vram_mib) MiB"
} > "$OUT/header.txt"
printf "name\tflags\tnote\tstatus\tload_s\tvram_mib\n" > "$OUT/meta.tsv"
echo "Output folder: $OUT"
[ -n "${LONG_FILE:-}" ] || echo "WARNING: SettingsDialogManager.kt not found; long-context test skipped (set LONG_FILE=...)."

trap 'echo; echo "Interrupted; stopping server."; stop_server; exit 130' INT TERM

while IFS=$'\t' read -r NAME FLAGS NOTE; do
  echo; echo "===== $NAME: $FLAGS ====="
  status="ok"; load_s="-"; vram="-"
  for f in $FLAGS; do
    k="${f%%=*}"
    if ! grep -qE -- "(^|[ ,])${k}([ ,=]|$)" <<<"$HELP"; then status="skipped: $k not in this llama.cpp build"; fi
  done
  if [ "$status" != "ok" ]; then
    echo "$status"
    printf "%s\t%s\t%s\t%s\t%s\t%s\n" "$NAME" "$FLAGS" "$NOTE" "$status" "$load_s" "$vram" >> "$OUT/meta.tsv"
    continue
  fi
  stop_server
  S="$OUT/qwen-server-$NAME"; LOG="$OUT/server_$NAME.log"
  # shellcheck disable=SC2086
  python3 "$TOOLS" edit "$QWEN_SERVER" "$S" "$LOG" $FLAGS || { status="config edit failed"; }
  if [ "$status" = "ok" ]; then
    t0=$(date +%s)
    "$S" > "$OUT/server_$NAME.stdout" 2>&1 &
    SPID=$!
    until curl -sf -o /dev/null "$URL/health"; do
      if ! kill -0 "$SPID" 2>/dev/null; then status="server exited during load"; break; fi
      if [ $(( $(date +%s) - t0 )) -gt 600 ]; then status="load timeout (600 s)"; break; fi
      sleep 2
    done
    load_s=$(( $(date +%s) - t0 ))
  fi
  if [ "$status" = "ok" ]; then
    vram=$(vram_mib)
    echo "Loaded in ${load_s}s, VRAM ${vram} MiB. Benchmarking..."
    LONG_ARGS=(); [ -n "${LONG_FILE:-}" ] && LONG_ARGS=(--long-file "$LONG_FILE" --long-runs "$LONG_RUNS")
    ( cd "$OUT" && python3 "$BENCH" run "$NAME" --url "$URL" --runs "$RUNS" --max-tokens "$MAX_TOKENS" "${LONG_ARGS[@]}" ) \
      || status="benchmark error"
  else
    echo "$NAME: $status"
    [ -s "$LOG" ] || cp "$OUT/server_$NAME.stdout" "$LOG" 2>/dev/null
  fi
  printf "%s\t%s\t%s\t%s\t%s\t%s\n" "$NAME" "$FLAGS" "$NOTE" "$status" "$load_s" "$vram" >> "$OUT/meta.tsv"
  stop_server; SPID=""
done < <(python3 "$TOOLS" configs "$CONFIGS")

echo; echo "===== Building report ====="
python3 "$TOOLS" report "$OUT"
echo "Done. Send me this file: $OUT/speed_report.md"
echo "(No server is running now; start qwen-server as usual.)"
