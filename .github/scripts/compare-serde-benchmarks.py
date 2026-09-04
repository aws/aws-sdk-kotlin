#!/usr/bin/env python3
"""
Compare serde benchmark reports produced by :benchmarks:serde-benchmarks.

Each side (base/head) may be given several report files, one per repetition. Repetitions are reduced with a
median so that a single noisy run on a shared CI runner can't decide the outcome.

Usage:
    compare-serde-benchmarks.py \
        --base base-1.json base-2.json \
        --head head-1.json head-2.json \
        --metric p50 \
        --threshold 10 \
        --output comparison.md
"""

import argparse
import json
import statistics
import sys

METRICS = ("mean", "p50", "p90", "p95", "p99")


def load(paths, metric):
    """Return {benchmark id: [metric value per repetition]}"""
    samples = {}
    for path in paths:
        with open(path) as f:
            report = json.load(f)
        for result in report["serde_benchmarks"]:
            samples.setdefault(result["id"], []).append(result[metric])
    return samples


def reduce_samples(samples):
    return {benchmark_id: statistics.median(values) for benchmark_id, values in samples.items()}


def fmt_nanos(value):
    if value >= 1_000_000:
        return f"{value / 1_000_000:.3f} ms"
    if value >= 1_000:
        return f"{value / 1_000:.3f} µs"
    return f"{value:.1f} ns"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", nargs="+", required=True, help="base branch report file(s)")
    parser.add_argument("--head", nargs="+", required=True, help="head branch report file(s)")
    parser.add_argument("--metric", default="p50", choices=METRICS, help="metric to compare (default: p50)")
    parser.add_argument(
        "--threshold",
        type=float,
        default=10.0,
        help="percent slowdown that counts as a regression (default: 10)",
    )
    parser.add_argument("--output", help="write the markdown report here instead of stdout")
    parser.add_argument(
        "--fail-on-regression",
        action="store_true",
        help="exit non-zero when any benchmark regresses past --threshold",
    )
    args = parser.parse_args()

    base = reduce_samples(load(args.base, args.metric))
    head = reduce_samples(load(args.head, args.metric))

    shared = sorted(set(base) & set(head))
    added = sorted(set(head) - set(base))
    removed = sorted(set(base) - set(head))

    rows = []
    for benchmark_id in shared:
        base_value, head_value = base[benchmark_id], head[benchmark_id]
        delta = ((head_value - base_value) / base_value * 100) if base_value else 0.0
        rows.append((benchmark_id, base_value, head_value, delta))

    # worst regressions first
    rows.sort(key=lambda row: row[3], reverse=True)

    regressions = [row for row in rows if row[3] > args.threshold]
    improvements = [row for row in rows if row[3] < -args.threshold]

    lines = ["## Serde benchmark comparison", ""]
    if regressions:
        lines.append(f"⚠️ **{len(regressions)} benchmark(s) slower than base by more than {args.threshold:g}%**")
    else:
        lines.append(f"✅ No benchmark slower than base by more than {args.threshold:g}%")
    if improvements:
        lines.append(f"🚀 {len(improvements)} benchmark(s) faster by more than {args.threshold:g}%")
    lines += [
        "",
        f"Comparing `{args.metric}` of serialization/deserialization time, median of "
        f"{len(args.base)} base and {len(args.head)} head run(s).",
        "",
        f"| Benchmark | Base {args.metric} | Head {args.metric} | Delta |",
        "|---|---:|---:|---:|",
    ]
    for benchmark_id, base_value, head_value, delta in rows:
        if delta > args.threshold:
            marker = "⚠️"
        elif delta < -args.threshold:
            marker = "🚀"
        else:
            marker = ""
        lines.append(
            f"| `{benchmark_id}` | {fmt_nanos(base_value)} | {fmt_nanos(head_value)} | {delta:+.1f}% {marker} |",
        )

    for title, ids in (("Only in head (new)", added), ("Only in base (removed)", removed)):
        if ids:
            lines += ["", f"**{title}:** " + ", ".join(f"`{i}`" for i in ids)]

    lines += [
        "",
        "<sub>Shared CI runners are noisy; treat single-digit deltas as inconclusive and re-run before acting "
        "on a result.</sub>",
        "",
    ]

    report = "\n".join(lines)
    if args.output:
        with open(args.output, "w") as f:
            f.write(report)
    else:
        print(report)

    if regressions and args.fail_on_regression:
        print(f"{len(regressions)} benchmark(s) regressed past {args.threshold:g}%", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
