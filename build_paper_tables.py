import csv
import re

COMMIT_HASH = "f34fad1"

def main():
    doc = []
    doc.append("# Comprehensive Paper Tables Reference (Single Source of Truth)\n\n")
    doc.append(f"> **Master Provenance Commit:** `{COMMIT_HASH}`\n")
    doc.append("> **Repository:** `DREB-Comparator` | **Library Tag:** `dreb-ratelimiter v1.0.2-phase1`\n")
    doc.append("> **Reporting Standard:** Seeds 2000–2029 (N=30 replications), strict original hypothesis wording, no relaxed tolerance margins, Candidate minus Reference (`cur - ref`) bootstrap paired differences.\n\n")
    doc.append("---\n\n")

    # 1. E1
    print("Formatting E1...")
    doc.append(get_e1_section())

    # 2. E2
    print("Formatting E2...")
    doc.append(get_e2_section())

    # 3. Compliant Bursty (0.78R)
    print("Formatting E2 Evasion/Compliant...")
    doc.append(get_e2_evasion_section())

    # 4. E3
    print("Formatting E3...")
    doc.append(get_e3_section())

    # 5. E7
    print("Formatting E7...")
    doc.append(get_e7_section())

    # 6. E8
    print("Formatting E8...")
    doc.append(get_e8_section())

    # 7. E9 (read from results/e9/README.md)
    print("Formatting E9...")
    doc.append(get_e9_section())

    # 8. E5 & E6
    print("Formatting E5 & E6...")
    doc.append(get_e5_e6_section())

    # 9. Corrected JMH
    print("Formatting JMH...")
    doc.append(get_jmh_section())

    with open('results/PAPER_TABLES.md', 'w') as f:
        f.write("".join(doc))
    print("Wrote results/PAPER_TABLES.md successfully.")

def get_e1_section():
    with open('results/e1/e1_summary_table.csv') as f:
        rows = list(csv.DictReader(f))

    out = []
    out.append("## 1. Experiment E1: Idle-Then-Burst Progression & Instantaneous Burst Acceptance\n\n")

    for c in [100.0, 1000.0]:
        for init in ['coldStart', 'drainedFirst']:
            c_int = int(c)
            out.append(f"### Table 1.{1 if c==100 and init=='coldStart' else (2 if c==100 else (3 if init=='coldStart' else 4))}: E1 Idle Progression (C={c_int}, {init}, Burst Ratio B/C = 1.0)\n\n")
            out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: {c_int}, Initial State: {init}, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")
            out.append("| Algorithm | Idle Duration D | Burst Requested | Admitted Tokens | Admitted / Offered % | Diff vs TB-small (Tokens) | Diff vs TB-large (Tokens) |\n")
            out.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: |\n")

            sub = [r for r in rows if float(r['capacity']) == c and r['initialState'] == init and float(r['burstRatio']) == 1.0]
            sub.sort(key=lambda r: (float(r['idleTau']), r['algorithm']))
            for r in sub:
                tau = float(r['idleTau'])
                req = int(float(r['burstRequested']))
                acc = float(r['accepted_mean'])
                pct = (acc / req) * 100.0
                d_s = float(r['diff_tb_small_mean'])
                d_l = float(r['diff_tb_large_mean'])
                out.append(f"| {r['algorithm']:<26} | {tau:4.2f}τ ({float(r['idleSec']):.3f}s) | {req:6d} | {acc:7.1f} | {pct:6.2f}% | {d_s:+7.1f} | {d_l:+7.1f} |\n")
            out.append("\n")

    out.append("### Table 1.5: Instantaneous Burst Acceptance per Variant (C=1000, coldStart, Idle D = 1.0τ)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")
    out.append("| Algorithm | B/C = 0.5 (500) | B/C = 0.8 (800) | B/C = 1.0 (1000) | B/C = 1.2 (1200) | B/C = 1.5 (1500) | B/C = 2.0 (2000) | B/C = 3.0 (3000) |\n")
    out.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n")

    d1_rows = [r for r in rows if float(r['capacity']) == 1000.0 and r['initialState'] == 'coldStart' and float(r['idleTau']) == 1.0]
    algos = sorted(set(r['algorithm'] for r in d1_rows))
    ratios = [0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0]
    for algo in algos:
        vals = []
        for rat in ratios:
            m = next((float(r['accepted_mean']) for r in d1_rows if r['algorithm'] == algo and abs(float(r['burstRatio']) - rat) < 1e-4), 0.0)
            vals.append(f"{m:6.0f}")
        out.append(f"| {algo:<26} | " + " | ".join(vals) + " |\n")
    out.append("\n---\n\n")
    return "".join(out)

def get_e2_section():
    out = []
    out.append("## 2. Experiment E2: Multi-Client Mixed Traffic & Contract Enforcement\n\n")
    out.append("### Table 2.1: Standard Abuser Performance (k=1.5, f=0.3, C=1000, coldStart)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: No (Stochastic Poisson and Exponential arrivals for legitimate traffic; deterministic periodic bursts for abusive traffic). Commit: `{COMMIT_HASH}`.*\n\n")

    with open('results/e2/README.md') as f:
        lines = f.readlines()

    t1_lines = []
    capture = False
    for line in lines:
        if "### Table 1: Standard Abuser" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---"):
                break
            if line.strip().startswith("|"):
                t1_lines.append(line)

    out.extend(t1_lines)
    out.append("\n")

    out.append("### Table 2.2: Admitted Rate vs TB-large and Contract Enforcement across k (f=0.3, C=1000, coldStart)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: Yes for abuser metrics. Commit: `{COMMIT_HASH}`.*\n\n")

    t4_lines = []
    capture = False
    for line in lines:
        if "### Table 4 (A5): DREB Admitted Rate vs TB-large and Contract" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---") or line.startswith("## "):
                break
            if line.strip().startswith("|"):
                t4_lines.append(line)

    out.extend(t4_lines)
    out.append("\n---\n\n")
    return "".join(out)

def get_e2_evasion_section():
    out = []
    out.append("## 3. Reclassified Evasion Scenario: Compliant Bursty Traffic at 0.78R\n\n")
    out.append("### Table 3.1: Compliant-Bursty False Rejection Rate (Offered Rate ≈ 0.78R <= R, f=0.3, C=1000, coldStart)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")

    with open('results/e2/README.md') as f:
        lines = f.readlines()

    t2_lines = []
    capture = False
    for line in lines:
        if "### Table 2: Reclassified Evasion Scenario" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---") or line.startswith("> **Note on Evasion"):
                break
            if line.strip().startswith("|"):
                t2_lines.append(line)

    out.extend(t2_lines)
    out.append("\n---\n\n")
    return "".join(out)

def get_e3_section():
    out = []
    out.append("## 4. Experiment E3: DREB v2 Ablation & Parameter Sweeps (Corrected CIs)\n\n")
    out.append("### Table 4.1: Full Ablation Summary across Capacities and Initial States (k=1.5, f=0.3)\n\n")
    out.append(f"*Caption: Held-Out Test Seeds: 2000–2029 (N=30), Capacity C: 1000 & 50000, Initial State: coldStart & preWarmed, Deterministic: No. Commit: `{COMMIT_HASH}`.*\n\n")

    with open('results/e3/README.md') as f:
        lines = f.readlines()

    t_lines = []
    capture = False
    for line in lines:
        if "## 2. Evaluation Results Summary" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---"):
                break
            if line.strip().startswith("|"):
                t_lines.append(line)

    out.extend(t_lines)
    out.append("\n")

    out.append("### Table 4.2: Generalization Across Overload k and Abusive Fraction f (C=1000, coldStart)\n\n")
    out.append(f"*Caption: Held-Out Test Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: No. Commit: `{COMMIT_HASH}`.*\n\n")

    t_gen = []
    capture = False
    for line in lines:
        if "## 3. Generalization Across Offered Rates" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---") or line.startswith("## 4."):
                break
            if line.strip().startswith("|"):
                t_gen.append(line)

    out.extend(t_gen)
    out.append("\n---\n\n")
    return "".join(out)

def get_e7_section():
    out = []
    out.append("## 5. Experiment E7: State Variable Mechanism Traces\n\n")
    out.append("### Table 5.1: Abuser State Variable Trace Metrics (k=1.5, burst=950, interval=0.633s, C=1000, R=1000, T=30.0s)\n\n")
    out.append(f"*Caption: Seed: 2000 (N=1), Capacity C: 1000, Initial State: coldStart, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")
    out.append("| Algorithm | Min sustained / S_max | Max abuse_score | Sustained Bucket Bindings | Burst Bucket Bindings | Total Admitted Tokens |\n")
    out.append("| :--- | :---: | :---: | :---: | :---: | :---: |\n")
    out.append("| Token Bucket (TB-large)  | 0.0000 | 0.0000 | 0 | 0 | 30,767 |\n")
    out.append("| Token Bucket (TB-env)    | 0.0000 | 0.0000 | 0 | 0 | 31,053 |\n")
    out.append("| DREB (proportional)      | 0.4730 | 0.0000 | 0 | 480 | 25,051 |\n")
    out.append("| DREB (burst-matched)     | 0.5357 | 0.0000 | 0 | 480 | 28,429 |\n")
    out.append("| DREB (v2 base 1.5)       | 0.5344 | 0.3860 | 0 | 480 | 28,308 |\n\n")

    out.append("### Table 5.2: Bursty-Legit Client Summary & Per-Burst Trace (C=1000, R=1000, Seed 2000)\n\n")
    out.append(f"*Caption: Seed: 2000 (N=1), Capacity C: 1000, Initial State: coldStart, Deterministic: Yes for trace sequence. Commit: `{COMMIT_HASH}`.*\n\n")
    out.append("| Algorithm | Offered Cost | Admitted Cost | Rejected Cost | False Rejection % |\n")
    out.append("| :--- | :---: | :---: | :---: | :---: |\n")
    out.append("| Token Bucket (TB-large)  | 9,470 | 7,175 | 2,295 | 24.23% |\n")
    out.append("| Token Bucket (TB-env)    | 9,756 | 7,461 | 2,295 | 23.52% |\n")
    out.append("| DREB (burst-matched)     | 10,939 | 7,071 | 3,868 | 35.36% |\n\n")
    out.append("---\n\n")
    return "".join(out)

def get_e8_section():
    out = []
    out.append("## 6. Experiment E8: Isolating the Idle Credit\n\n")
    out.append("### Table 6.1: Instantaneous Burst Acceptance after Idleness (Requests = 2C at t = D, C=1000)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart & preWarmed, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")

    with open('results/e8/README.md') as f:
        lines = f.readlines()

    t_lines = []
    capture = False
    for line in lines:
        if "## 3. Instantaneous Burst Acceptance" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---") or line.startswith("## 4."):
                break
            if line.strip().startswith("|"):
                t_lines.append(line)

    out.extend(t_lines)
    out.append("\n")

    out.append("### Table 6.2: Pareto Trade-off Points across k (C=1000, f=0.3, coldStart)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: No for legit FRR. Commit: `{COMMIT_HASH}`.*\n\n")

    # Extract Table for k=1.5 and summary across k
    capture = False
    t_trade = []
    for line in lines:
        if "### Table 1: Pareto Metrics at C = 1000, k = 1.50" in line:
            capture = True
            continue
        if capture:
            if line.startswith("---") or line.startswith("### Table 2:"):
                break
            if line.strip().startswith("|"):
                t_trade.append(line)

    out.extend(t_trade)
    out.append("\n---\n\n")
    return "".join(out)

def get_e9_section():
    out = []
    out.append("## 7. Experiment E9: Static Baseline Fairness & Bursty-Legit Sensitivity\n\n")

    with open('results/e9/README.md') as f:
        content = f.read()

    # Extract Section 5 (45-row sensitivity table)
    out.append("### Table 7.1: Full 45-Setting Per-Setting Verification for H9c (9 Legit Models x 5 k Values, C=1000, coldStart)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: No for bursty FRR. Commit: `{COMMIT_HASH}`.*\n\n")

    sec5_match = re.search(r'## 5\. Full Per-Setting Table Supporting H9c.*?\n\n(.*?)(?=\n### 5\.1|\n---)', content, re.DOTALL)
    if sec5_match:
        out.append(sec5_match.group(1).strip() + "\n\n")

    # Extract Section 5.1 (coldStart vs preWarmed breakdown)
    out.append("### Table 7.2: Dominance Breakdown per k: coldStart vs preWarmed (C=1000, Abuser Burst = 0.95C)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart vs preWarmed, Deterministic: No for bursty FRR. Commit: `{COMMIT_HASH}`.*\n\n")

    sec51_match = re.search(r'### 5\.1 Dominance Verdict per k: coldStart vs preWarmed.*?\n\n(.*?)(?=\n---|\n## 6\.)', content, re.DOTALL)
    if sec51_match:
        out.append(sec51_match.group(1).strip() + "\n\n")

    # Extract Section 6 (Abuser burst size sensitivity)
    out.append("### Table 7.3: Abuser Burst-Size Sensitivity Sweep across k and Burst Sizes (C=1000, coldStart & preWarmed)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart & preWarmed, Deterministic: No for bursty FRR. Commit: `{COMMIT_HASH}`.*\n\n")

    sec6_match = re.search(r'## 6\. Abuser Burst-Size Sensitivity.*?\n\n(.*?)(?=\n\*\*First Burst Size|\n---)', content, re.DOTALL)
    if sec6_match:
        out.append(sec6_match.group(1).strip() + "\n\n")

    out.append("---\n\n")
    return "".join(out)

def get_e5_e6_section():
    out = []
    out.append("## 8. Experiments E5 & E6: Boundary Compliance & Variable Cost Overdraw\n\n")

    out.append("### Table 8.1: Experiment E5 Boundary Compliance (C=1000, Periodic & Poisson Arrivals across k in {0.90R, 0.98R, 1.00R, 1.02R, 1.10R})\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: coldStart, Deterministic: Yes for periodic, No for Poisson. Commit: `{COMMIT_HASH}`.*\n\n")

    with open('results/e5/README.md') as f:
        e5_content = f.read()

    sec2_match = re.search(r'## 2\. Comprehensive Boundary Results \(C = 1000\)\n\n(.*?)(?=\n## 3\.|\Z)', e5_content, re.DOTALL)
    if sec2_match:
        out.append(sec2_match.group(1).strip() + "\n\n")

    out.append("### Table 8.2: Experiment E6 Post-Idle Single-Request Admission Boundary Test (Pre-Warmed, C=1000)\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Initial State: preWarmed, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")

    with open('results/e6/README.md') as f:
        e6_content = f.read()

    e6_sec2 = re.search(r'## 2\. Post-Idle Single-Request Admission Boundary Test.*?\n\n(.*?)(?=\n---|\n## 3\.)', e6_content, re.DOTALL)
    if e6_sec2:
        out.append(e6_sec2.group(1).strip() + "\n\n")

    out.append("### Table 8.3: Experiment E6 Variable Cost Requests Admitted Cost and FRR by Cost Class\n\n")
    out.append(f"*Caption: Seeds: 2000–2029 (N=30), Capacity C: 1000, Request Costs in {1, 5, 20, 60, 200}, Initial State: coldStart, Deterministic: No. Commit: `{COMMIT_HASH}`.*\n\n")

    e6_sec3 = re.search(r'## 3\. Comparative Performance Overview under Variable Cost Traces\n\n(.*?)(?=\n---)', e6_content, re.DOTALL)
    if e6_sec3:
        out.append(e6_sec3.group(1).strip() + "\n\n")

    e6_sec4 = re.search(r'## 3\. False Rejection Rate \(FRR\) by Request Cost Class\n\n(.*?)(?=\Z)', e6_content, re.DOTALL)
    if e6_sec4:
        out.append(e6_sec4.group(1).strip() + "\n\n")

    out.append("---\n\n")
    return "".join(out)

def get_jmh_section():
    out = []
    out.append("## 9. JMH Performance, Latency & Resource Benchmarks\n\n")

    with open('results/perf/README.md') as f:
        perf_content = f.read()

    out.append("### Table 9.1: Batched Latency Distribution per allow() Call across Key Scales (Nanoseconds)\n\n")
    out.append(f"*Caption: Single Machine Relative Comparison, Apple M3 (8 Cores), OpenJDK 26.0.2 release 21, N=1000 Batch Mean Samples, Deterministic: No. Commit: `{COMMIT_HASH}`.*\n\n")

    sec1_match = re.search(r'## 1\. Latency Distribution per allow\(\) Call.*?\n\n(.*?)(?=\n---|\n## 2\.)', perf_content, re.DOTALL)
    if sec1_match:
        out.append(sec1_match.group(1).strip() + "\n\n")

    out.append("### Table 9.2: Multi-Threaded Throughput Scaling (1, 2, 4, 8 Threads for same-key and many-key with 99.9% CI)\n\n")
    out.append(f"*Caption: Single Machine Relative Comparison, Apple M3 (4P + 4E Cores), 99.9% Student-t CI across 5 intervals, Deterministic: No. Commit: `{COMMIT_HASH}`.*\n\n")

    sec2_match = re.search(r'## 2\. Multi-Threaded Throughput.*?\n\n(.*?)(?=\n### Architectural Scaling|\n---)', perf_content, re.DOTALL)
    if sec2_match:
        out.append(sec2_match.group(1).strip() + "\n\n")

    out.append("### Table 9.3: GC Memory Allocation per allow() Invocation (Total vs Algorithm Engine Alone)\n\n")
    out.append(f"*Caption: Single Machine Relative Comparison, JVM ThreadMXBean thread-allocated bytes per operation, Deterministic: Yes. Commit: `{COMMIT_HASH}`.*\n\n")

    sec3_match = re.search(r'## 3\. GC Memory Allocation per allow\(\) Invocation\n\n(.*?)(?=\n---|\n## 4\.)', perf_content, re.DOTALL)
    if sec3_match:
        out.append(sec3_match.group(1).strip() + "\n\n")

    out.append("### Table 9.4: Retained Memory Footprint per Client Key (10^3, 10^5, 10^6 Keys)\n\n")
    out.append(f"*Caption: Single Machine Relative Comparison, Heap delta after forced Full GC cycles, Deterministic: No. Commit: `{COMMIT_HASH}`.*\n\n")

    sec4_match = re.search(r'## 4\. Memory Footprint per Client Key.*?\n\n(.*?)(?=\n---|\n## 5\.)', perf_content, re.DOTALL)
    if sec4_match:
        out.append(sec4_match.group(1).strip() + "\n\n")

    return "".join(out)

if __name__ == '__main__':
    main()
