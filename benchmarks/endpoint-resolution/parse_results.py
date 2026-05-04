#!/usr/bin/env python3
"""Parse JMH JSON results and estimate total invocation count per benchmark."""
import json
import sys
import glob

def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "benchmarks/build/reports/benchmarks/main/*.json"
    files = glob.glob(path)
    if not files:
        print(f"No files found matching: {path}")
        sys.exit(1)

    for f in files:
        with open(f) as fp:
            results = json.load(fp)

        print(f"{'Benchmark':<70} {'Score (ns/op)':>14} {'Error':>10} {'Est. Count':>12}")
        print("-" * 110)
        for entry in results:
            name = entry["benchmark"].split(".")[-1]
            score = entry["primaryMetric"]["score"]
            error = entry["primaryMetric"]["scoreError"]
            iterations = entry["measurementIterations"]
            # Each iteration runs for 1 second = 1e9 ns
            ops_per_iter = 1_000_000_000 / score
            total_ops = int(ops_per_iter * iterations)
            print(f"{name:<70} {score:>14.3f} {error:>9.3f} {total_ops:>12,}")

if __name__ == "__main__":
    main()
