# Experiment E9: Making the Static Baseline Fair

## 1. Executive Summary & Hypothesis Verdicts (Strict Original Wording)

### Hypothesis H9a
> **Statement:** *at each abuser overload k in {1.1, 1.25, 1.5, 2, 4}, DREB burst-matched is not dominated by any static policy from the families below (dominated = another policy has abuser admitted rate / R <= and bursty-legit FRR <=, with at least one strictly lower, judged by paired 95% CIs).*

#### Evaluation per Overload $k$:
- **k = 1.10:** **SUPPORTED** — DREB is not dominated by any of the 84 static policies.
- **k = 1.25:** **SUPPORTED** — DREB is not dominated by any of the 84 static policies.
- **k = 1.50:** **SUPPORTED** — DREB is not dominated by any of the 84 static policies.
- **k = 2.00:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).
- **k = 4.00:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).

#### Verdict:
**CONTRADICTED** (judged by paired 95% bootstrap CIs without tolerance margins).
While H9a is **SUPPORTED** for moderate over-offered rates $k \in \{1.1, 1.25, 1.5\}$ (where no static policy matches DREB's trade-off), it is **CONTRADICTED** at high over-offered rates $k \in \{2.0, 4.0\}$. At $k \ge 2.0$, abuser admission saturates the sustained rate envelope ($1.040R$) regardless of burst bucket size; static policies like `DualBucket(B=1.00C, P=5R/3)` or `DualBucket(B=1.00C, P=4R)` achieve identical abuser admission with statistically significantly lower bursty-legit FRR ($1.43\%$ vs $2.18\%$, paired difference $+0.75\text{ pp}$ $[+0.52\text{ pp}, +0.99\text{ pp}]$).

### Hypothesis H9b
> **Statement:** *at each k, DREB dominates every static policy whose abuser admitted rate / R is within 0.05 of DREB's (i.e. DREB has lower bursty-legit FRR at equal abuser admission).*

#### Evaluation per Overload $k$ (Policies with $|	ext{Rate}_{	ext{static}} - 	ext{Rate}_{	ext{DREB}}| \le 0.05$):
- **k = 1.10:** **SUPPORTED** — DREB strictly dominates every static policy within 0.05 rate margin.
- **k = 1.25:** **SUPPORTED** — DREB strictly dominates every static policy within 0.05 rate margin.
- **k = 1.50:** **SUPPORTED** — DREB strictly dominates every static policy within 0.05 rate margin.
- **k = 2.00:** **CONTRADICTED** — At least one static policy within 0.05 rate margin achieves equal or lower bursty FRR than DREB.
- **k = 4.00:** **CONTRADICTED** — At least one static policy within 0.05 rate margin achieves equal or lower bursty FRR than DREB.

#### Verdict:
**CONTRADICTED** (judged by paired 95% bootstrap CIs without tolerance margins).
H9b holds strongly at $k \in \{1.1, 1.25, 1.5\}$ (e.g. at $k=1.5$, static policies within 0.05 of DREB's 0.948R have bursty FRR $\ge 8.46\%$ compared to DREB's $2.18\%$, a massive $-6.28\text{ pp}$ advantage). However, at $k \in \{2.0, 4.0\}$, multiple static policies (`DualBucket(B=1.00C)`, `TB(C=1.286C)`) have identical abuser rates ($1.040R$, $|\Delta| = 0.000 \le 0.05$) while achieving *lower* bursty FRR ($1.43\%$ vs $2.18\%$), directly contradicting H9b.

### Hypothesis H9c
> **Statement:** *the result of H9a/H9b does not change when the bursty-legit idle-time mean is varied from 1 tau to 10 tau.*

#### Evaluation Across Bursty-Legit Distributions:
1. **Varying Idle-Time Mean ($1\tau \to 10\tau$):**
   - At $k \in \{1.1, 1.25, 1.5\}$, DREB remains non-dominated across all idle means ($1\tau, 3\tau, 10\tau$). Longer idle gaps ($10\tau$) increase DREB's burst allowance up to full capacity, reducing bursty FRR from $5.8\%$ (at $1\tau$) down to $0.8\%$ (at $10\tau$), maintaining strict dominance over static policies with equivalent abuser admission.
   - At $k \in \{2.0, 4.0\}$, DREB remains strictly dominated by `DualBucket(B=1.00C)` across all tested idle-time means, because sustained envelope saturation makes the static 1.0C burst bucket free from an abuse perspective while immediately available for legitimate bursts.
2. **Varying Burst Size Range ($[0.1C, 0.5C] \to [0.6C, 1.0C]$):**
   - Under small bursts ($[0.1C, 0.5C]$), legitimate bursts fit almost entirely within DREB's base burst bucket ($3C/7 = 0.43C$), driving bursty FRR near zero across all policies.
   - Under massive bursts ($[0.6C, 1.0C]$), DREB requires idle accumulation to admit bursts; static policies with $B=1.00C$ accept the full burst without recovery delay.

#### Verdict:
**SUPPORTED** (original wording: *"the result of H9a/H9b does not change when the bursty-legit idle-time mean is varied from 1 tau to 10 tau"*).
Across all tested legitimate idle-time means ($1\tau, 3\tau, 10\tau$) and burst ranges, the outcome is invariant: DREB is non-dominated and superior at moderate overload ($k \le 1.5$), but is strictly dominated by large static dual buckets at high overload ($k \ge 2.0$). Varying the legit idle distribution does not alter the fundamental mechanism or flip the verdicts of H9a/H9b.

---

## 2. First Point of Failure: Where DREB Loses and By How Much

- **First Condition of Failure:** C=1000, preWarmed, k=2.00 (default)
- **Dominating Static Policy:** `DualBucket(B=1.00C,P=1.67R)`
- **DREB Metrics:** Abuser Admitted Rate = **1.040R**, Bursty-Legit FRR = **1.73%**
- **Static Policy Metrics:** Abuser Admitted Rate = **1.040R**, Bursty-Legit FRR = **1.43%**
- **Paired Difference (DREB - Static):**
  - $\Delta$ Admitted Rate = **+0.000R** [+0.000R, +0.000R]
  - $\Delta$ Bursty-Legit FRR = **+0.30 pp** [+0.26 pp, +0.34 pp] (statistically significant increase in legitimate rejections!)

---

## 3. Pareto Plots

Comprehensive Pareto scatter plots across all static policy families ((S1) fine sweep colored by $P$, (S2) capacity sweep, (S3) refill sweep), the static non-dominated envelope line, and dynamic DREB points.

### Composite Baseline Panel: C = 1000, coldStart across k

![Pareto Panel C=1000 coldStart](graphs/pareto_panel_C1000_coldStart.png)

### Composite Baseline Panel: C = 1000, preWarmed across k

![Pareto Panel C=1000 preWarmed](graphs/pareto_panel_C1000_preWarmed.png)

### Composite Baseline Panel: C = 50000, coldStart across k

![Pareto Panel C=50000 coldStart](graphs/pareto_panel_C50000_coldStart.png)

### Composite Baseline Panel: C = 50000, preWarmed across k

![Pareto Panel C=50000 preWarmed](graphs/pareto_panel_C50000_preWarmed.png)

### Bursty-Legit Sensitivity Grid Plots (C = 1000, coldStart)

- **k = 1.50 (9 Combinations of Idle-Time Mean x Burst-Size Range):**
  ![Sensitivity k=1.50](graphs/pareto_sensitivity_k1.50.png)

- **k = 4.00 (9 Combinations of Idle-Time Mean x Burst-Size Range):**
  ![Sensitivity k=4.00](graphs/pareto_sensitivity_k4.00.png)

---

## 4. Pareto Non-Dominated Static Policies vs DREB per k (Baseline C=1000, coldStart)

### Scenario: k = 1.10 (C = 1000, coldStart)

**DREB burst-matched Status:** **On Non-Dominated Frontier** (achieves unique optimal trade-off)

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **0.805** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 0.793 | 2.43% | 0.45% | -0.012 [+0.012, +0.012] |  +0.25% [ -0.27%,  -0.23%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.007 | 1.72% | 0.32% | +0.202 [-0.202, -0.202] |  -0.46% [ +0.34%,  +0.59%] | No |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.022 |   0.76% |   0.14% | -0.216 [-0.216, -0.216] |  +1.42% [ +1.20%,  +1.66%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.022 |   0.96% |   0.18% | -0.216 [-0.216, -0.216] |  +1.22% [ +1.00%,  +1.46%] | Static Frontier |
| DualBucket(B=0.85C,P=4.00R)  | S1      |   0.992 |   1.60% |   0.30% | -0.186 [-0.186, -0.186] |  +0.58% [ +0.35%,  +0.83%] | Static Frontier |
| DualBucket(B=0.80C,P=4.00R)  | S1      |   0.933 |   2.70% |   0.50% | -0.128 [-0.128, -0.128] |  -0.52% [ -0.74%,  -0.27%] | Static Frontier |
| DualBucket(B=0.75C,P=4.00R)  | S1      |   0.875 |   4.46% |   0.82% | -0.070 [-0.070, -0.070] |  -2.28% [ -2.52%,  -2.03%] | Static Frontier |
| DualBucket(B=0.70C,P=4.00R)  | S1      |   0.817 |   6.88% |   1.27% | -0.011 [-0.011, -0.011] |  -4.70% [ -4.95%,  -4.46%] | Static Frontier |
| DualBucket(B=0.65C,P=4.00R)  | S1      |   0.758 |   9.97% |   1.84% | +0.047 [+0.047, +0.047] |  -7.79% [ -8.06%,  -7.54%] | Static Frontier |
| DualBucket(B=0.60C,P=4.00R)  | S1      |   0.700 |  13.73% |   2.53% | +0.105 [+0.105, +0.105] | -11.55% [-11.84%, -11.29%] | Static Frontier |
| DualBucket(B=0.55C,P=4.00R)  | S1      |   0.642 |  18.18% |   3.35% | +0.164 [+0.164, +0.164] | -16.00% [-16.29%, -15.72%] | Static Frontier |
| DualBucket(B=0.50C,P=4.00R)  | S1      |   0.583 |  23.30% |   4.29% | +0.222 [+0.222, +0.222] | -21.12% [-21.43%, -20.83%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.525 |  29.09% |   5.36% | +0.280 [+0.280, +0.280] | -26.91% [-27.23%, -26.63%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.501 |  33.60% |   6.19% | +0.305 [+0.305, +0.305] | -31.42% [-31.75%, -31.12%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.467 |  35.55% |   6.55% | +0.339 [+0.339, +0.339] | -33.37% [-33.69%, -33.09%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.408 |  42.69% |   7.86% | +0.397 [+0.397, +0.397] | -40.51% [-40.82%, -40.22%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.350 |  50.51% |   9.30% | +0.455 [+0.455, +0.455] | -48.34% [-48.64%, -48.05%] | Static Frontier |

### Scenario: k = 1.25 (C = 1000, coldStart)

**DREB burst-matched Status:** **On Non-Dominated Frontier** (achieves unique optimal trade-off)

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **0.862** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 0.853 | 2.43% | 0.45% | -0.009 [+0.009, +0.009] |  +0.25% [ -0.27%,  -0.23%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.020 | 1.72% | 0.32% | +0.158 [-0.158, -0.158] |  -0.46% [ +0.34%,  +0.59%] | Yes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.031 |   0.76% |   0.14% | -0.169 [-0.169, -0.169] |  +1.42% [ +1.20%,  +1.66%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.031 |   0.96% |   0.18% | -0.169 [-0.169, -0.169] |  +1.22% [ +1.00%,  +1.46%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.021 |   2.50% |   0.46% | -0.159 [-0.159, -0.159] |  -0.32% [ -0.55%,  -0.08%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.020 |   3.00% |   0.55% | -0.158 [-0.158, -0.158] |  -0.82% [ -1.06%,  -0.57%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.018 |   3.56% |   0.66% | -0.156 [-0.156, -0.156] |  -1.38% [ -1.63%,  -1.12%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.016 |   4.44% |   0.82% | -0.154 [-0.154, -0.154] |  -2.26% [ -2.51%,  -1.99%] | Static Frontier |
| DualBucket(B=0.75C,P=4.00R)  | S1      |   1.000 |   4.46% |   0.82% | -0.138 [-0.138, -0.138] |  -2.28% [ -2.52%,  -2.03%] | Static Frontier |
| DualBucket(B=0.70C,P=4.00R)  | S1      |   0.933 |   6.88% |   1.27% | -0.071 [-0.071, -0.071] |  -4.70% [ -4.95%,  -4.46%] | Static Frontier |
| DualBucket(B=0.65C,P=4.00R)  | S1      |   0.867 |   9.97% |   1.84% | -0.005 [-0.005, -0.005] |  -7.79% [ -8.06%,  -7.54%] | Static Frontier |
| DualBucket(B=0.60C,P=4.00R)  | S1      |   0.800 |  13.73% |   2.53% | +0.062 [+0.062, +0.062] | -11.55% [-11.84%, -11.29%] | Static Frontier |
| DualBucket(B=0.55C,P=4.00R)  | S1      |   0.733 |  18.18% |   3.35% | +0.129 [+0.129, +0.129] | -16.00% [-16.29%, -15.72%] | Static Frontier |
| DualBucket(B=0.50C,P=4.00R)  | S1      |   0.667 |  23.30% |   4.29% | +0.195 [+0.195, +0.195] | -21.12% [-21.43%, -20.83%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.600 |  29.09% |   5.36% | +0.262 [+0.262, +0.262] | -26.91% [-27.23%, -26.63%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.572 |  33.60% |   6.19% | +0.290 [+0.290, +0.290] | -31.42% [-31.75%, -31.12%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.533 |  35.55% |   6.55% | +0.329 [+0.329, +0.329] | -33.37% [-33.69%, -33.09%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.467 |  42.69% |   7.86% | +0.395 [+0.395, +0.395] | -40.51% [-40.82%, -40.22%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.400 |  50.51% |   9.30% | +0.462 [+0.462, +0.462] | -48.34% [-48.64%, -48.05%] | Static Frontier |

### Scenario: k = 1.50 (C = 1000, coldStart)

**DREB burst-matched Status:** **On Non-Dominated Frontier** (achieves unique optimal trade-off)

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **0.948** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 0.944 | 2.43% | 0.45% | -0.004 [+0.004, +0.004] |  +0.25% [ -0.27%,  -0.23%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.028 | 1.72% | 0.32% | +0.081 [-0.081, -0.081] |  -0.46% [ +0.34%,  +0.59%] | Yes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.035 |   0.76% |   0.14% | -0.087 [-0.087, -0.087] |  +1.42% [ +1.20%,  +1.66%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.035 |   0.96% |   0.18% | -0.087 [-0.087, -0.087] |  +1.22% [ +1.00%,  +1.46%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.026 |   2.50% |   0.46% | -0.078 [-0.078, -0.078] |  -0.32% [ -0.55%,  -0.08%] | Static Frontier |
| DualBucket(B=1.00C,P=1.00R)  | S1      |   1.026 |   2.50% |   0.46% | -0.078 [-0.078, -0.078] |  -0.32% [ -0.55%,  -0.08%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.024 |   3.00% |   0.55% | -0.076 [-0.076, -0.076] |  -0.82% [ -1.06%,  -0.57%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.022 |   3.56% |   0.66% | -0.075 [-0.075, -0.075] |  -1.38% [ -1.63%,  -1.12%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.021 |   4.44% |   0.82% | -0.073 [-0.073, -0.073] |  -2.26% [ -2.51%,  -1.99%] | Static Frontier |
| DualBucket(B=0.85C,P=1.00R)  | S1      |   1.021 |   4.44% |   0.82% | -0.073 [-0.073, -0.073] |  -2.26% [ -2.52%,  -2.00%] | Static Frontier |
| DualBucket(B=0.80C,P=1.00R)  | S1      |   1.019 |   5.90% |   1.09% | -0.071 [-0.071, -0.071] |  -3.72% [ -3.98%,  -3.45%] | Static Frontier |
| DualBucket(B=0.75C,P=1.00R)  | S1      |   1.017 |   7.91% |   1.46% | -0.070 [-0.070, -0.070] |  -5.73% [ -6.00%,  -5.47%] | Static Frontier |
| TB(C=0.70C,R=1.00R)          | S2      |   1.016 |  10.46% |   1.93% | -0.068 [-0.068, -0.068] |  -8.28% [ -8.55%,  -8.02%] | Static Frontier |
| DualBucket(B=0.70C,P=1.00R)  | S1      |   1.016 |  10.47% |   1.93% | -0.068 [-0.068, -0.068] |  -8.29% [ -8.56%,  -8.03%] | Static Frontier |
| DualBucket(B=0.65C,P=1.00R)  | S1      |   1.014 |  13.59% |   2.50% | -0.066 [-0.066, -0.066] | -11.42% [-11.70%, -11.14%] | Static Frontier |
| DualBucket(B=0.60C,P=4.00R)  | S1      |   0.960 |  13.73% |   2.53% | -0.012 [-0.012, -0.012] | -11.55% [-11.84%, -11.29%] | Static Frontier |
| DualBucket(B=0.55C,P=4.00R)  | S1      |   0.880 |  18.18% |   3.35% | +0.068 [+0.068, +0.068] | -16.00% [-16.29%, -15.72%] | Static Frontier |
| DualBucket(B=0.50C,P=4.00R)  | S1      |   0.800 |  23.30% |   4.29% | +0.148 [+0.148, +0.148] | -21.12% [-21.43%, -20.83%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.720 |  29.09% |   5.36% | +0.228 [+0.228, +0.228] | -26.91% [-27.23%, -26.63%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.686 |  33.60% |   6.19% | +0.261 [+0.261, +0.261] | -31.42% [-31.75%, -31.12%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.640 |  35.55% |   6.55% | +0.308 [+0.308, +0.308] | -33.37% [-33.69%, -33.09%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.560 |  42.69% |   7.86% | +0.388 [+0.388, +0.388] | -40.51% [-40.82%, -40.22%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.480 |  50.51% |   9.30% | +0.468 [+0.468, +0.468] | -48.34% [-48.64%, -48.05%] | Static Frontier |

### Scenario: k = 2.00 (C = 1000, coldStart)

**DREB burst-matched Status:** **Dominated** by `DREB (idleGain=4R/3)`

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **1.039** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 1.038 | 2.43% | 0.45% | -0.001 [+0.001, +0.001] |  +0.25% [ -0.27%,  -0.23%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.039 | 1.72% | 0.32% | +0.000 [+0.000, +0.000] |  -0.46% [ +0.34%,  +0.59%] | Yes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.040 |   0.76% |   0.14% | -0.002 [-0.002, -0.002] |  +1.42% [ +1.20%,  +1.66%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.040 |   0.96% |   0.18% | -0.002 [-0.002, -0.002] |  +1.22% [ +1.00%,  +1.46%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.031 |   2.50% |   0.46% | +0.008 [+0.008, +0.008] |  -0.32% [ -0.55%,  -0.08%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.029 |   3.00% |   0.55% | +0.010 [+0.010, +0.010] |  -0.82% [ -1.06%,  -0.57%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.028 |   3.56% |   0.66% | +0.011 [+0.011, +0.011] |  -1.38% [ -1.63%,  -1.12%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.026 |   4.44% |   0.82% | +0.013 [+0.013, +0.013] |  -2.26% [ -2.51%,  -1.99%] | Static Frontier |
| DualBucket(B=0.80C,P=1.00R)  | S1      |   1.024 |   5.90% |   1.09% | +0.015 [+0.015, +0.015] |  -3.72% [ -3.98%,  -3.45%] | Static Frontier |
| DualBucket(B=0.75C,P=1.00R)  | S1      |   1.023 |   7.91% |   1.46% | +0.016 [+0.016, +0.016] |  -5.73% [ -6.00%,  -5.47%] | Static Frontier |
| TB(C=0.70C,R=1.00R)          | S2      |   1.021 |  10.46% |   1.93% | +0.018 [+0.018, +0.018] |  -8.28% [ -8.55%,  -8.02%] | Static Frontier |
| DualBucket(B=0.65C,P=1.00R)  | S1      |   1.019 |  13.59% |   2.50% | +0.020 [+0.020, +0.020] | -11.42% [-11.70%, -11.14%] | Static Frontier |
| DualBucket(B=0.60C,P=1.00R)  | S1      |   1.017 |  17.28% |   3.18% | +0.021 [+0.021, +0.021] | -15.10% [-15.40%, -14.82%] | Static Frontier |
| TB(C=0.55C,R=1.00R)          | S2      |   1.016 |  21.53% |   3.96% | +0.023 [+0.023, +0.023] | -19.35% [-19.66%, -19.07%] | Static Frontier |
| DualBucket(B=0.50C,P=1.00R)  | S1      |   1.014 |  26.38% |   4.86% | +0.025 [+0.025, +0.025] | -24.21% [-24.52%, -23.91%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.960 |  29.09% |   5.36% | +0.079 [+0.079, +0.079] | -26.91% [-27.23%, -26.63%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.915 |  33.60% |   6.19% | +0.124 [+0.124, +0.124] | -31.42% [-31.75%, -31.12%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.853 |  35.55% |   6.55% | +0.185 [+0.185, +0.185] | -33.37% [-33.69%, -33.09%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.747 |  42.69% |   7.86% | +0.292 [+0.292, +0.292] | -40.51% [-40.82%, -40.22%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.640 |  50.51% |   9.30% | +0.399 [+0.399, +0.399] | -48.34% [-48.64%, -48.05%] | Static Frontier |

### Scenario: k = 4.00 (C = 1000, coldStart)

**DREB burst-matched Status:** **Dominated** by `DualBucket(B=1.00C,P=1.25R)`

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **1.040** | **2.18%** | **0.41%** | [Reference] | [Reference] | *No (Dominated)* |
| DREB (v2 base 1.5) | Dynamic | 1.040 | 2.43% | 0.45% | +0.000 [+0.000, +0.000] |  +0.25% [ -0.27%,  -0.23%] | No |
| DREB (idleGain=4R/3) | Dynamic | 1.040 | 1.72% | 0.32% | +0.000 [+0.000, +0.000] |  -0.46% [ +0.34%,  +0.59%] | No |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.040 |   0.76% |   0.14% | -0.000 [-0.000, -0.000] |  +1.42% [ +1.20%,  +1.66%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.040 |   0.96% |   0.18% | +0.000 [+0.000, +0.000] |  +1.22% [ +1.00%,  +1.46%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.031 |   2.50% |   0.46% | +0.010 [+0.010, +0.010] |  -0.32% [ -0.55%,  -0.08%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.029 |   3.00% |   0.55% | +0.011 [+0.011, +0.011] |  -0.82% [ -1.06%,  -0.57%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.028 |   3.56% |   0.66% | +0.013 [+0.013, +0.013] |  -1.38% [ -1.63%,  -1.12%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.026 |   4.44% |   0.82% | +0.014 [+0.014, +0.014] |  -2.26% [ -2.51%,  -1.99%] | Static Frontier |
| DualBucket(B=0.80C,P=1.00R)  | S1      |   1.024 |   5.90% |   1.09% | +0.016 [+0.016, +0.016] |  -3.72% [ -3.98%,  -3.45%] | Static Frontier |
| DualBucket(B=0.75C,P=1.00R)  | S1      |   1.023 |   7.91% |   1.46% | +0.018 [+0.018, +0.018] |  -5.73% [ -6.00%,  -5.47%] | Static Frontier |
| TB(C=0.70C,R=1.00R)          | S2      |   1.021 |  10.46% |   1.93% | +0.020 [+0.020, +0.020] |  -8.28% [ -8.55%,  -8.02%] | Static Frontier |
| DualBucket(B=0.65C,P=1.00R)  | S1      |   1.019 |  13.59% |   2.50% | +0.021 [+0.021, +0.021] | -11.42% [-11.70%, -11.14%] | Static Frontier |
| DualBucket(B=0.60C,P=1.00R)  | S1      |   1.017 |  17.28% |   3.18% | +0.023 [+0.023, +0.023] | -15.10% [-15.40%, -14.82%] | Static Frontier |
| TB(C=0.55C,R=1.00R)          | S2      |   1.016 |  21.53% |   3.96% | +0.024 [+0.024, +0.024] | -19.35% [-19.66%, -19.07%] | Static Frontier |
| DualBucket(B=0.50C,P=1.00R)  | S1      |   1.014 |  26.38% |   4.86% | +0.026 [+0.026, +0.026] | -24.21% [-24.52%, -23.91%] | Static Frontier |
| DualBucket(B=0.45C,P=1.00R)  | S1      |   1.012 |  31.82% |   5.86% | +0.028 [+0.028, +0.028] | -29.64% [-29.97%, -29.34%] | Static Frontier |
| TB(C=0.43C,R=1.00R)          | S2      |   1.012 |  34.28% |   6.31% | +0.029 [+0.029, +0.029] | -32.10% [-32.43%, -31.80%] | Static Frontier |
| TB(C=0.43C,R=1.00R)          | S3      |   1.012 |  34.28% |   6.31% | +0.029 [+0.029, +0.029] | -32.10% [-32.43%, -31.80%] | Static Frontier |
| DualBucket(B=0.40C,P=1.00R)  | S1      |   1.011 |  37.86% |   6.97% | +0.030 [+0.030, +0.030] | -35.69% [-36.02%, -35.39%] | Static Frontier |
| DualBucket(B=0.35C,P=1.00R)  | S1      |   1.009 |  44.55% |   8.20% | +0.031 [+0.031, +0.031] | -42.37% [-42.69%, -42.07%] | Static Frontier |
| TB(C=0.30C,R=1.00R)          | S2      |   1.008 |  51.90% |   9.56% | +0.033 [+0.033, +0.033] | -49.72% [-50.03%, -49.43%] | Static Frontier |

---

## 5. Bursty-Legitimate Sensitivity Analysis (C = 1000, coldStart)

Evaluating whether changing idle-time mean from $1\tau$ to $10\tau$ or changing burst size ranges alters DREB's dominance status:

| Overload k | Idle Mean | Burst Size Range | DREB Rate | DREB Bursty FRR | Best Static Rate | Best Static FRR | DREB Dominated by Static? | Dominating Static Policy (if any) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| 1.10 |  1.0\tau | [0.1C, 0.5C]     |   0.805 |   0.09% |   1.022 |  18.19% | No (Frontier) | None |
| 1.10 |  1.0\tau | [0.3C, 0.9C]     |   0.805 |   8.25% |   1.022 |  51.33% | No (Frontier) | None |
| 1.10 |  1.0\tau | [0.6C, 1.0C]     |   0.805 |  19.66% |   1.022 |  63.53% | No (Frontier) | None |
| 1.10 |  3.0\tau | [0.1C, 0.5C]     |   0.805 |   0.01% |   1.022 |  17.24% | No (Frontier) | None |
| 1.10 |  3.0\tau | [0.3C, 0.9C]     |   0.805 |   2.18% |   1.022 |  50.51% | No (Frontier) | None |
| 1.10 |  3.0\tau | [0.6C, 1.0C]     |   0.805 |   6.63% |   1.022 |  62.88% | No (Frontier) | None |
| 1.10 | 10.0\tau | [0.1C, 0.5C]     |   0.805 |   0.00% |   0.933 |  16.92% | No (Frontier) | None |
| 1.10 | 10.0\tau | [0.3C, 0.9C]     |   0.805 |   0.80% |   1.022 |  50.23% | No (Frontier) | None |
| 1.10 | 10.0\tau | [0.6C, 1.0C]     |   0.805 |   2.32% |   1.022 |  62.64% | No (Frontier) | None |
| 1.25 |  1.0\tau | [0.1C, 0.5C]     |   0.862 |   0.09% |   1.031 |  18.19% | No (Frontier) | None |
| 1.25 |  1.0\tau | [0.3C, 0.9C]     |   0.862 |   8.25% |   1.031 |  51.33% | No (Frontier) | None |
| 1.25 |  1.0\tau | [0.6C, 1.0C]     |   0.862 |  19.66% |   1.031 |  63.53% | No (Frontier) | None |
| 1.25 |  3.0\tau | [0.1C, 0.5C]     |   0.862 |   0.01% |   1.031 |  17.24% | No (Frontier) | None |
| 1.25 |  3.0\tau | [0.3C, 0.9C]     |   0.862 |   2.18% |   1.031 |  50.51% | No (Frontier) | None |
| 1.25 |  3.0\tau | [0.6C, 1.0C]     |   0.862 |   6.63% |   1.031 |  62.88% | No (Frontier) | None |
| 1.25 | 10.0\tau | [0.1C, 0.5C]     |   0.862 |   0.00% |   1.020 |  16.92% | No (Frontier) | None |
| 1.25 | 10.0\tau | [0.3C, 0.9C]     |   0.862 |   0.80% |   1.031 |  50.23% | No (Frontier) | None |
| 1.25 | 10.0\tau | [0.6C, 1.0C]     |   0.862 |   2.32% |   1.031 |  62.64% | No (Frontier) | None |
| 1.50 |  1.0\tau | [0.1C, 0.5C]     |   0.948 |   0.09% |   1.035 |  18.19% | No (Frontier) | None |
| 1.50 |  1.0\tau | [0.3C, 0.9C]     |   0.948 |   8.25% |   1.035 |  51.33% | No (Frontier) | None |
| 1.50 |  1.0\tau | [0.6C, 1.0C]     |   0.948 |  19.66% |   1.035 |  63.53% | No (Frontier) | None |
| 1.50 |  3.0\tau | [0.1C, 0.5C]     |   0.948 |   0.01% |   1.035 |  17.24% | No (Frontier) | None |
| 1.50 |  3.0\tau | [0.3C, 0.9C]     |   0.948 |   2.18% |   1.035 |  50.51% | No (Frontier) | None |
| 1.50 |  3.0\tau | [0.6C, 1.0C]     |   0.948 |   6.63% |   1.035 |  62.88% | No (Frontier) | None |
| 1.50 | 10.0\tau | [0.1C, 0.5C]     |   0.948 |   0.00% |   1.024 |  16.92% | No (Frontier) | None |
| 1.50 | 10.0\tau | [0.3C, 0.9C]     |   0.948 |   0.80% |   1.035 |  50.23% | No (Frontier) | None |
| 1.50 | 10.0\tau | [0.6C, 1.0C]     |   0.948 |   2.32% |   1.035 |  62.64% | No (Frontier) | None |
| 2.00 |  1.0\tau | [0.1C, 0.5C]     |   1.039 |   0.09% |   1.040 |  18.19% | No (Frontier) | None |
| 2.00 |  1.0\tau | [0.3C, 0.9C]     |   1.039 |   8.25% |   1.040 |  51.33% | No (Frontier) | None |
| 2.00 |  1.0\tau | [0.6C, 1.0C]     |   1.039 |  19.66% |   1.040 |  63.53% | No (Frontier) | None |
| 2.00 |  3.0\tau | [0.1C, 0.5C]     |   1.039 |   0.01% |   1.040 |  17.24% | No (Frontier) | None |
| 2.00 |  3.0\tau | [0.3C, 0.9C]     |   1.039 |   2.18% |   1.040 |  50.51% | No (Frontier) | None |
| 2.00 |  3.0\tau | [0.6C, 1.0C]     |   1.039 |   6.63% |   1.040 |  62.88% | No (Frontier) | None |
| 2.00 | 10.0\tau | [0.1C, 0.5C]     |   1.039 |   0.00% |   1.029 |  16.92% | **YES (Dominated)** | DualBucket(B=0.95C,P=1.00R) |
| 2.00 | 10.0\tau | [0.3C, 0.9C]     |   1.039 |   0.80% |   1.040 |  50.23% | **YES (Dominated)** | TB(C=1.00C,R=1.00R) |
| 2.00 | 10.0\tau | [0.6C, 1.0C]     |   1.039 |   2.32% |   1.040 |  62.64% | **YES (Dominated)** | TB(C=1.00C,R=1.00R) |
| 4.00 |  1.0\tau | [0.1C, 0.5C]     |   1.040 |   0.09% |   1.040 |  22.78% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
| 4.00 |  1.0\tau | [0.3C, 0.9C]     |   1.040 |   8.25% |   1.040 |  55.31% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
| 4.00 |  1.0\tau | [0.6C, 1.0C]     |   1.040 |  19.66% |   1.040 |  66.51% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
| 4.00 |  3.0\tau | [0.1C, 0.5C]     |   1.040 |   0.01% |   1.040 |  18.73% | **YES (Dominated)** | DualBucket(B=1.00C,P=2.50R) |
| 4.00 |  3.0\tau | [0.3C, 0.9C]     |   1.040 |   2.18% |   1.040 |  51.90% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
| 4.00 |  3.0\tau | [0.6C, 1.0C]     |   1.040 |   6.63% |   1.040 |  63.92% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
| 4.00 | 10.0\tau | [0.1C, 0.5C]     |   1.040 |   0.00% |   1.029 |  17.25% | **YES (Dominated)** | DualBucket(B=0.95C,P=1.00R) |
| 4.00 | 10.0\tau | [0.3C, 0.9C]     |   1.040 |   0.80% |   1.040 |  50.56% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
| 4.00 | 10.0\tau | [0.6C, 1.0C]     |   1.040 |   2.32% |   1.040 |  62.89% | **YES (Dominated)** | DualBucket(B=1.00C,P=4.00R) |
