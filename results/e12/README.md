# Experiment E12: DREB v2 Gain Sweep & Ablation Completion

## 1. Executive Summary & Pre-Registered Hypothesis Verdict

Experiment E12 explores the 48-setting factorial parameter grid of DREB v2 across `abuseGain` $\in \{0, 0.15, 0.3, 0.6\}$, `abuseDecayRate` $\in \{0.25, 0.75, 1.5, 3.0\}$, and `sustainedCostGain` $\in \{0, 0.35, 1.0\}$ with base decay 1.5 under the standard multi-client workload of Experiment E2 ($k \in \{1.1, 1.5, 2.0\}$, $f=0.30$, $N=30$ seeds 2000–2029).

### Hypothesis H12 Verdict
> **Statement:** *"no v2 setting dominates both v1 and the best static dual bucket."*

- **Verdict:** **SUPPORTED** (judged across all 48 configurations, no tolerance margins).
- **Finding:** Exactly **0 out of 48** settings dominate both v1 burst-matched and the best static dual bucket (`DualBucket(B=0.85C, P=4.00R)` / `DualBucket(B=1.00C, P=4.00R)`). Parameter configurations that increase penalty gains (`abuseGain` $\ge 0.30$) lower abuser admitted rate but increase bursty legitimate false rejections, while configurations that relax gains to protect legitimate traffic admit higher abuser throughput. No configuration simultaneously beats or matches both references on both metrics.

### Effect of `sustainedCostGain`
- **Finding:** `sustainedCostGain` has a **measurable effect in 16 / 16 parameter groups**.
- In particular, at slow decay $D = 0.25$, increasing `sustainedCostGain` from 0.0 to 1.0 substantially curbs abuser throughput by up to $0.123R$ (e.g. from $0.9187R$ down to $0.7958R$), at the expense of increasing bursty legitimate FRR from $1.82\%$ to $5.21\%$. At faster decay rates ($D \ge 0.75$), the effect is subtle but consistent (reducing abuser rate by $\approx 0.0010R$ and increasing FRR by $0.1\%$ to $1.0\%$).

---

## 2. Table 12.1: DREB v2 48-Setting Factorial Grid & Empirical Pareto Frontier

| ID | AbuseGain | AbuseDecay | SustainedG | Abuser Rate / R | Bursty FRR % | Steady FRR % | Overall FRR % | Pareto Frontier? | Dominates v1? | Dominates DB? |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| #1   |      0.00 |       0.25 |       0.00 |        0.9187R |       1.82% |       0.00% |        0.34% | **YES**          | No            | No            |
| #2   |      0.00 |       0.25 |       0.35 |        0.7992R |       2.68% |       0.00% |        0.49% | **YES**          | No            | No            |
| #3   |      0.00 |       0.25 |       1.00 |        0.7958R |       5.21% |       0.00% |        0.96% | **YES**          | No            | No            |
| #4   |      0.00 |       0.75 |       0.00 |        0.9187R |       1.82% |       0.00% |        0.34% | **YES**          | No            | No            |
| #5   |      0.00 |       0.75 |       0.35 |        0.9184R |       2.01% |       0.00% |        0.37% | No               | No            | No            |
| #6   |      0.00 |       0.75 |       1.00 |        0.9178R |       2.98% |       0.00% |        0.55% | No               | No            | No            |
| #7   |      0.00 |       1.50 |       0.00 |        0.9187R |       1.82% |       0.00% |        0.34% | **YES**          | No            | No            |
| #8   |      0.00 |       1.50 |       0.35 |        0.9184R |       1.89% |       0.00% |        0.35% | No               | No            | No            |
| #9   |      0.00 |       1.50 |       1.00 |        0.9178R |       2.36% |       0.00% |        0.43% | No               | No            | No            |
| #10  |      0.00 |       3.00 |       0.00 |        0.9187R |       1.82% |       0.00% |        0.34% | **YES**          | No            | No            |
| #11  |      0.00 |       3.00 |       0.35 |        0.9184R |       1.84% |       0.00% |        0.34% | **YES**          | No            | No            |
| #12  |      0.00 |       3.00 |       1.00 |        0.9178R |       2.00% |       0.00% |        0.37% | No               | No            | No            |
| #13  |      0.15 |       0.25 |       0.00 |        0.8308R |       2.12% |       0.00% |        0.39% | **YES**          | No            | No            |
| #14  |      0.15 |       0.25 |       0.35 |        0.7993R |       2.77% |       0.00% |        0.51% | No               | No            | No            |
| #15  |      0.15 |       0.25 |       1.00 |        0.7958R |       5.22% |       0.00% |        0.96% | **YES**          | No            | No            |
| #16  |      0.15 |       0.75 |       0.00 |        0.9156R |       1.99% |       0.00% |        0.37% | No               | No            | No            |
| #17  |      0.15 |       0.75 |       0.35 |        0.9153R |       2.13% |       0.00% |        0.39% | No               | No            | No            |
| #18  |      0.15 |       0.75 |       1.00 |        0.9147R |       3.01% |       0.00% |        0.55% | No               | No            | No            |
| #19  |      0.15 |       1.50 |       0.00 |        0.9156R |       1.95% |       0.00% |        0.36% | No               | No            | No            |
| #20  |      0.15 |       1.50 |       0.35 |        0.9153R |       2.01% |       0.00% |        0.37% | No               | No            | No            |
| #21  |      0.15 |       1.50 |       1.00 |        0.9147R |       2.40% |       0.00% |        0.44% | No               | No            | No            |
| #22  |      0.15 |       3.00 |       0.00 |        0.9156R |       1.92% |       0.00% |        0.36% | **YES**          | No            | No            |
| #23  |      0.15 |       3.00 |       0.35 |        0.9153R |       1.94% |       0.00% |        0.36% | **YES**          | No            | No            |
| #24  |      0.15 |       3.00 |       1.00 |        0.9147R |       2.07% |       0.00% |        0.38% | No               | No            | No            |
| #25  |      0.30 |       0.25 |       0.00 |        0.8175R |       2.39% |       0.00% |        0.44% | **YES**          | No            | No            |
| #26  |      0.30 |       0.25 |       0.35 |        0.7993R |       2.88% |       0.00% |        0.53% | No               | No            | No            |
| #27  |      0.30 |       0.25 |       1.00 |        0.7959R |       5.23% |       0.00% |        0.96% | No               | No            | No            |
| #28  |      0.30 |       0.75 |       0.00 |        0.9128R |       2.15% |       0.00% |        0.40% | No               | No            | No            |
| #29  |      0.30 |       0.75 |       0.35 |        0.9124R |       2.26% |       0.00% |        0.42% | No               | No            | No            |
| #30  |      0.30 |       0.75 |       1.00 |        0.9118R |       3.04% |       0.00% |        0.56% | No               | No            | No            |
| #31  |      0.30 |       1.50 |       0.00 |        0.9128R |       2.07% |       0.00% |        0.38% | No               | No            | No            |
| #32  |      0.30 |       1.50 |       0.35 |        0.9124R |       2.11% |       0.00% |        0.39% | No               | No            | No            |
| #33  |      0.30 |       1.50 |       1.00 |        0.9118R |       2.46% |       0.00% |        0.45% | No               | No            | No            |
| #34  |      0.30 |       3.00 |       0.00 |        0.9128R |       2.02% |       0.00% |        0.37% | **YES**          | No            | No            |
| #35  |      0.30 |       3.00 |       0.35 |        0.9124R |       2.04% |       0.00% |        0.38% | **YES**          | No            | No            |
| #36  |      0.30 |       3.00 |       1.00 |        0.9118R |       2.14% |       0.00% |        0.39% | No               | No            | No            |
| #37  |      0.60 |       0.25 |       0.00 |        0.8086R |       2.91% |       0.00% |        0.54% | No               | No            | No            |
| #38  |      0.60 |       0.25 |       0.35 |        0.7994R |       3.18% |       0.00% |        0.59% | No               | No            | No            |
| #39  |      0.60 |       0.25 |       1.00 |        0.7964R |       5.26% |       0.00% |        0.97% | No               | No            | No            |
| #40  |      0.60 |       0.75 |       0.00 |        0.9074R |       2.42% |       0.00% |        0.45% | No               | No            | No            |
| #41  |      0.60 |       0.75 |       0.35 |        0.9071R |       2.50% |       0.00% |        0.46% | No               | No            | No            |
| #42  |      0.60 |       0.75 |       1.00 |        0.9065R |       3.13% |       0.00% |        0.58% | No               | No            | No            |
| #43  |      0.60 |       1.50 |       0.00 |        0.9074R |       2.28% |       0.00% |        0.42% | No               | No            | No            |
| #44  |      0.60 |       1.50 |       0.35 |        0.9071R |       2.31% |       0.00% |        0.43% | No               | No            | No            |
| #45  |      0.60 |       1.50 |       1.00 |        0.9065R |       2.58% |       0.00% |        0.47% | No               | No            | No            |
| #46  |      0.60 |       3.00 |       0.00 |        0.9074R |       2.21% |       0.00% |        0.41% | No               | No            | No            |
| #47  |      0.60 |       3.00 |       0.35 |        0.9071R |       2.21% |       0.00% |        0.41% | No               | No            | No            |
| #48  |      0.60 |       3.00 |       1.00 |        0.9065R |       2.28% |       0.00% |        0.42% | No               | No            | No            |
| REF  | DREB (burst-matched) [v1]         |        0.9187R |       1.82% |       0.00% |        0.91% | N/A              | N/A           | N/A           |
| REF  | DualBucket (B=0.85C, P=4.00R)     |        1.0003R |       1.58% |       0.00% |        0.79% | N/A              | N/A           | N/A           |

---

## 3. Table 12.2: Hypothesis H12 Evaluation Summary

| Hypothesis | Pre-Registered Statement | Evaluation Metric | Measured Result | Verdict |
| :--- | :--- | :--- | :--- | :---: |
| **H12** | *"no v2 setting dominates both v1 and the best static dual bucket."* | Joint Dominance Count over 48 Settings | Exactly **0 / 48** settings dominate both baselines | **SUPPORTED** |
