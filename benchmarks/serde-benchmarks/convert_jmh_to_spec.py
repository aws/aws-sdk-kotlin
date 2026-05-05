#!/usr/bin/env python3
"""
Converts JMH JSON benchmark output to the AwsSdkPerformanceBenchmarkModels spec format.

Usage:
    python3 convert_jmh_to_spec.py <jmh_output.json> [--instance m7i.xlarge]

JMH must be run with:
    outputTimeUnit = "ns"
    reportFormat = "json"
"""
import json
import math
import platform
import sys
from pathlib import Path


def percentile(sorted_data: list[float], p: float) -> float:
    """Compute the p-th percentile (0-100) using nearest-rank."""
    if not sorted_data:
        return 0.0
    k = (p / 100.0) * (len(sorted_data) - 1)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return sorted_data[int(k)]
    return sorted_data[f] * (c - k) + sorted_data[c] * (k - f)


def std_dev(data: list[float], mean: float) -> float:
    if len(data) < 2:
        return 0.0
    variance = sum((x - mean) ** 2 for x in data) / (len(data) - 1)
    return math.sqrt(variance)


def convert(jmh_results: list[dict], instance: str = "unknown") -> dict:
    spec_benchmarks = []

    for entry in jmh_results:
        # Extract test case id from benchmark name
        # Format: package.ClassName.methodName
        benchmark_name = entry.get("benchmark", "")
        test_id = benchmark_name.rsplit(".", 1)[-1] if benchmark_name else "unknown"

        primary = entry.get("primaryMetric", {})
        raw_data = primary.get("rawData", [[]])

        # Flatten all fork iterations into a single list
        all_samples = []
        for fork_data in raw_data:
            all_samples.extend(fork_data)

        if not all_samples:
            continue

        n = len(all_samples)
        sorted_samples = sorted(all_samples)
        mean_val = sum(all_samples) / n

        spec_benchmarks.append({
            "id": test_id,
            "n": n,
            "mean": round(mean_val),
            "p50": round(percentile(sorted_samples, 50)),
            "p90": round(percentile(sorted_samples, 90)),
            "p95": round(percentile(sorted_samples, 95)),
            "p99": round(percentile(sorted_samples, 99)),
            "std_dev": round(std_dev(all_samples, mean_val)),
        })

    os_info = f"{platform.system()} {platform.machine()}"

    return {
        "metadata": {
            "lang": "Kotlin",
            "software": [
                ["smithy-kotlin", "SNAPSHOT"],
                ["AWS SDK for Kotlin", "SNAPSHOT"],
            ],
            "os": os_info,
            "instance": instance,
            "precision": "-9",  # nanosecond precision (JVM System.nanoTime)
        },
        "serde_benchmarks": spec_benchmarks,
    }


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <jmh_output.json> [--instance TYPE]", file=sys.stderr)
        sys.exit(1)

    input_path = Path(sys.argv[1])
    instance = "unknown"
    if "--instance" in sys.argv:
        idx = sys.argv.index("--instance")
        if idx + 1 < len(sys.argv):
            instance = sys.argv[idx + 1]

    with open(input_path) as f:
        jmh_data = json.load(f)

    result = convert(jmh_data, instance)

    output_path = input_path.with_suffix(".spec.json")
    with open(output_path, "w") as f:
        json.dump(result, f, indent=2)

    print(f"Wrote {len(result['serde_benchmarks'])} benchmark results to {output_path}")


if __name__ == "__main__":
    main()
