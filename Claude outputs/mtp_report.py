#!/usr/bin/env python3
"""Build one report from an mtp_sweep.sh output folder.

Usage: python3 mtp_report.py <sweep folder>
Writes <sweep folder>/sweep_report.md and prints it.
Standard library only.
"""
import csv, glob, math, os, re, statistics, sys

LOG_RE = re.compile(r"draft acceptance = ([0-9.]+) \(\s*(\d+) accepted /\s*(\d+) generated\), mean len =\s*([0-9.]+)")


def num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def med(xs):
    xs = [x for x in xs if x is not None]
    return statistics.median(xs) if xs else None


def fmt(x, d=2):
    return "n/a" if x is None else f"{x:.{d}f}"


def geo(xs):
    xs = [x for x in xs if x]
    return math.exp(sum(math.log(x) for x in xs) / len(xs)) if xs else None


def main():
    d = sys.argv[1]
    meta = {}
    mp = os.path.join(d, "sweep_meta.tsv")
    if os.path.exists(mp):
        with open(mp) as f:
            for r in csv.DictReader(f, delimiter="\t"):
                meta[int(r["n"])] = r
    header = open(os.path.join(d, "sweep_header.txt")).read() if os.path.exists(os.path.join(d, "sweep_header.txt")) else ""

    runs = {}
    for p in glob.glob(os.path.join(d, "mtp_bench_n*.csv")):
        n = int(re.search(r"_n(\d+)\.csv$", p).group(1))
        with open(p) as f:
            runs[n] = list(csv.DictReader(f))

    logs = {}
    for n in sorted(set(meta) | set(runs)):
        lp = os.path.join(d, f"server_n{n}.log")
        lines = []
        if os.path.exists(lp):
            with open(lp, errors="replace") as f:
                for line in f:
                    m = LOG_RE.search(line)
                    if m:
                        lines.append((float(m[1]), int(m[2]), int(m[3]), float(m[4]), line.strip()))
        logs[n] = lines

    ns = sorted(set(meta) | set(runs))
    prompts = []
    for n in ns:
        for r in runs.get(n, []):
            if r["prompt"] not in prompts:
                prompts.append(r["prompt"])

    def per(n, p, key):
        return [num(r[key]) for r in runs.get(n, []) if r["prompt"] == p]

    out = []
    w = out.append
    w("# MTP draft-length sweep report\n")
    w(header.strip() + "\n")

    # Table 1: overview
    base = None
    if 0 in runs:
        base = geo([med(per(0, p, "predicted_per_second")) for p in prompts])
    w("## 1. Overview (generation speed = median of runs per prompt)\n")
    cols = " | ".join(f"{p} t/s" for p in prompts)
    w(f"| N | status | {cols} | geo-mean t/s | vs N=0 | code-mean t/s | log accept (acc/drafted) | max mean len | setting verified (actual draft len) | load s | VRAM used MiB |")
    w("|" + "---|" * (10 + len(prompts)))
    for n in ns:
        m = meta.get(n, {})
        meds = [med(per(n, p, "predicted_per_second")) for p in prompts]
        g = geo(meds)
        code = [x for p, x in zip(prompts, meds) if p != "prose" and x]
        codem = statistics.mean(code) if code else None
        lg = logs.get(n, [])
        acc = sum(x[1] for x in lg); gen = sum(x[2] for x in lg)
        maxlen = max((x[3] for x in lg), default=None)
        # effective draft length per check: mean len = 1 + n_eff * acceptance
        neff = med([(x[3] - 1) / x[0] for x in lg if x[0] > 0])
        if n == 0:
            verified = "yes (no drafting)" if not lg else "NO: drafting seen"
        else:
            verified = "n/a" if neff is None else (f"yes ({neff:.1f})" if abs(neff - n) <= 0.6 else f"NO: drafted {neff:.1f}")
        vs = f"{100 * (g - base) / base:+.1f}%" if (g and base) else "n/a"
        w(f"| {n} | {m.get('status', '?')} | " + " | ".join(fmt(x) for x in meds) +
          f" | {fmt(g)} | {vs} | {fmt(codem)} | " +
          (f"{acc / gen:.3f} ({acc}/{gen})" if gen else "n/a") +
          f" | {fmt(maxlen)} | {verified} | {m.get('load_s', '?')} | {m.get('vram_mib', '?')} |")

    # Table 2: spread (noise)
    w("\n## 2. Run-to-run spread of generation t/s (min / median / max)\n")
    w("| N | " + " | ".join(prompts) + " |")
    w("|" + "---|" * (1 + len(prompts)))
    for n in ns:
        cells = []
        for p in prompts:
            xs = [x for x in per(n, p, "predicted_per_second") if x]
            cells.append(f"{min(xs):.1f} / {statistics.median(xs):.1f} / {max(xs):.1f}" if xs else "n/a")
        w(f"| {n} | " + " | ".join(cells) + " |")

    # Table 3: acceptance and drafting volume per prompt
    w("\n## 3. Per-prompt acceptance (median) and drafted tokens per generated token\n")
    w("| N | " + " | ".join(f"{p} acc | {p} drafted/gen" for p in prompts) + " |")
    w("|" + "---|" * (1 + 2 * len(prompts)))
    for n in ns:
        cells = []
        for p in prompts:
            rs = [r for r in runs.get(n, []) if r["prompt"] == p]
            acc = med([num(r["accept_rate"]) for r in rs])
            dn = sum(num(r["draft_n"]) or 0 for r in rs); pn = sum(num(r["predicted_n"]) or 0 for r in rs)
            cells += [fmt(acc), fmt(dn / pn if pn else None)]
        w(f"| {n} | " + " | ".join(cells) + " |")

    # Table 4: prompt processing + tokens produced
    w("\n## 4. Prompt processing t/s (median) and tokens generated (median)\n")
    w("| N | " + " | ".join(f"{p} pp t/s | {p} tokens" for p in prompts) + " |")
    w("|" + "---|" * (1 + 2 * len(prompts)))
    for n in ns:
        cells = []
        for p in prompts:
            cells += [fmt(med(per(n, p, "prompt_per_second")), 1), fmt(med(per(n, p, "predicted_n")), 0)]
        w(f"| {n} | " + " | ".join(cells) + " |")

    # Failures
    bad = [n for n in ns if meta.get(n, {}).get("status") not in ("ok", None)]
    if bad:
        w("\n## 5. Failures (last server log lines)\n")
        for n in bad:
            w(f"### N={n}: {meta[n].get('status')}\n```")
            lp = os.path.join(d, f"server_n{n}.log")
            if os.path.exists(lp):
                w("".join(open(lp, errors="replace").readlines()[-25:]).rstrip())
            w("```")

    # Raw appendix
    w("\n## Appendix A. Server log draft lines per N\n")
    for n in ns:
        w(f"### N={n}\n```")
        for x in logs.get(n, []):
            w(x[4])
        if not logs.get(n):
            w("(no draft lines)")
        w("```")
    w("\n## Appendix B. All per-request rows\n```")
    first = True
    for n in ns:
        for r in runs.get(n, []):
            if first:
                w(",".join(r.keys())); first = False
            w(",".join(str(v) for v in r.values()))
    w("```")

    text = "\n".join(out) + "\n"
    with open(os.path.join(d, "sweep_report.md"), "w") as f:
        f.write(text)
    print(text)


if __name__ == "__main__":
    main()
