#!/usr/bin/env bash
# MTP draft-length sweep: for N = 0..8, start qwen-server with --spec-draft-n-max N,
# run mtp_bench.py, kill the server, repeat. Then build one report.
#
# Usage:  bash mtp_sweep.sh                 (from the folder holding mtp_bench.py + mtp_report.py)
# Options (environment variables):
#   QWEN_SERVER=/path/to/qwen-server   default: found on PATH
#   N_FROM=0 N_TO=8 RUNS=5 MAX_TOKENS=1024
#
# Your qwen-server script is NOT modified: each N uses a temporary copy in the output folder.
# N=0 removes the two --spec lines (true no-drafting baseline).
# Each N gets its own server log so the report can prove the setting took effect.
# Afterwards no server is left running; start qwen-server yourself as usual.
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCH="$HERE/mtp_bench.py"; REPORT="$HERE/mtp_report.py"
QWEN_SERVER="${QWEN_SERVER:-$(command -v qwen-server || true)}"
N_FROM="${N_FROM:-0}"; N_TO="${N_TO:-8}"; RUNS="${RUNS:-5}"; MAX_TOKENS="${MAX_TOKENS:-1024}"
PORT=8080; URL="http://127.0.0.1:$PORT"
OUT="$HERE/mtp_sweep_$(date +%Y%m%d_%H%M%S)"

for f in "$BENCH" "$REPORT"; do [ -f "$f" ] || { echo "Missing $f (keep it next to this script)"; exit 1; }; done
[ -n "$QWEN_SERVER" ] && [ -f "$QWEN_SERVER" ] || { echo "qwen-server not found; run with QWEN_SERVER=/path/to/qwen-server bash mtp_sweep.sh"; exit 1; }
grep -q -- '--spec-draft-n-max' "$QWEN_SERVER" || { echo "$QWEN_SERVER has no --spec-draft-n-max line; stopping."; exit 1; }
mkdir -p "$OUT"

vram_mib() {  # used VRAM on the largest amdgpu card (the 7900 XTX), MiB
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

{
  echo "- Date: $(date '+%Y-%m-%d %H:%M %Z')"
  echo "- Script: $QWEN_SERVER"
  echo "- Original spec lines: $(grep -E -- '--spec-' "$QWEN_SERVER" | tr -s ' ' | tr '\n' ' ')"
  echo "- llama version: $(llama server --version 2>&1 | grep -iE 'version|build' | head -2 | tr '\n' ' ')"
  echo "- Runs per prompt: $RUNS, max_tokens: $MAX_TOKENS, prompts: refactor / codegen / prose, seeds 1001..$((1000+RUNS))"
  echo "- Idle VRAM before sweep (server stopped): $(stop_server; vram_mib) MiB"
} > "$OUT/sweep_header.txt"
printf "n\tstatus\tload_s\tvram_mib\n" > "$OUT/sweep_meta.tsv"
echo "Output folder: $OUT"

trap 'echo; echo "Interrupted; stopping server."; stop_server; exit 130' INT TERM

for N in $(seq "$N_FROM" "$N_TO"); do
  echo; echo "===== N=$N ====="
  stop_server
  S="$OUT/qwen-server-n$N"
  LOG="$OUT/server_n$N.log"
  if [ "$N" -eq 0 ]; then
    sed -e '/--spec-type/d' -e '/--spec-draft-n-max/d' "$QWEN_SERVER" > "$S"
  else
    sed -e "s/--spec-draft-n-max[ =][0-9]*/--spec-draft-n-max $N/" "$QWEN_SERVER" > "$S"
  fi
  sed -i -e "s|--log-file [^ ]*|--log-file \"$LOG\"|" "$S"
  chmod +x "$S"
  echo "Spec lines used: $(grep -E -- '--spec-' "$S" | tr -s ' ' | tr '\n' ' ')"

  t0=$(date +%s)
  "$S" > "$OUT/server_n$N.stdout" 2>&1 &
  SPID=$!
  status="ok"
  until curl -sf -o /dev/null "$URL/health"; do
    if ! kill -0 "$SPID" 2>/dev/null; then status="server exited during load"; break; fi
    if [ $(( $(date +%s) - t0 )) -gt 600 ]; then status="load timeout (600 s)"; break; fi
    sleep 2
  done
  load_s=$(( $(date +%s) - t0 ))
  vram="n/a"
  if [ "$status" = "ok" ]; then
    vram=$(vram_mib)
    echo "Loaded in ${load_s}s, VRAM ${vram} MiB. Benchmarking..."
    ( cd "$OUT" && python3 "$BENCH" run "n$N" --url "$URL" --runs "$RUNS" --max-tokens "$MAX_TOKENS" ) \
      || status="benchmark error"
  else
    echo "N=$N: $status (see $LOG / server_n$N.stdout)"
    [ -s "$LOG" ] || cp "$OUT/server_n$N.stdout" "$LOG"
  fi
  printf "%s\t%s\t%s\t%s\n" "$N" "$status" "$load_s" "$vram" >> "$OUT/sweep_meta.tsv"
  stop_server; SPID=""
done

echo; echo "===== Building report ====="
python3 "$REPORT" "$OUT" > /dev/null
echo "Done. Send me this file: $OUT/sweep_report.md"
echo "(No server is running now; start qwen-server as usual.)"
