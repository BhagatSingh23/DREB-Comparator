import json
import os
import sys

ALGO_DISPLAY = {
    "token_bucket": "Token Bucket",
    "leaky_bucket": "Leaky Bucket",
    "fixed_window": "Fixed Window",
    "sliding_window_log": "Sliding Window Log",
    "sliding_window_counter": "Sliding Window Counter",
    "dreb_proportional": "DREB (proportional)",
    "dreb_burst_matched": "DREB (burst-matched)",
    "dreb_v2_base15": "DREB (v2 base 1.5)",
    "static_dual_bucket": "DualBucket (B=3C/7)"
}

ALGO_ORDER = [
    "token_bucket",
    "leaky_bucket",
    "fixed_window",
    "sliding_window_log",
    "sliding_window_counter",
    "dreb_proportional",
    "dreb_burst_matched",
    "dreb_v2_base15",
    "static_dual_bucket"
]

PATH_DISPLAY = {
    "benchmarkAdapter": "Adapter",
    "benchmarkDirect": "Direct",
    "benchmarkEngineAlone": "Engine-Alone"
}

PATH_ORDER = ["Adapter", "Direct", "Engine-Alone"]

def load_run(json_path):
    with open(json_path, "r") as f:
        data = json.load(f)
    results = {}
    for entry in data:
        bench_full = entry["benchmark"]
        bench_name = bench_full.split(".")[-1]
        algo = entry["params"]["algorithm"]
        score = entry["primaryMetric"]["score"] # ops/s
        unit = entry["primaryMetric"]["scoreUnit"]
        if unit == "ops/us":
            score *= 1e6
        elif unit == "ops/ms":
            score *= 1e3
        
        path = PATH_DISPLAY.get(bench_name, bench_name)
        if algo not in results:
            results[algo] = {}
        results[algo][path] = score
    return results

def main():
    runs = []
    for r in [1, 2, 3]:
        p = f"results/perf/raw_runs/jmh_same_key_run{r}.json"
        if not os.path.exists(p):
            print(f"File not found: {p}", file=sys.stderr)
            sys.exit(1)
        res = load_run(p)
        db_p = f"results/perf/raw_runs/jmh_static_db_run{r}.json"
        if os.path.exists(db_p):
            db_res = load_run(db_p)
            for k, v in db_res.items():
                res[k] = v
        runs.append(res)
    
    print("### Table 2.2: 1-Thread Throughput across Measured Paths under same-key (1 Key, 3-Run Stabilized)\n")
    print("| Algorithm | Measured Path | Run 1 (ops/s) | Run 2 (ops/s) | Run 3 (ops/s) | Median (ops/s) | Range (ops/s) | ns / decision (1/thrpt) |")
    print("| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |")

    table_data = {}
    for algo in ALGO_ORDER:
        algo_name = ALGO_DISPLAY[algo]
        table_data[algo] = {}
        for path in PATH_ORDER:
            s1 = runs[0][algo][path]
            s2 = runs[1][algo][path]
            s3 = runs[2][algo][path]
            sorted_scores = sorted([s1, s2, s3])
            median = sorted_scores[1]
            spread = sorted_scores[2] - sorted_scores[0]
            ns_per_op = (1.0 / median) * 1e9

            table_data[algo][path] = {
                "r1": s1, "r2": s2, "r3": s3,
                "median": median, "range": spread,
                "ns": ns_per_op
            }

            algo_col = f"**{algo_name}**" if path == "Adapter" else algo_name
            print(f"| {algo_col:<25} | {path:<12} | {s1:>12,.0f} | {s2:>12,.0f} | {s3:>12,.0f} | **{median:>12,.0f}** | {spread:>11,.0f} | {ns_per_op:>6.2f} ns |")

    print("\n--- Parity and Ordering Verification ---")
    all_monotonic = True
    for algo in ALGO_ORDER:
        adapter_med = table_data[algo]["Adapter"]["median"]
        direct_med = table_data[algo]["Direct"]["median"]
        engine_med = table_data[algo]["Engine-Alone"]["median"]
        
        ok = (engine_med >= direct_med) and (direct_med >= adapter_med * 0.98) # allow tiny measurement noise margin
        status = "PASS" if (engine_med >= direct_med >= adapter_med) else "CHECK"
        print(f"[{status}] {ALGO_DISPLAY[algo]}: Engine-Alone ({engine_med:,.0f}) >= Direct ({direct_med:,.0f}) >= Adapter ({adapter_med:,.0f})")
        if not (engine_med >= direct_med and direct_med >= adapter_med):
            all_monotonic = False

if __name__ == "__main__":
    main()
