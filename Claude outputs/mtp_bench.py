#!/usr/bin/env python3
"""MTP draft-length benchmark for a running llama-server (OpenAI-compatible chat endpoint).

Usage:
  python3 mtp_bench.py run  <label> [--url http://127.0.0.1:8080] [--runs 3] [--max-tokens 1024]
                            [--long-file path/to/BigFile.kt] [--long-runs 3]
  python3 mtp_bench.py compare <a.csv> <b.csv>

Typical:
  # server started with the new setting (draft n-max 5)
  python3 mtp_bench.py run n5
  # restart server with the old setting (draft n-max 2), nothing else changed
  python3 mtp_bench.py run n2
  python3 mtp_bench.py compare mtp_bench_n2.csv mtp_bench_n5.csv

Speculative/MTP decoding is verified by the main model, so output quality is unchanged in
principle; what changes is generation speed and draft acceptance. That is what this measures.
Standard library only.
"""
import csv, json, statistics, sys, time, urllib.request

PROMPTS = {
    # Agent-like: rewrite supplied code (lots of copying -> drafts should accept well)
    "refactor": (
        "Rewrite this Kotlin function so it uses a when-expression and returns early. "
        "Output only the code.\n\n```kotlin\n"
        "fun dayLabel(index: Int, ctx: Context): String {\n"
        "    var label = \"\"\n"
        "    if (index == 0) {\n        label = ctx.getString(R.string.today)\n    } else {\n"
        "        if (index == 1) {\n            label = ctx.getString(R.string.tomorrow)\n"
        "        } else {\n            label = ctx.getString(R.string.day_n, index + 1)\n        }\n    }\n"
        "    return label\n}\n```"
    ),
    # Agent-like: new code from a spec
    "codegen": (
        "Write a Kotlin function `parseCapTime(s: String): Instant?` that accepts ISO-8601 "
        "timestamps with 'Z' or a UTC offset like +02:00, tries OffsetDateTime first, falls back "
        "to Instant.parse, and returns null on failure. Then write three JUnit 4 tests for it. "
        "Output only code."
    ),
    # Free prose (usually lowest acceptance)
    "prose": (
        "Explain in about 300 words why speculative decoding speeds up text generation and "
        "when a longer draft can make it slower."
    ),
}

FIELDS = ["label", "prompt", "run", "seed", "wall_s", "prompt_n", "prompt_per_second",
          "predicted_n", "predicted_per_second", "draft_n", "draft_n_accepted", "accept_rate"]


def post(url, body, timeout=900):
    req = urllib.request.Request(url, data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read())


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return json.loads(r.read())


def show_props(base):
    try:
        props = get(base + "/props")
    except Exception as e:  # older servers / disabled endpoint
        print(f"(could not read /props: {e})")
        return
    found = {}

    def walk(o, path=""):
        if isinstance(o, dict):
            for k, v in o.items():
                p = f"{path}.{k}" if path else k
                if any(t in k.lower() for t in ("spec", "draft", "mtp")):
                    found[p] = v
                walk(v, p)
    walk(props)
    print("Server speculative settings reported by /props:")
    if found:
        for k, v in found.items():
            s = json.dumps(v)
            print(f"  {k} = {s[:200]}")
    else:
        print("  (none reported; check the server log line for the draft settings)")


LONG_TASK = ("Below is SettingsDialogManager.kt from an Android app. Find the code that builds the widget "
             "opacity card and rewrite that function so its colors come from the theme palette instead of "
             "hardcoded hex values. Output only the rewritten function.\n\n```kotlin\n{src}\n```")


def run(label, base, runs, max_tokens, long_file=None, long_runs=3):
    show_props(base)
    url = base + "/v1/chat/completions"
    print("Warm-up request (discarded)...")
    post(url, {"messages": [{"role": "user", "content": "Say OK."}], "max_tokens": 16})
    rows = []
    prompts = [(k, v, runs) for k, v in PROMPTS.items()]
    if long_file:
        files = [x for x in long_file.split(":") if x]
        with open(files[0], errors="replace") as f:
            first = f.read()
        prompts.append(("longctx", LONG_TASK.format(src=first), long_runs))
        if len(files) > 1:  # bigger context: several source files, same task
            parts = []
            for fp in files:
                with open(fp, errors="replace") as f:
                    parts.append(f"// ===== {fp.rsplit('/', 1)[-1]} =====\n" + f.read())
            prompts.append(("xlctx", LONG_TASK.replace("Below is SettingsDialogManager.kt", "Below are several Kotlin files")
                            .replace("Find the code", "In SettingsDialogManager.kt, find the code")
                            .format(src="\n\n".join(parts)), long_runs))
    for name, text, nruns in prompts:
        for i in range(1, nruns + 1):
            seed = 1000 + i  # same seeds for every label -> like-for-like
            body = {"messages": [{"role": "user", "content": text}],
                    "max_tokens": max_tokens, "seed": seed,
                    # long prompts: run 1 reads the prompt cold (measures prompt processing),
                    # later runs reuse the cached prompt so they measure generation at depth cheaply
                    "cache_prompt": name in ("longctx", "xlctx"), "stream": False}
            t0 = time.time()
            resp = post(url, body)
            wall = time.time() - t0
            t = resp.get("timings", {}) or {}
            dn, da = t.get("draft_n"), t.get("draft_n_accepted")
            row = {"label": label, "prompt": name, "run": i, "seed": seed,
                   "wall_s": round(wall, 2), "prompt_n": t.get("prompt_n"),
                   "prompt_per_second": round(t.get("prompt_per_second", 0) or 0, 2),
                   "predicted_n": t.get("predicted_n"),
                   "predicted_per_second": round(t.get("predicted_per_second", 0) or 0, 2),
                   "draft_n": dn, "draft_n_accepted": da,
                   "accept_rate": round(da / dn, 3) if dn else None}
            rows.append(row)
            print(f"  {name:9s} run {i}: {row['predicted_per_second']:7.2f} t/s, "
                  f"{row['predicted_n']} tok, accept {row['accept_rate']}")
    out = f"mtp_bench_{label}.csv"
    with open(out, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS)
        w.writeheader()
        w.writerows(rows)
    print(f"\nWrote {out}")
    summarize({label: rows})


def load(path):
    with open(path) as f:
        rows = list(csv.DictReader(f))
    for r in rows:
        for k in ("predicted_per_second", "accept_rate", "predicted_n"):
            r[k] = float(r[k]) if r[k] not in ("", "None", None) else None
    return rows


def summarize(groups):
    labels = list(groups)
    prompts = list(dict.fromkeys(r["prompt"] for g in groups.values() for r in g))
    head = f"{'prompt':10s}" + "".join(f"{l + ' t/s':>14s}{l + ' acc':>11s}" for l in labels)
    print("\nMedian per prompt:\n" + head)
    totals = {l: [] for l in labels}
    for p in prompts:
        line = f"{p:10s}"
        for l in labels:
            rs = [r for r in groups[l] if r["prompt"] == p]
            tps = [r["predicted_per_second"] for r in rs if r["predicted_per_second"]]
            acc = [r["accept_rate"] for r in rs if r["accept_rate"] is not None]
            mt = statistics.median(tps) if tps else float("nan")
            totals[l].append(mt)
            ma = f"{statistics.median(acc):.2f}" if acc else "n/a"
            line += f"{mt:14.2f}{ma:>11s}"
        print(line)
    if len(labels) == 2:
        a, b = labels
        print("\nChange in t/s, " + b + " vs " + a + ":")
        for p, x, y in zip(prompts, totals[a], totals[b]):
            print(f"  {p:9s} {100 * (y - x) / x:+6.1f}%")


def main():
    args = sys.argv[1:]
    if not args or args[0] not in ("run", "compare"):
        print(__doc__)
        sys.exit(1)
    if args[0] == "compare":
        a, b = args[1], args[2]
        ga, gb = load(a), load(b)
        summarize({ga[0]["label"]: ga, gb[0]["label"]: gb})
        return
    label = args[1]
    opts = dict(zip(args[2::2], args[3::2]))
    run(label, opts.get("--url", "http://127.0.0.1:8080").rstrip("/"),
        int(opts.get("--runs", 3)), int(opts.get("--max-tokens", 1024)),
        opts.get("--long-file"), int(opts.get("--long-runs", 3)))


if __name__ == "__main__":
    main()
