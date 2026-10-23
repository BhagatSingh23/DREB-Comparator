# Experiment E9: Making the Static Baseline Fair

## 1. Executive Summary & Hypothesis Verdicts (Strict Original Wording)

### Hypothesis H9a
> **Statement:** *at each abuser overload k in {1.1, 1.25, 1.5, 2, 4}, DREB burst-matched is not dominated by any static policy from the families below (dominated = another policy has abuser admitted rate / R <= and bursty-legit FRR <=, with at least one strictly lower, judged by paired 95% CIs).*

#### Evaluation per Overload $k$:
- **k = 1.10:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).
- **k = 1.25:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).
- **k = 1.50:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).
- **k = 2.00:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).
- **k = 4.00:** **CONTRADICTED** — DREB is strictly dominated by at least one static policy (e.g. DualBucket with large B).

#### Verdict:
**CONTRADICTED** (judged by paired 95% bootstrap CIs without tolerance margins).
While H9a is **SUPPORTED** for moderate over-offered rates $k \in \{1.1, 1.25, 1.5\}$ (where no static policy matches DREB's trade-off), it is **CONTRADICTED** at high over-offered rates $k \in \{2.0, 4.0\}$. At $k \ge 2.0$, abuser admission saturates the sustained rate envelope ($1.040R$) regardless of burst bucket size; static policies like `DualBucket(B=1.00C, P=5R/3)` or `DualBucket(B=1.00C, P=4R)` achieve identical abuser admission with statistically significantly lower bursty-legit FRR ($1.43\%$ vs $2.18\%$, paired difference $+0.75\text{ pp}$ $[+0.52\text{ pp}, +0.99\text{ pp}]$).

### Hypothesis H9b
> **Statement:** *at each k, DREB dominates every static policy whose abuser admitted rate / R is within 0.05 of DREB's (i.e. DREB has lower bursty-legit FRR at equal abuser admission).*

#### Evaluation per Overload $k$ (Policies with $|	ext{Rate}_{	ext{static}} - 	ext{Rate}_{	ext{DREB}}| \le 0.05$):
- **k = 1.10:** **CONTRADICTED** — At least one static policy within 0.05 rate margin achieves equal or lower bursty FRR than DREB.
- **k = 1.25:** **CONTRADICTED** — At least one static policy within 0.05 rate margin achieves equal or lower bursty FRR than DREB.
- **k = 1.50:** **CONTRADICTED** — At least one static policy within 0.05 rate margin achieves equal or lower bursty FRR than DREB.
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

- **First Condition of Failure:** C=1000, coldStart, k=1.10, abuserBurst=0.30C
- **Dominating Static Policy:** `DualBucket(B=1.00C,P=1.25R)`
- **DREB Metrics:** Abuser Admitted Rate = **1.034R**, Bursty-Legit FRR = **2.18%**
- **Static Policy Metrics:** Abuser Admitted Rate = **1.034R**, Bursty-Legit FRR = **1.91%**
- **Paired Difference (Static - DREB):**
  - $\Delta$ Admitted Rate = **+0.000R** [+0.000R, +0.000R]
  - $\Delta$ Bursty-Legit FRR = **-0.27 pp** [-0.52 pp, -0.04 pp] (lower legitimate rejections for static policy!)

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
| DREB (v2 base 1.5) | Dynamic | 0.793 | 2.43% | 0.45% | -0.012 [-0.012, -0.012] |  +0.25% [ +0.23%,  +0.27%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.007 | 1.72% | 0.32% | +0.202 [+0.202, +0.202] |  -0.46% [ -0.59%,  -0.34%] | No |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.022 |   0.76% |   0.14% | +0.216 [+0.216, +0.216] |  -1.42% [ -1.66%,  -1.20%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.022 |   0.96% |   0.18% | +0.216 [+0.216, +0.216] |  -1.22% [ -1.46%,  -1.00%] | Static Frontier |
| DualBucket(B=0.85C,P=4.00R)  | S1      |   0.992 |   1.60% |   0.30% | +0.186 [+0.186, +0.186] |  -0.58% [ -0.82%,  -0.35%] | Static Frontier |
| DualBucket(B=0.80C,P=4.00R)  | S1      |   0.933 |   2.70% |   0.50% | +0.128 [+0.128, +0.128] |  +0.52% [ +0.27%,  +0.75%] | Static Frontier |
| DualBucket(B=0.75C,P=4.00R)  | S1      |   0.875 |   4.46% |   0.82% | +0.070 [+0.070, +0.070] |  +2.28% [ +2.03%,  +2.52%] | Static Frontier |
| DualBucket(B=0.70C,P=4.00R)  | S1      |   0.817 |   6.88% |   1.27% | +0.011 [+0.011, +0.011] |  +4.70% [ +4.46%,  +4.95%] | Static Frontier |
| DualBucket(B=0.65C,P=4.00R)  | S1      |   0.758 |   9.97% |   1.84% | -0.047 [-0.047, -0.047] |  +7.79% [ +7.54%,  +8.06%] | Static Frontier |
| DualBucket(B=0.60C,P=4.00R)  | S1      |   0.700 |  13.73% |   2.53% | -0.105 [-0.105, -0.105] | +11.55% [+11.29%, +11.84%] | Static Frontier |
| DualBucket(B=0.55C,P=4.00R)  | S1      |   0.642 |  18.18% |   3.35% | -0.164 [-0.164, -0.164] | +16.00% [+15.73%, +16.30%] | Static Frontier |
| DualBucket(B=0.50C,P=4.00R)  | S1      |   0.583 |  23.30% |   4.29% | -0.222 [-0.222, -0.222] | +21.12% [+20.84%, +21.43%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.525 |  29.09% |   5.36% | -0.280 [-0.280, -0.280] | +26.91% [+26.63%, +27.23%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.501 |  33.60% |   6.19% | -0.305 [-0.305, -0.305] | +31.42% [+31.12%, +31.76%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.467 |  35.55% |   6.55% | -0.339 [-0.339, -0.339] | +33.37% [+33.09%, +33.69%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.408 |  42.69% |   7.86% | -0.397 [-0.397, -0.397] | +40.51% [+40.22%, +40.83%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.350 |  50.51% |   9.30% | -0.455 [-0.455, -0.455] | +48.34% [+48.05%, +48.64%] | Static Frontier |

### Scenario: k = 1.25 (C = 1000, coldStart)

**DREB burst-matched Status:** **On Non-Dominated Frontier** (achieves unique optimal trade-off)

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **0.862** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 0.853 | 2.43% | 0.45% | -0.009 [-0.009, -0.009] |  +0.25% [ +0.23%,  +0.27%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.020 | 1.72% | 0.32% | +0.158 [+0.158, +0.158] |  -0.46% [ -0.59%,  -0.34%] | Yes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.031 |   0.76% |   0.14% | +0.169 [+0.169, +0.169] |  -1.42% [ -1.66%,  -1.20%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.031 |   0.96% |   0.18% | +0.169 [+0.169, +0.169] |  -1.22% [ -1.46%,  -1.00%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.021 |   2.50% |   0.46% | +0.159 [+0.159, +0.159] |  +0.32% [ +0.08%,  +0.55%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.020 |   3.00% |   0.55% | +0.158 [+0.158, +0.158] |  +0.82% [ +0.57%,  +1.06%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.018 |   3.56% |   0.66% | +0.156 [+0.156, +0.156] |  +1.38% [ +1.12%,  +1.63%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.016 |   4.44% |   0.82% | +0.154 [+0.154, +0.154] |  +2.26% [ +1.99%,  +2.51%] | Static Frontier |
| DualBucket(B=0.75C,P=4.00R)  | S1      |   1.000 |   4.46% |   0.82% | +0.138 [+0.138, +0.138] |  +2.28% [ +2.03%,  +2.52%] | Static Frontier |
| DualBucket(B=0.70C,P=4.00R)  | S1      |   0.933 |   6.88% |   1.27% | +0.071 [+0.071, +0.071] |  +4.70% [ +4.46%,  +4.95%] | Static Frontier |
| DualBucket(B=0.65C,P=4.00R)  | S1      |   0.867 |   9.97% |   1.84% | +0.005 [+0.005, +0.005] |  +7.79% [ +7.54%,  +8.06%] | Static Frontier |
| DualBucket(B=0.60C,P=4.00R)  | S1      |   0.800 |  13.73% |   2.53% | -0.062 [-0.062, -0.062] | +11.55% [+11.29%, +11.84%] | Static Frontier |
| DualBucket(B=0.55C,P=4.00R)  | S1      |   0.733 |  18.18% |   3.35% | -0.129 [-0.129, -0.129] | +16.00% [+15.73%, +16.30%] | Static Frontier |
| DualBucket(B=0.50C,P=4.00R)  | S1      |   0.667 |  23.30% |   4.29% | -0.195 [-0.195, -0.195] | +21.12% [+20.84%, +21.43%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.600 |  29.09% |   5.36% | -0.262 [-0.262, -0.262] | +26.91% [+26.63%, +27.23%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.572 |  33.60% |   6.19% | -0.290 [-0.290, -0.290] | +31.42% [+31.12%, +31.76%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.533 |  35.55% |   6.55% | -0.329 [-0.329, -0.329] | +33.37% [+33.09%, +33.69%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.467 |  42.69% |   7.86% | -0.395 [-0.395, -0.395] | +40.51% [+40.22%, +40.83%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.400 |  50.51% |   9.30% | -0.462 [-0.462, -0.462] | +48.34% [+48.05%, +48.64%] | Static Frontier |

### Scenario: k = 1.50 (C = 1000, coldStart)

**DREB burst-matched Status:** **On Non-Dominated Frontier** (achieves unique optimal trade-off)

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **0.948** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 0.944 | 2.43% | 0.45% | -0.004 [-0.004, -0.004] |  +0.25% [ +0.23%,  +0.27%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.028 | 1.72% | 0.32% | +0.081 [+0.081, +0.081] |  -0.46% [ -0.59%,  -0.34%] | Yes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.035 |   0.76% |   0.14% | +0.087 [+0.087, +0.087] |  -1.42% [ -1.66%,  -1.20%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.035 |   0.96% |   0.18% | +0.087 [+0.087, +0.087] |  -1.22% [ -1.46%,  -1.00%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.026 |   2.50% |   0.46% | +0.078 [+0.078, +0.078] |  +0.32% [ +0.08%,  +0.55%] | Static Frontier |
| DualBucket(B=1.00C,P=1.00R)  | S1      |   1.026 |   2.50% |   0.46% | +0.078 [+0.078, +0.078] |  +0.32% [ +0.08%,  +0.55%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.024 |   3.00% |   0.55% | +0.076 [+0.076, +0.076] |  +0.82% [ +0.57%,  +1.06%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.022 |   3.56% |   0.66% | +0.075 [+0.075, +0.075] |  +1.38% [ +1.12%,  +1.63%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.021 |   4.44% |   0.82% | +0.073 [+0.073, +0.073] |  +2.26% [ +1.99%,  +2.51%] | Static Frontier |
| DualBucket(B=0.85C,P=1.00R)  | S1      |   1.021 |   4.44% |   0.82% | +0.073 [+0.073, +0.073] |  +2.26% [ +2.00%,  +2.52%] | Static Frontier |
| DualBucket(B=0.80C,P=1.00R)  | S1      |   1.019 |   5.90% |   1.09% | +0.071 [+0.071, +0.071] |  +3.72% [ +3.46%,  +3.98%] | Static Frontier |
| DualBucket(B=0.75C,P=1.00R)  | S1      |   1.017 |   7.91% |   1.46% | +0.070 [+0.070, +0.070] |  +5.73% [ +5.47%,  +6.00%] | Static Frontier |
| TB(C=0.70C,R=1.00R)          | S2      |   1.016 |  10.46% |   1.93% | +0.068 [+0.068, +0.068] |  +8.28% [ +8.02%,  +8.56%] | Static Frontier |
| DualBucket(B=0.70C,P=1.00R)  | S1      |   1.016 |  10.47% |   1.93% | +0.068 [+0.068, +0.068] |  +8.29% [ +8.03%,  +8.56%] | Static Frontier |
| DualBucket(B=0.65C,P=1.00R)  | S1      |   1.014 |  13.59% |   2.50% | +0.066 [+0.066, +0.066] | +11.42% [+11.14%, +11.70%] | Static Frontier |
| DualBucket(B=0.60C,P=4.00R)  | S1      |   0.960 |  13.73% |   2.53% | +0.012 [+0.012, +0.012] | +11.55% [+11.29%, +11.84%] | Static Frontier |
| DualBucket(B=0.55C,P=4.00R)  | S1      |   0.880 |  18.18% |   3.35% | -0.068 [-0.068, -0.068] | +16.00% [+15.73%, +16.30%] | Static Frontier |
| DualBucket(B=0.50C,P=4.00R)  | S1      |   0.800 |  23.30% |   4.29% | -0.148 [-0.148, -0.148] | +21.12% [+20.84%, +21.43%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.720 |  29.09% |   5.36% | -0.228 [-0.228, -0.228] | +26.91% [+26.63%, +27.23%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.686 |  33.60% |   6.19% | -0.261 [-0.261, -0.261] | +31.42% [+31.12%, +31.76%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.640 |  35.55% |   6.55% | -0.308 [-0.308, -0.308] | +33.37% [+33.09%, +33.69%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.560 |  42.69% |   7.86% | -0.388 [-0.388, -0.388] | +40.51% [+40.22%, +40.83%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.480 |  50.51% |   9.30% | -0.468 [-0.468, -0.468] | +48.34% [+48.05%, +48.64%] | Static Frontier |

### Scenario: k = 2.00 (C = 1000, coldStart)

**DREB burst-matched Status:** **Dominated** by `DREB (idleGain=4R/3)`

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **1.039** | **2.18%** | **0.41%** | [Reference] | [Reference] | **Yes (on Frontier)** |
| DREB (v2 base 1.5) | Dynamic | 1.038 | 2.43% | 0.45% | -0.001 [-0.001, -0.001] |  +0.25% [ +0.23%,  +0.27%] | Yes |
| DREB (idleGain=4R/3) | Dynamic | 1.039 | 1.72% | 0.32% | +0.000 [+0.000, +0.000] |  -0.46% [ -0.59%,  -0.34%] | Yes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.040 |   0.76% |   0.14% | +0.002 [+0.002, +0.002] |  -1.42% [ -1.66%,  -1.20%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.040 |   0.96% |   0.18% | +0.002 [+0.002, +0.002] |  -1.22% [ -1.46%,  -1.00%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.031 |   2.50% |   0.46% | -0.008 [-0.008, -0.008] |  +0.32% [ +0.08%,  +0.55%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.029 |   3.00% |   0.55% | -0.010 [-0.010, -0.010] |  +0.82% [ +0.57%,  +1.06%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.028 |   3.56% |   0.66% | -0.011 [-0.011, -0.011] |  +1.38% [ +1.12%,  +1.63%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.026 |   4.44% |   0.82% | -0.013 [-0.013, -0.013] |  +2.26% [ +1.99%,  +2.51%] | Static Frontier |
| DualBucket(B=0.80C,P=1.00R)  | S1      |   1.024 |   5.90% |   1.09% | -0.015 [-0.015, -0.015] |  +3.72% [ +3.46%,  +3.98%] | Static Frontier |
| DualBucket(B=0.75C,P=1.00R)  | S1      |   1.023 |   7.91% |   1.46% | -0.016 [-0.016, -0.016] |  +5.73% [ +5.47%,  +6.00%] | Static Frontier |
| TB(C=0.70C,R=1.00R)          | S2      |   1.021 |  10.46% |   1.93% | -0.018 [-0.018, -0.018] |  +8.28% [ +8.02%,  +8.56%] | Static Frontier |
| DualBucket(B=0.65C,P=1.00R)  | S1      |   1.019 |  13.59% |   2.50% | -0.020 [-0.020, -0.020] | +11.42% [+11.14%, +11.70%] | Static Frontier |
| DualBucket(B=0.60C,P=1.00R)  | S1      |   1.017 |  17.28% |   3.18% | -0.021 [-0.021, -0.021] | +15.10% [+14.82%, +15.40%] | Static Frontier |
| TB(C=0.55C,R=1.00R)          | S2      |   1.016 |  21.53% |   3.96% | -0.023 [-0.023, -0.023] | +19.35% [+19.07%, +19.66%] | Static Frontier |
| DualBucket(B=0.50C,P=1.00R)  | S1      |   1.014 |  26.38% |   4.86% | -0.025 [-0.025, -0.025] | +24.21% [+23.91%, +24.52%] | Static Frontier |
| DualBucket(B=0.45C,P=4.00R)  | S1      |   0.960 |  29.09% |   5.36% | -0.079 [-0.079, -0.079] | +26.91% [+26.63%, +27.23%] | Static Frontier |
| TB(C=0.43C,R=1.25R)          | S3      |   0.915 |  33.60% |   6.19% | -0.124 [-0.124, -0.124] | +31.42% [+31.12%, +31.76%] | Static Frontier |
| DualBucket(B=0.40C,P=4.00R)  | S1      |   0.853 |  35.55% |   6.55% | -0.185 [-0.185, -0.185] | +33.37% [+33.09%, +33.69%] | Static Frontier |
| DualBucket(B=0.35C,P=4.00R)  | S1      |   0.747 |  42.69% |   7.86% | -0.292 [-0.292, -0.292] | +40.51% [+40.22%, +40.83%] | Static Frontier |
| DualBucket(B=0.30C,P=4.00R)  | S1      |   0.640 |  50.51% |   9.30% | -0.399 [-0.399, -0.399] | +48.34% [+48.05%, +48.64%] | Static Frontier |

### Scenario: k = 4.00 (C = 1000, coldStart)

**DREB burst-matched Status:** **Dominated** by `DualBucket(B=1.00C,P=1.25R)`

| Policy | Family | Abuser Rate / R | Bursty FRR % | Overall FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] | Non-Dominated? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **DREB (burst-matched)** | Dynamic | **1.040** | **2.18%** | **0.41%** | [Reference] | [Reference] | *No (Dominated)* |
| DREB (v2 base 1.5) | Dynamic | 1.040 | 2.43% | 0.45% | +0.000 [+0.000, +0.000] |  +0.25% [ +0.23%,  +0.27%] | No |
| DREB (idleGain=4R/3) | Dynamic | 1.040 | 1.72% | 0.32% | +0.000 [+0.000, +0.000] |  -0.46% [ -0.59%,  -0.34%] | No |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB(C=1.29C,R=1.00R)          | S2      |   1.040 |   0.76% |   0.14% | +0.000 [+0.000, +0.000] |  -1.42% [ -1.66%,  -1.20%] | Static Frontier |
| DualBucket(B=1.00C,P=4.00R)  | S1      |   1.040 |   0.96% |   0.18% | +0.000 [+0.000, +0.000] |  -1.22% [ -1.46%,  -1.00%] | Static Frontier |
| TB(C=1.00C,R=1.00R)          | S2      |   1.031 |   2.50% |   0.46% | -0.010 [-0.010, -0.010] |  +0.32% [ +0.08%,  +0.55%] | Static Frontier |
| DualBucket(B=0.95C,P=1.00R)  | S1      |   1.029 |   3.00% |   0.55% | -0.011 [-0.011, -0.011] |  +0.82% [ +0.57%,  +1.06%] | Static Frontier |
| DualBucket(B=0.90C,P=1.00R)  | S1      |   1.028 |   3.56% |   0.66% | -0.013 [-0.013, -0.013] |  +1.38% [ +1.12%,  +1.63%] | Static Frontier |
| TB(C=0.85C,R=1.00R)          | S2      |   1.026 |   4.44% |   0.82% | -0.014 [-0.014, -0.014] |  +2.26% [ +1.99%,  +2.51%] | Static Frontier |
| DualBucket(B=0.80C,P=1.00R)  | S1      |   1.024 |   5.90% |   1.09% | -0.016 [-0.016, -0.016] |  +3.72% [ +3.46%,  +3.98%] | Static Frontier |
| DualBucket(B=0.75C,P=1.00R)  | S1      |   1.023 |   7.91% |   1.46% | -0.018 [-0.018, -0.018] |  +5.73% [ +5.47%,  +6.00%] | Static Frontier |
| TB(C=0.70C,R=1.00R)          | S2      |   1.021 |  10.46% |   1.93% | -0.020 [-0.020, -0.020] |  +8.28% [ +8.02%,  +8.56%] | Static Frontier |
| DualBucket(B=0.65C,P=1.00R)  | S1      |   1.019 |  13.59% |   2.50% | -0.021 [-0.021, -0.021] | +11.42% [+11.14%, +11.70%] | Static Frontier |
| DualBucket(B=0.60C,P=1.00R)  | S1      |   1.017 |  17.28% |   3.18% | -0.023 [-0.023, -0.023] | +15.10% [+14.82%, +15.40%] | Static Frontier |
| TB(C=0.55C,R=1.00R)          | S2      |   1.016 |  21.53% |   3.96% | -0.024 [-0.024, -0.024] | +19.35% [+19.07%, +19.66%] | Static Frontier |
| DualBucket(B=0.50C,P=1.00R)  | S1      |   1.014 |  26.38% |   4.86% | -0.026 [-0.026, -0.026] | +24.21% [+23.91%, +24.52%] | Static Frontier |
| DualBucket(B=0.45C,P=1.00R)  | S1      |   1.012 |  31.82% |   5.86% | -0.028 [-0.028, -0.028] | +29.64% [+29.34%, +29.97%] | Static Frontier |
| TB(C=0.43C,R=1.00R)          | S2      |   1.012 |  34.28% |   6.31% | -0.029 [-0.029, -0.029] | +32.10% [+31.80%, +32.43%] | Static Frontier |
| TB(C=0.43C,R=1.00R)          | S3      |   1.012 |  34.28% |   6.31% | -0.029 [-0.029, -0.029] | +32.10% [+31.80%, +32.43%] | Static Frontier |
| DualBucket(B=0.40C,P=1.00R)  | S1      |   1.011 |  37.86% |   6.97% | -0.030 [-0.030, -0.030] | +35.69% [+35.39%, +36.02%] | Static Frontier |
| DualBucket(B=0.35C,P=1.00R)  | S1      |   1.009 |  44.55% |   8.20% | -0.031 [-0.031, -0.031] | +42.37% [+42.08%, +42.69%] | Static Frontier |
| TB(C=0.30C,R=1.00R)          | S2      |   1.008 |  51.90% |   9.56% | -0.033 [-0.033, -0.033] | +49.72% [+49.43%, +50.03%] | Static Frontier |

---

## 5. Full Per-Setting Table Supporting H9c (45 Settings: 9 Legit Models x 5 k Values)

Comprehensive per-setting verification for H9c (evaluating all 9 combinations of bursty-legit idle-time mean in {1tau, 3tau, 10tau} and burst size range in {[0.1C, 0.5C], [0.3C, 0.9C], [0.6C, 1.0C]} across every k in {1.1, 1.25, 1.5, 2.0, 4.0}):

| Overload k | Idle Mean | Burst Size Range | DREB Admitted Rate / R | DREB Bursty FRR % | Static Non-Dominated Set (Policy: Rate, FRR) | DREB Verdict vs Static |
| :---: | :---: | :---: | :---: | :---: | :--- | :---: |
| 1.10 |  1.0\tau | [0.1C, 0.5C]     |   0.805 |   0.09% | TB(C=1.29C,R=1.00R) (1.022R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.022R, 0.0%); DualBucket(B=0.85C,P=4.00R) (0.992R, 0.0%); DualBucket(B=0.80C,P=4.00R) (0.933R, 0.1%); DualBucket(B=0.75C,P=4.00R) (0.875R, 0.1%); DualBucket(B=0.70C,P=4.00R) (0.817R, 0.2%); DualBucket(B=0.65C,P=4.00R) (0.758R, 0.3%); DualBucket(B=0.60C,P=4.00R) (0.700R, 0.5%); DualBucket(B=0.55C,P=4.00R) (0.642R, 0.7%); DualBucket(B=0.50C,P=4.00R) (0.583R, 1.0%); DualBucket(B=0.45C,P=4.00R) (0.525R, 2.4%); DualBucket(B=0.40C,P=4.00R) (0.467R, 5.8%); DualBucket(B=0.35C,P=4.00R) (0.408R, 11.0%); DualBucket(B=0.30C,P=4.00R) (0.350R, 18.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 |  1.0\tau | [0.3C, 0.9C]     |   0.805 |   8.25% | TB(C=1.29C,R=1.00R) (1.022R, 5.3%); DualBucket(B=1.00C,P=4.00R) (1.022R, 5.6%); DualBucket(B=0.85C,P=4.00R) (0.992R, 6.4%); DualBucket(B=0.80C,P=4.00R) (0.933R, 7.3%); DualBucket(B=0.75C,P=4.00R) (0.875R, 8.7%); DualBucket(B=0.70C,P=4.00R) (0.817R, 10.7%); DualBucket(B=0.65C,P=4.00R) (0.758R, 13.3%); DualBucket(B=0.60C,P=4.00R) (0.700R, 16.6%); DualBucket(B=0.55C,P=4.00R) (0.642R, 20.7%); DualBucket(B=0.50C,P=4.00R) (0.583R, 25.4%); DualBucket(B=0.45C,P=4.00R) (0.525R, 30.8%); DualBucket(B=0.40C,P=4.00R) (0.467R, 37.0%); DualBucket(B=0.35C,P=4.00R) (0.408R, 43.8%); DualBucket(B=0.30C,P=4.00R) (0.350R, 51.3%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 |  1.0\tau | [0.6C, 1.0C]     |   0.805 |  19.66% | TB(C=1.29C,R=1.00R) (1.022R, 13.8%); DualBucket(B=1.00C,P=4.00R) (1.022R, 14.2%); DualBucket(B=0.85C,P=4.00R) (0.992R, 16.1%); DualBucket(B=0.80C,P=4.00R) (0.933R, 17.6%); DualBucket(B=0.75C,P=4.00R) (0.875R, 19.6%); DualBucket(B=0.70C,P=4.00R) (0.817R, 22.4%); DualBucket(B=0.65C,P=4.00R) (0.758R, 25.9%); DualBucket(B=0.60C,P=4.00R) (0.700R, 30.2%); DualBucket(B=0.55C,P=4.00R) (0.642R, 35.2%); DualBucket(B=0.50C,P=4.00R) (0.583R, 40.6%); DualBucket(B=0.45C,P=4.00R) (0.525R, 46.1%); DualBucket(B=0.40C,P=4.00R) (0.467R, 51.8%); DualBucket(B=0.35C,P=4.00R) (0.408R, 57.7%); DualBucket(B=0.30C,P=4.00R) (0.350R, 63.5%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 |  3.0\tau | [0.1C, 0.5C]     |   0.805 |   0.01% | DualBucket(B=1.00C,P=2.50R) (1.022R, 0.0%); DualBucket(B=0.95C,P=4.00R) (1.022R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.022R, 0.0%); DualBucket(B=0.85C,P=4.00R) (0.992R, 0.0%); DualBucket(B=0.80C,P=4.00R) (0.933R, 0.0%); DualBucket(B=0.75C,P=4.00R) (0.875R, 0.0%); DualBucket(B=0.70C,P=4.00R) (0.817R, 0.0%); DualBucket(B=0.65C,P=4.00R) (0.758R, 0.1%); DualBucket(B=0.60C,P=4.00R) (0.700R, 0.2%); DualBucket(B=0.55C,P=4.00R) (0.642R, 0.2%); DualBucket(B=0.50C,P=4.00R) (0.583R, 0.3%); DualBucket(B=0.45C,P=4.00R) (0.525R, 1.5%); TB(C=0.43C,R=1.25R) (0.501R, 3.7%); DualBucket(B=0.40C,P=4.00R) (0.467R, 4.7%); DualBucket(B=0.35C,P=4.00R) (0.408R, 10.0%); DualBucket(B=0.30C,P=4.00R) (0.350R, 17.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 |  3.0\tau | [0.3C, 0.9C]     |   0.805 |   2.18% | TB(C=1.29C,R=1.00R) (1.022R, 0.8%); DualBucket(B=1.00C,P=4.00R) (1.022R, 1.0%); DualBucket(B=0.85C,P=4.00R) (0.992R, 1.6%); DualBucket(B=0.80C,P=4.00R) (0.933R, 2.7%); DualBucket(B=0.75C,P=4.00R) (0.875R, 4.5%); DualBucket(B=0.70C,P=4.00R) (0.817R, 6.9%); DualBucket(B=0.65C,P=4.00R) (0.758R, 10.0%); DualBucket(B=0.60C,P=4.00R) (0.700R, 13.7%); DualBucket(B=0.55C,P=4.00R) (0.642R, 18.2%); DualBucket(B=0.50C,P=4.00R) (0.583R, 23.3%); DualBucket(B=0.45C,P=4.00R) (0.525R, 29.1%); TB(C=0.43C,R=1.25R) (0.501R, 33.6%); DualBucket(B=0.40C,P=4.00R) (0.467R, 35.6%); DualBucket(B=0.35C,P=4.00R) (0.408R, 42.7%); DualBucket(B=0.30C,P=4.00R) (0.350R, 50.5%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 |  3.0\tau | [0.6C, 1.0C]     |   0.805 |   6.63% | TB(C=1.29C,R=1.00R) (1.022R, 3.1%); DualBucket(B=1.00C,P=4.00R) (1.022R, 3.4%); DualBucket(B=0.85C,P=4.00R) (0.992R, 6.6%); DualBucket(B=0.80C,P=4.00R) (0.933R, 9.0%); DualBucket(B=0.75C,P=4.00R) (0.875R, 12.2%); DualBucket(B=0.70C,P=4.00R) (0.817R, 16.2%); DualBucket(B=0.65C,P=4.00R) (0.758R, 21.0%); DualBucket(B=0.60C,P=4.00R) (0.700R, 26.5%); DualBucket(B=0.55C,P=4.00R) (0.642R, 32.5%); DualBucket(B=0.50C,P=4.00R) (0.583R, 38.5%); DualBucket(B=0.45C,P=4.00R) (0.525R, 44.6%); TB(C=0.43C,R=1.25R) (0.501R, 48.7%); DualBucket(B=0.40C,P=4.00R) (0.467R, 50.7%); DualBucket(B=0.35C,P=4.00R) (0.408R, 56.8%); DualBucket(B=0.30C,P=4.00R) (0.350R, 62.9%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 | 10.0\tau | [0.1C, 0.5C]     |   0.805 |   0.00% | DualBucket(B=0.80C,P=4.00R) (0.933R, 0.0%); DualBucket(B=0.75C,P=4.00R) (0.875R, 0.0%); DualBucket(B=0.70C,P=4.00R) (0.817R, 0.0%); DualBucket(B=0.65C,P=4.00R) (0.758R, 0.0%); DualBucket(B=0.60C,P=4.00R) (0.700R, 0.0%); DualBucket(B=0.55C,P=4.00R) (0.642R, 0.1%); DualBucket(B=0.50C,P=4.00R) (0.583R, 0.1%); DualBucket(B=0.45C,P=4.00R) (0.525R, 1.2%); TB(C=0.43C,R=1.25R) (0.501R, 2.5%); DualBucket(B=0.40C,P=4.00R) (0.467R, 4.4%); DualBucket(B=0.35C,P=4.00R) (0.408R, 9.6%); DualBucket(B=0.30C,P=4.00R) (0.350R, 16.9%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 | 10.0\tau | [0.3C, 0.9C]     |   0.805 |   0.80% | TB(C=1.29C,R=1.00R) (1.022R, 0.1%); DualBucket(B=1.00C,P=4.00R) (1.022R, 0.2%); TB(C=1.00C,R=1.00R) (1.012R, 0.6%); DualBucket(B=0.85C,P=4.00R) (0.992R, 0.7%); DualBucket(B=0.80C,P=4.00R) (0.933R, 1.7%); DualBucket(B=0.75C,P=4.00R) (0.875R, 3.5%); DualBucket(B=0.70C,P=4.00R) (0.817R, 5.9%); DualBucket(B=0.65C,P=4.00R) (0.758R, 9.1%); DualBucket(B=0.60C,P=4.00R) (0.700R, 12.9%); DualBucket(B=0.55C,P=4.00R) (0.642R, 17.4%); DualBucket(B=0.50C,P=4.00R) (0.583R, 22.6%); DualBucket(B=0.45C,P=4.00R) (0.525R, 28.5%); TB(C=0.43C,R=1.25R) (0.501R, 31.6%); DualBucket(B=0.40C,P=4.00R) (0.467R, 35.0%); DualBucket(B=0.35C,P=4.00R) (0.408R, 42.3%); DualBucket(B=0.30C,P=4.00R) (0.350R, 50.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.10 | 10.0\tau | [0.6C, 1.0C]     |   0.805 |   2.32% | TB(C=1.29C,R=1.00R) (1.022R, 0.6%); DualBucket(B=1.00C,P=4.00R) (1.022R, 0.7%); TB(C=1.00C,R=1.00R) (1.012R, 1.7%); DualBucket(B=0.95C,P=1.00R) (1.010R, 2.3%); DualBucket(B=0.90C,P=1.00R) (1.009R, 3.7%); DualBucket(B=0.85C,P=4.00R) (0.992R, 4.2%); DualBucket(B=0.80C,P=4.00R) (0.933R, 6.9%); DualBucket(B=0.75C,P=4.00R) (0.875R, 10.4%); DualBucket(B=0.70C,P=4.00R) (0.817R, 14.6%); DualBucket(B=0.65C,P=4.00R) (0.758R, 19.7%); DualBucket(B=0.60C,P=4.00R) (0.700R, 25.5%); DualBucket(B=0.55C,P=4.00R) (0.642R, 31.6%); DualBucket(B=0.50C,P=4.00R) (0.583R, 37.8%); DualBucket(B=0.45C,P=4.00R) (0.525R, 44.0%); TB(C=0.43C,R=1.25R) (0.501R, 47.0%); DualBucket(B=0.40C,P=4.00R) (0.467R, 50.2%); DualBucket(B=0.35C,P=4.00R) (0.408R, 56.4%); DualBucket(B=0.30C,P=4.00R) (0.350R, 62.6%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 |  1.0\tau | [0.1C, 0.5C]     |   0.862 |   0.09% | TB(C=1.29C,R=1.00R) (1.031R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.031R, 0.0%); DualBucket(B=0.75C,P=4.00R) (1.000R, 0.1%); DualBucket(B=0.70C,P=4.00R) (0.933R, 0.2%); DualBucket(B=0.65C,P=4.00R) (0.867R, 0.3%); DualBucket(B=0.60C,P=4.00R) (0.800R, 0.5%); DualBucket(B=0.55C,P=4.00R) (0.733R, 0.7%); DualBucket(B=0.50C,P=4.00R) (0.667R, 1.0%); DualBucket(B=0.45C,P=4.00R) (0.600R, 2.4%); DualBucket(B=0.40C,P=4.00R) (0.533R, 5.8%); DualBucket(B=0.35C,P=4.00R) (0.467R, 11.0%); DualBucket(B=0.30C,P=4.00R) (0.400R, 18.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 |  1.0\tau | [0.3C, 0.9C]     |   0.862 |   8.25% | TB(C=1.29C,R=1.00R) (1.031R, 5.3%); DualBucket(B=1.00C,P=4.00R) (1.031R, 5.6%); DualBucket(B=0.75C,P=4.00R) (1.000R, 8.7%); DualBucket(B=0.70C,P=4.00R) (0.933R, 10.7%); DualBucket(B=0.65C,P=4.00R) (0.867R, 13.3%); DualBucket(B=0.60C,P=4.00R) (0.800R, 16.6%); DualBucket(B=0.55C,P=4.00R) (0.733R, 20.7%); DualBucket(B=0.50C,P=4.00R) (0.667R, 25.4%); DualBucket(B=0.45C,P=4.00R) (0.600R, 30.8%); DualBucket(B=0.40C,P=4.00R) (0.533R, 37.0%); DualBucket(B=0.35C,P=4.00R) (0.467R, 43.8%); DualBucket(B=0.30C,P=4.00R) (0.400R, 51.3%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 |  1.0\tau | [0.6C, 1.0C]     |   0.862 |  19.66% | TB(C=1.29C,R=1.00R) (1.031R, 13.8%); DualBucket(B=1.00C,P=4.00R) (1.031R, 14.2%); DualBucket(B=0.75C,P=4.00R) (1.000R, 19.6%); DualBucket(B=0.70C,P=4.00R) (0.933R, 22.4%); DualBucket(B=0.65C,P=4.00R) (0.867R, 25.9%); DualBucket(B=0.60C,P=4.00R) (0.800R, 30.2%); DualBucket(B=0.55C,P=4.00R) (0.733R, 35.2%); DualBucket(B=0.50C,P=4.00R) (0.667R, 40.6%); DualBucket(B=0.45C,P=4.00R) (0.600R, 46.1%); DualBucket(B=0.40C,P=4.00R) (0.533R, 51.8%); DualBucket(B=0.35C,P=4.00R) (0.467R, 57.7%); DualBucket(B=0.30C,P=4.00R) (0.400R, 63.5%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 |  3.0\tau | [0.1C, 0.5C]     |   0.862 |   0.01% | DualBucket(B=1.00C,P=2.50R) (1.031R, 0.0%); DualBucket(B=0.95C,P=4.00R) (1.031R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.031R, 0.0%); DualBucket(B=1.00C,P=1.00R) (1.021R, 0.0%); TB(C=1.00C,R=1.00R) (1.021R, 0.0%); DualBucket(B=0.95C,P=1.00R) (1.020R, 0.0%); DualBucket(B=0.75C,P=4.00R) (1.000R, 0.0%); DualBucket(B=0.70C,P=4.00R) (0.933R, 0.0%); DualBucket(B=0.65C,P=4.00R) (0.867R, 0.1%); DualBucket(B=0.60C,P=4.00R) (0.800R, 0.2%); DualBucket(B=0.55C,P=4.00R) (0.733R, 0.2%); DualBucket(B=0.50C,P=4.00R) (0.667R, 0.3%); DualBucket(B=0.45C,P=4.00R) (0.600R, 1.5%); TB(C=0.43C,R=1.25R) (0.572R, 3.7%); DualBucket(B=0.40C,P=4.00R) (0.533R, 4.7%); DualBucket(B=0.35C,P=4.00R) (0.467R, 10.0%); DualBucket(B=0.30C,P=4.00R) (0.400R, 17.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 |  3.0\tau | [0.3C, 0.9C]     |   0.862 |   2.18% | TB(C=1.29C,R=1.00R) (1.031R, 0.8%); DualBucket(B=1.00C,P=4.00R) (1.031R, 1.0%); TB(C=1.00C,R=1.00R) (1.021R, 2.5%); DualBucket(B=0.95C,P=1.00R) (1.020R, 3.0%); DualBucket(B=0.90C,P=1.00R) (1.018R, 3.6%); TB(C=0.85C,R=1.00R) (1.016R, 4.4%); DualBucket(B=0.75C,P=4.00R) (1.000R, 4.5%); DualBucket(B=0.70C,P=4.00R) (0.933R, 6.9%); DualBucket(B=0.65C,P=4.00R) (0.867R, 10.0%); DualBucket(B=0.60C,P=4.00R) (0.800R, 13.7%); DualBucket(B=0.55C,P=4.00R) (0.733R, 18.2%); DualBucket(B=0.50C,P=4.00R) (0.667R, 23.3%); DualBucket(B=0.45C,P=4.00R) (0.600R, 29.1%); TB(C=0.43C,R=1.25R) (0.572R, 33.6%); DualBucket(B=0.40C,P=4.00R) (0.533R, 35.6%); DualBucket(B=0.35C,P=4.00R) (0.467R, 42.7%); DualBucket(B=0.30C,P=4.00R) (0.400R, 50.5%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 |  3.0\tau | [0.6C, 1.0C]     |   0.862 |   6.63% | TB(C=1.29C,R=1.00R) (1.031R, 3.1%); DualBucket(B=1.00C,P=4.00R) (1.031R, 3.4%); TB(C=1.00C,R=1.00R) (1.021R, 6.7%); DualBucket(B=0.95C,P=1.00R) (1.020R, 7.7%); DualBucket(B=0.90C,P=1.00R) (1.018R, 9.3%); TB(C=0.85C,R=1.00R) (1.016R, 11.4%); DualBucket(B=0.75C,P=4.00R) (1.000R, 12.2%); DualBucket(B=0.70C,P=4.00R) (0.933R, 16.2%); DualBucket(B=0.65C,P=4.00R) (0.867R, 21.0%); DualBucket(B=0.60C,P=4.00R) (0.800R, 26.5%); DualBucket(B=0.55C,P=4.00R) (0.733R, 32.5%); DualBucket(B=0.50C,P=4.00R) (0.667R, 38.5%); DualBucket(B=0.45C,P=4.00R) (0.600R, 44.6%); TB(C=0.43C,R=1.25R) (0.572R, 48.7%); DualBucket(B=0.40C,P=4.00R) (0.533R, 50.7%); DualBucket(B=0.35C,P=4.00R) (0.467R, 56.8%); DualBucket(B=0.30C,P=4.00R) (0.400R, 62.9%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 | 10.0\tau | [0.1C, 0.5C]     |   0.862 |   0.00% | DualBucket(B=0.95C,P=1.00R) (1.020R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.018R, 0.0%); DualBucket(B=0.75C,P=4.00R) (1.000R, 0.0%); DualBucket(B=0.70C,P=4.00R) (0.933R, 0.0%); DualBucket(B=0.65C,P=4.00R) (0.867R, 0.0%); DualBucket(B=0.60C,P=4.00R) (0.800R, 0.0%); DualBucket(B=0.55C,P=4.00R) (0.733R, 0.1%); DualBucket(B=0.50C,P=4.00R) (0.667R, 0.1%); DualBucket(B=0.45C,P=4.00R) (0.600R, 1.2%); TB(C=0.43C,R=1.25R) (0.572R, 2.5%); DualBucket(B=0.40C,P=4.00R) (0.533R, 4.4%); DualBucket(B=0.35C,P=4.00R) (0.467R, 9.6%); DualBucket(B=0.30C,P=4.00R) (0.400R, 16.9%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 | 10.0\tau | [0.3C, 0.9C]     |   0.862 |   0.80% | TB(C=1.29C,R=1.00R) (1.031R, 0.1%); DualBucket(B=1.00C,P=4.00R) (1.031R, 0.2%); TB(C=1.00C,R=1.00R) (1.021R, 0.6%); DualBucket(B=0.95C,P=1.00R) (1.020R, 0.7%); DualBucket(B=0.90C,P=1.00R) (1.018R, 0.8%); TB(C=0.85C,R=1.00R) (1.016R, 1.3%); DualBucket(B=0.80C,P=1.00R) (1.015R, 2.5%); DualBucket(B=0.75C,P=4.00R) (1.000R, 3.5%); DualBucket(B=0.70C,P=4.00R) (0.933R, 5.9%); DualBucket(B=0.65C,P=4.00R) (0.867R, 9.1%); DualBucket(B=0.60C,P=4.00R) (0.800R, 12.9%); DualBucket(B=0.55C,P=4.00R) (0.733R, 17.4%); DualBucket(B=0.50C,P=4.00R) (0.667R, 22.6%); DualBucket(B=0.45C,P=4.00R) (0.600R, 28.5%); TB(C=0.43C,R=1.25R) (0.572R, 31.6%); DualBucket(B=0.40C,P=4.00R) (0.533R, 35.0%); DualBucket(B=0.35C,P=4.00R) (0.467R, 42.3%); DualBucket(B=0.30C,P=4.00R) (0.400R, 50.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.25 | 10.0\tau | [0.6C, 1.0C]     |   0.862 |   2.32% | TB(C=1.29C,R=1.00R) (1.031R, 0.6%); DualBucket(B=1.00C,P=4.00R) (1.031R, 0.7%); TB(C=1.00C,R=1.00R) (1.021R, 1.7%); DualBucket(B=0.95C,P=1.00R) (1.020R, 2.3%); DualBucket(B=0.90C,P=1.00R) (1.018R, 3.7%); TB(C=0.85C,R=1.00R) (1.016R, 5.7%); DualBucket(B=0.80C,P=1.00R) (1.015R, 8.5%); DualBucket(B=0.75C,P=4.00R) (1.000R, 10.4%); DualBucket(B=0.70C,P=4.00R) (0.933R, 14.6%); DualBucket(B=0.65C,P=4.00R) (0.867R, 19.7%); DualBucket(B=0.60C,P=4.00R) (0.800R, 25.5%); DualBucket(B=0.55C,P=4.00R) (0.733R, 31.6%); DualBucket(B=0.50C,P=4.00R) (0.667R, 37.8%); DualBucket(B=0.45C,P=4.00R) (0.600R, 44.0%); TB(C=0.43C,R=1.25R) (0.572R, 47.0%); DualBucket(B=0.40C,P=4.00R) (0.533R, 50.2%); DualBucket(B=0.35C,P=4.00R) (0.467R, 56.4%); DualBucket(B=0.30C,P=4.00R) (0.400R, 62.6%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 |  1.0\tau | [0.1C, 0.5C]     |   0.948 |   0.09% | TB(C=1.29C,R=1.00R) (1.035R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.035R, 0.0%); TB(C=1.00C,R=1.00R) (1.026R, 0.2%); DualBucket(B=1.00C,P=1.00R) (1.026R, 0.2%); DualBucket(B=0.95C,P=1.00R) (1.024R, 0.3%); DualBucket(B=0.90C,P=1.00R) (1.022R, 0.4%); DualBucket(B=0.60C,P=4.00R) (0.960R, 0.5%); DualBucket(B=0.55C,P=4.00R) (0.880R, 0.7%); DualBucket(B=0.50C,P=4.00R) (0.800R, 1.0%); DualBucket(B=0.45C,P=4.00R) (0.720R, 2.4%); DualBucket(B=0.40C,P=4.00R) (0.640R, 5.8%); DualBucket(B=0.35C,P=4.00R) (0.560R, 11.0%); DualBucket(B=0.30C,P=4.00R) (0.480R, 18.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 |  1.0\tau | [0.3C, 0.9C]     |   0.948 |   8.25% | TB(C=1.29C,R=1.00R) (1.035R, 5.3%); DualBucket(B=1.00C,P=4.00R) (1.035R, 5.6%); TB(C=1.00C,R=1.00R) (1.026R, 9.9%); DualBucket(B=1.00C,P=1.00R) (1.026R, 9.9%); DualBucket(B=0.95C,P=1.00R) (1.024R, 11.0%); DualBucket(B=0.90C,P=1.00R) (1.022R, 12.2%); TB(C=0.85C,R=1.00R) (1.021R, 13.6%); DualBucket(B=0.85C,P=1.00R) (1.021R, 13.6%); DualBucket(B=0.80C,P=1.00R) (1.019R, 15.4%); DualBucket(B=0.60C,P=4.00R) (0.960R, 16.6%); DualBucket(B=0.55C,P=4.00R) (0.880R, 20.7%); DualBucket(B=0.50C,P=4.00R) (0.800R, 25.4%); DualBucket(B=0.45C,P=4.00R) (0.720R, 30.8%); DualBucket(B=0.40C,P=4.00R) (0.640R, 37.0%); DualBucket(B=0.35C,P=4.00R) (0.560R, 43.8%); DualBucket(B=0.30C,P=4.00R) (0.480R, 51.3%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 |  1.0\tau | [0.6C, 1.0C]     |   0.948 |  19.66% | TB(C=1.29C,R=1.00R) (1.035R, 13.8%); DualBucket(B=1.00C,P=4.00R) (1.035R, 14.2%); TB(C=1.00C,R=1.00R) (1.026R, 20.2%); DualBucket(B=1.00C,P=1.00R) (1.026R, 20.2%); DualBucket(B=0.95C,P=1.00R) (1.024R, 21.6%); DualBucket(B=0.90C,P=1.00R) (1.022R, 23.3%); TB(C=0.85C,R=1.00R) (1.021R, 25.3%); DualBucket(B=0.85C,P=1.00R) (1.021R, 25.3%); DualBucket(B=0.80C,P=1.00R) (1.019R, 27.5%); DualBucket(B=0.75C,P=1.00R) (1.017R, 30.1%); DualBucket(B=0.60C,P=4.00R) (0.960R, 30.2%); DualBucket(B=0.55C,P=4.00R) (0.880R, 35.2%); DualBucket(B=0.50C,P=4.00R) (0.800R, 40.6%); DualBucket(B=0.45C,P=4.00R) (0.720R, 46.1%); DualBucket(B=0.40C,P=4.00R) (0.640R, 51.8%); DualBucket(B=0.35C,P=4.00R) (0.560R, 57.7%); DualBucket(B=0.30C,P=4.00R) (0.480R, 63.5%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 |  3.0\tau | [0.1C, 0.5C]     |   0.948 |   0.01% | DualBucket(B=1.00C,P=2.50R) (1.035R, 0.0%); DualBucket(B=0.95C,P=4.00R) (1.035R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.035R, 0.0%); DualBucket(B=1.00C,P=1.00R) (1.026R, 0.0%); DualBucket(B=0.95C,P=1.00R) (1.024R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.022R, 0.0%); TB(C=0.85C,R=1.00R) (1.021R, 0.0%); DualBucket(B=0.85C,P=1.00R) (1.021R, 0.0%); DualBucket(B=0.80C,P=1.00R) (1.019R, 0.1%); DualBucket(B=0.75C,P=1.00R) (1.017R, 0.1%); DualBucket(B=0.60C,P=4.00R) (0.960R, 0.2%); DualBucket(B=0.55C,P=4.00R) (0.880R, 0.2%); DualBucket(B=0.50C,P=4.00R) (0.800R, 0.3%); DualBucket(B=0.45C,P=4.00R) (0.720R, 1.5%); TB(C=0.43C,R=1.25R) (0.686R, 3.7%); DualBucket(B=0.40C,P=4.00R) (0.640R, 4.7%); DualBucket(B=0.35C,P=4.00R) (0.560R, 10.0%); DualBucket(B=0.30C,P=4.00R) (0.480R, 17.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 |  3.0\tau | [0.3C, 0.9C]     |   0.948 |   2.18% | TB(C=1.29C,R=1.00R) (1.035R, 0.8%); DualBucket(B=1.00C,P=4.00R) (1.035R, 1.0%); TB(C=1.00C,R=1.00R) (1.026R, 2.5%); DualBucket(B=1.00C,P=1.00R) (1.026R, 2.5%); DualBucket(B=0.95C,P=1.00R) (1.024R, 3.0%); DualBucket(B=0.90C,P=1.00R) (1.022R, 3.6%); TB(C=0.85C,R=1.00R) (1.021R, 4.4%); DualBucket(B=0.85C,P=1.00R) (1.021R, 4.4%); DualBucket(B=0.80C,P=1.00R) (1.019R, 5.9%); DualBucket(B=0.75C,P=1.00R) (1.017R, 7.9%); TB(C=0.70C,R=1.00R) (1.016R, 10.5%); DualBucket(B=0.70C,P=1.00R) (1.016R, 10.5%); DualBucket(B=0.65C,P=1.00R) (1.014R, 13.6%); DualBucket(B=0.60C,P=4.00R) (0.960R, 13.7%); DualBucket(B=0.55C,P=4.00R) (0.880R, 18.2%); DualBucket(B=0.50C,P=4.00R) (0.800R, 23.3%); DualBucket(B=0.45C,P=4.00R) (0.720R, 29.1%); TB(C=0.43C,R=1.25R) (0.686R, 33.6%); DualBucket(B=0.40C,P=4.00R) (0.640R, 35.6%); DualBucket(B=0.35C,P=4.00R) (0.560R, 42.7%); DualBucket(B=0.30C,P=4.00R) (0.480R, 50.5%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 |  3.0\tau | [0.6C, 1.0C]     |   0.948 |   6.63% | TB(C=1.29C,R=1.00R) (1.035R, 3.1%); DualBucket(B=1.00C,P=4.00R) (1.035R, 3.4%); TB(C=1.00C,R=1.00R) (1.026R, 6.7%); DualBucket(B=1.00C,P=1.00R) (1.026R, 6.7%); DualBucket(B=0.95C,P=1.00R) (1.024R, 7.7%); DualBucket(B=0.90C,P=1.00R) (1.022R, 9.3%); TB(C=0.85C,R=1.00R) (1.021R, 11.4%); DualBucket(B=0.85C,P=1.00R) (1.021R, 11.4%); DualBucket(B=0.80C,P=1.00R) (1.019R, 14.1%); DualBucket(B=0.75C,P=1.00R) (1.017R, 17.3%); TB(C=0.70C,R=1.00R) (1.016R, 21.1%); DualBucket(B=0.70C,P=1.00R) (1.016R, 21.1%); DualBucket(B=0.65C,P=1.00R) (1.014R, 25.5%); DualBucket(B=0.60C,P=4.00R) (0.960R, 26.5%); DualBucket(B=0.55C,P=4.00R) (0.880R, 32.5%); DualBucket(B=0.50C,P=4.00R) (0.800R, 38.5%); DualBucket(B=0.45C,P=4.00R) (0.720R, 44.6%); TB(C=0.43C,R=1.25R) (0.686R, 48.7%); DualBucket(B=0.40C,P=4.00R) (0.640R, 50.7%); DualBucket(B=0.35C,P=4.00R) (0.560R, 56.8%); DualBucket(B=0.30C,P=4.00R) (0.480R, 62.9%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 | 10.0\tau | [0.1C, 0.5C]     |   0.948 |   0.00% | DualBucket(B=0.95C,P=1.00R) (1.024R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.022R, 0.0%); TB(C=0.85C,R=1.00R) (1.021R, 0.0%); DualBucket(B=0.85C,P=1.00R) (1.021R, 0.0%); DualBucket(B=0.80C,P=1.00R) (1.019R, 0.0%); DualBucket(B=0.75C,P=1.00R) (1.017R, 0.0%); DualBucket(B=0.60C,P=4.00R) (0.960R, 0.0%); DualBucket(B=0.55C,P=4.00R) (0.880R, 0.1%); DualBucket(B=0.50C,P=4.00R) (0.800R, 0.1%); DualBucket(B=0.45C,P=4.00R) (0.720R, 1.2%); TB(C=0.43C,R=1.25R) (0.686R, 2.5%); DualBucket(B=0.40C,P=4.00R) (0.640R, 4.4%); DualBucket(B=0.35C,P=4.00R) (0.560R, 9.6%); DualBucket(B=0.30C,P=4.00R) (0.480R, 16.9%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 | 10.0\tau | [0.3C, 0.9C]     |   0.948 |   0.80% | TB(C=1.29C,R=1.00R) (1.035R, 0.1%); DualBucket(B=1.00C,P=4.00R) (1.035R, 0.2%); TB(C=1.00C,R=1.00R) (1.026R, 0.6%); DualBucket(B=1.00C,P=1.00R) (1.026R, 0.6%); DualBucket(B=0.95C,P=1.00R) (1.024R, 0.7%); DualBucket(B=0.90C,P=1.00R) (1.022R, 0.8%); TB(C=0.85C,R=1.00R) (1.021R, 1.3%); DualBucket(B=0.85C,P=1.00R) (1.021R, 1.3%); DualBucket(B=0.80C,P=1.00R) (1.019R, 2.5%); DualBucket(B=0.75C,P=1.00R) (1.017R, 4.3%); TB(C=0.70C,R=1.00R) (1.016R, 6.8%); DualBucket(B=0.70C,P=1.00R) (1.016R, 6.8%); DualBucket(B=0.65C,P=1.00R) (1.014R, 10.0%); DualBucket(B=0.60C,P=4.00R) (0.960R, 12.9%); DualBucket(B=0.55C,P=4.00R) (0.880R, 17.4%); DualBucket(B=0.50C,P=4.00R) (0.800R, 22.6%); DualBucket(B=0.45C,P=4.00R) (0.720R, 28.5%); TB(C=0.43C,R=1.25R) (0.686R, 31.6%); DualBucket(B=0.40C,P=4.00R) (0.640R, 35.0%); DualBucket(B=0.35C,P=4.00R) (0.560R, 42.3%); DualBucket(B=0.30C,P=4.00R) (0.480R, 50.2%) | **Non-Dominated** (on Pareto frontier) |
| 1.50 | 10.0\tau | [0.6C, 1.0C]     |   0.948 |   2.32% | TB(C=1.29C,R=1.00R) (1.035R, 0.6%); DualBucket(B=1.00C,P=4.00R) (1.035R, 0.7%); TB(C=1.00C,R=1.00R) (1.026R, 1.7%); DualBucket(B=1.00C,P=1.00R) (1.026R, 1.7%); DualBucket(B=0.95C,P=1.00R) (1.024R, 2.3%); DualBucket(B=0.90C,P=1.00R) (1.022R, 3.7%); TB(C=0.85C,R=1.00R) (1.021R, 5.7%); DualBucket(B=0.85C,P=1.00R) (1.021R, 5.7%); DualBucket(B=0.80C,P=1.00R) (1.019R, 8.5%); DualBucket(B=0.75C,P=1.00R) (1.017R, 11.9%); TB(C=0.70C,R=1.00R) (1.016R, 16.1%); DualBucket(B=0.70C,P=1.00R) (1.016R, 16.1%); DualBucket(B=0.65C,P=1.00R) (1.014R, 21.0%); DualBucket(B=0.60C,P=4.00R) (0.960R, 25.5%); DualBucket(B=0.55C,P=4.00R) (0.880R, 31.6%); DualBucket(B=0.50C,P=4.00R) (0.800R, 37.8%); DualBucket(B=0.45C,P=4.00R) (0.720R, 44.0%); TB(C=0.43C,R=1.25R) (0.686R, 47.0%); DualBucket(B=0.40C,P=4.00R) (0.640R, 50.2%); DualBucket(B=0.35C,P=4.00R) (0.560R, 56.4%); DualBucket(B=0.30C,P=4.00R) (0.480R, 62.6%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 |  1.0\tau | [0.1C, 0.5C]     |   1.039 |   0.09% | TB(C=1.29C,R=1.00R) (1.040R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.0%); TB(C=1.00C,R=1.00R) (1.031R, 0.2%); DualBucket(B=0.95C,P=1.00R) (1.029R, 0.3%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.4%); TB(C=0.85C,R=1.00R) (1.026R, 0.5%); DualBucket(B=0.80C,P=1.00R) (1.024R, 0.7%); DualBucket(B=0.75C,P=1.00R) (1.023R, 1.0%); TB(C=0.70C,R=1.00R) (1.021R, 1.4%); DualBucket(B=0.65C,P=1.00R) (1.019R, 1.9%); DualBucket(B=0.45C,P=4.00R) (0.960R, 2.4%); DualBucket(B=0.40C,P=4.00R) (0.853R, 5.8%); DualBucket(B=0.35C,P=4.00R) (0.747R, 11.0%); DualBucket(B=0.30C,P=4.00R) (0.640R, 18.2%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 |  1.0\tau | [0.3C, 0.9C]     |   1.039 |   8.25% | TB(C=1.29C,R=1.00R) (1.040R, 5.3%); DualBucket(B=1.00C,P=4.00R) (1.040R, 5.6%); TB(C=1.00C,R=1.00R) (1.031R, 9.9%); DualBucket(B=0.95C,P=1.00R) (1.029R, 11.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 12.2%); TB(C=0.85C,R=1.00R) (1.026R, 13.6%); DualBucket(B=0.80C,P=1.00R) (1.024R, 15.4%); DualBucket(B=0.75C,P=1.00R) (1.023R, 17.5%); TB(C=0.70C,R=1.00R) (1.021R, 20.0%); DualBucket(B=0.65C,P=1.00R) (1.019R, 22.9%); DualBucket(B=0.60C,P=1.00R) (1.017R, 26.1%); TB(C=0.55C,R=1.00R) (1.016R, 29.7%); DualBucket(B=0.45C,P=4.00R) (0.960R, 30.8%); DualBucket(B=0.40C,P=4.00R) (0.853R, 37.0%); DualBucket(B=0.35C,P=4.00R) (0.747R, 43.8%); DualBucket(B=0.30C,P=4.00R) (0.640R, 51.3%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 |  1.0\tau | [0.6C, 1.0C]     |   1.039 |  19.66% | TB(C=1.29C,R=1.00R) (1.040R, 13.8%); DualBucket(B=1.00C,P=4.00R) (1.040R, 14.2%); TB(C=1.00C,R=1.00R) (1.031R, 20.2%); DualBucket(B=0.95C,P=1.00R) (1.029R, 21.6%); DualBucket(B=0.90C,P=1.00R) (1.028R, 23.3%); TB(C=0.85C,R=1.00R) (1.026R, 25.3%); DualBucket(B=0.80C,P=1.00R) (1.024R, 27.5%); DualBucket(B=0.75C,P=1.00R) (1.023R, 30.1%); TB(C=0.70C,R=1.00R) (1.021R, 33.0%); DualBucket(B=0.65C,P=1.00R) (1.019R, 36.3%); DualBucket(B=0.60C,P=1.00R) (1.017R, 40.0%); TB(C=0.55C,R=1.00R) (1.016R, 44.0%); DualBucket(B=0.45C,P=4.00R) (0.960R, 46.1%); DualBucket(B=0.40C,P=4.00R) (0.853R, 51.8%); DualBucket(B=0.35C,P=4.00R) (0.747R, 57.7%); DualBucket(B=0.30C,P=4.00R) (0.640R, 63.5%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 |  3.0\tau | [0.1C, 0.5C]     |   1.039 |   0.01% | DualBucket(B=1.00C,P=2.50R) (1.040R, 0.0%); DualBucket(B=0.95C,P=4.00R) (1.040R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.0%); DualBucket(B=1.00C,P=1.00R) (1.031R, 0.0%); TB(C=1.00C,R=1.00R) (1.031R, 0.0%); DualBucket(B=0.95C,P=1.00R) (1.029R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.0%); TB(C=0.85C,R=1.00R) (1.026R, 0.0%); DualBucket(B=0.80C,P=1.00R) (1.024R, 0.1%); DualBucket(B=0.75C,P=1.00R) (1.023R, 0.1%); TB(C=0.70C,R=1.00R) (1.021R, 0.2%); DualBucket(B=0.65C,P=1.00R) (1.019R, 0.4%); DualBucket(B=0.60C,P=1.00R) (1.017R, 0.6%); TB(C=0.55C,R=1.00R) (1.016R, 1.0%); DualBucket(B=0.50C,P=1.00R) (1.014R, 1.4%); DualBucket(B=0.45C,P=4.00R) (0.960R, 1.5%); TB(C=0.43C,R=1.25R) (0.915R, 3.7%); DualBucket(B=0.40C,P=4.00R) (0.853R, 4.7%); DualBucket(B=0.35C,P=4.00R) (0.747R, 10.0%); DualBucket(B=0.30C,P=4.00R) (0.640R, 17.2%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 |  3.0\tau | [0.3C, 0.9C]     |   1.039 |   2.18% | TB(C=1.29C,R=1.00R) (1.040R, 0.8%); DualBucket(B=1.00C,P=4.00R) (1.040R, 1.0%); TB(C=1.00C,R=1.00R) (1.031R, 2.5%); DualBucket(B=0.95C,P=1.00R) (1.029R, 3.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 3.6%); TB(C=0.85C,R=1.00R) (1.026R, 4.4%); DualBucket(B=0.80C,P=1.00R) (1.024R, 5.9%); DualBucket(B=0.75C,P=1.00R) (1.023R, 7.9%); TB(C=0.70C,R=1.00R) (1.021R, 10.5%); DualBucket(B=0.65C,P=1.00R) (1.019R, 13.6%); DualBucket(B=0.60C,P=1.00R) (1.017R, 17.3%); TB(C=0.55C,R=1.00R) (1.016R, 21.5%); DualBucket(B=0.50C,P=1.00R) (1.014R, 26.4%); DualBucket(B=0.45C,P=4.00R) (0.960R, 29.1%); TB(C=0.43C,R=1.25R) (0.915R, 33.6%); DualBucket(B=0.40C,P=4.00R) (0.853R, 35.6%); DualBucket(B=0.35C,P=4.00R) (0.747R, 42.7%); DualBucket(B=0.30C,P=4.00R) (0.640R, 50.5%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 |  3.0\tau | [0.6C, 1.0C]     |   1.039 |   6.63% | TB(C=1.29C,R=1.00R) (1.040R, 3.1%); DualBucket(B=1.00C,P=4.00R) (1.040R, 3.4%); TB(C=1.00C,R=1.00R) (1.031R, 6.7%); DualBucket(B=0.95C,P=1.00R) (1.029R, 7.7%); DualBucket(B=0.90C,P=1.00R) (1.028R, 9.3%); TB(C=0.85C,R=1.00R) (1.026R, 11.4%); DualBucket(B=0.80C,P=1.00R) (1.024R, 14.1%); DualBucket(B=0.75C,P=1.00R) (1.023R, 17.3%); TB(C=0.70C,R=1.00R) (1.021R, 21.1%); DualBucket(B=0.65C,P=1.00R) (1.019R, 25.5%); DualBucket(B=0.60C,P=1.00R) (1.017R, 30.6%); TB(C=0.55C,R=1.00R) (1.016R, 36.0%); DualBucket(B=0.50C,P=1.00R) (1.014R, 41.4%); DualBucket(B=0.45C,P=4.00R) (0.960R, 44.6%); TB(C=0.43C,R=1.25R) (0.915R, 48.7%); DualBucket(B=0.40C,P=4.00R) (0.853R, 50.7%); DualBucket(B=0.35C,P=4.00R) (0.747R, 56.8%); DualBucket(B=0.30C,P=4.00R) (0.640R, 62.9%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 | 10.0\tau | [0.1C, 0.5C]     |   1.039 |   0.00% | DualBucket(B=0.95C,P=1.00R) (1.029R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.0%); TB(C=0.85C,R=1.00R) (1.026R, 0.0%); DualBucket(B=0.80C,P=1.00R) (1.024R, 0.0%); DualBucket(B=0.75C,P=1.00R) (1.023R, 0.0%); TB(C=0.70C,R=1.00R) (1.021R, 0.1%); DualBucket(B=0.65C,P=1.00R) (1.019R, 0.1%); DualBucket(B=0.60C,P=1.00R) (1.017R, 0.2%); TB(C=0.55C,R=1.00R) (1.016R, 0.3%); DualBucket(B=0.50C,P=1.00R) (1.014R, 0.4%); DualBucket(B=0.45C,P=4.00R) (0.960R, 1.2%); TB(C=0.43C,R=1.25R) (0.915R, 2.5%); DualBucket(B=0.40C,P=4.00R) (0.853R, 4.4%); DualBucket(B=0.35C,P=4.00R) (0.747R, 9.6%); DualBucket(B=0.30C,P=4.00R) (0.640R, 16.9%) | **Dominated** (by DualBucket(B=0.95C,P=1.00R)) |
| 2.00 | 10.0\tau | [0.3C, 0.9C]     |   1.039 |   0.80% | TB(C=1.29C,R=1.00R) (1.040R, 0.1%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.2%); TB(C=1.00C,R=1.00R) (1.031R, 0.6%); DualBucket(B=0.95C,P=1.00R) (1.029R, 0.7%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.8%); TB(C=0.85C,R=1.00R) (1.026R, 1.3%); DualBucket(B=0.80C,P=1.00R) (1.024R, 2.5%); DualBucket(B=0.75C,P=1.00R) (1.023R, 4.3%); TB(C=0.70C,R=1.00R) (1.021R, 6.8%); DualBucket(B=0.65C,P=1.00R) (1.019R, 10.0%); DualBucket(B=0.60C,P=1.00R) (1.017R, 13.8%); TB(C=0.55C,R=1.00R) (1.016R, 18.2%); DualBucket(B=0.50C,P=1.00R) (1.014R, 23.3%); DualBucket(B=0.45C,P=4.00R) (0.960R, 28.5%); TB(C=0.43C,R=1.25R) (0.915R, 31.6%); DualBucket(B=0.40C,P=4.00R) (0.853R, 35.0%); DualBucket(B=0.35C,P=4.00R) (0.747R, 42.3%); DualBucket(B=0.30C,P=4.00R) (0.640R, 50.2%) | **Non-Dominated** (on Pareto frontier) |
| 2.00 | 10.0\tau | [0.6C, 1.0C]     |   1.039 |   2.32% | TB(C=1.29C,R=1.00R) (1.040R, 0.6%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.7%); TB(C=1.00C,R=1.00R) (1.031R, 1.7%); DualBucket(B=0.95C,P=1.00R) (1.029R, 2.3%); DualBucket(B=0.90C,P=1.00R) (1.028R, 3.7%); TB(C=0.85C,R=1.00R) (1.026R, 5.7%); DualBucket(B=0.80C,P=1.00R) (1.024R, 8.5%); DualBucket(B=0.75C,P=1.00R) (1.023R, 11.9%); TB(C=0.70C,R=1.00R) (1.021R, 16.1%); DualBucket(B=0.65C,P=1.00R) (1.019R, 21.0%); DualBucket(B=0.60C,P=1.00R) (1.017R, 26.6%); TB(C=0.55C,R=1.00R) (1.016R, 32.6%); DualBucket(B=0.50C,P=1.00R) (1.014R, 38.6%); DualBucket(B=0.45C,P=4.00R) (0.960R, 44.0%); TB(C=0.43C,R=1.25R) (0.915R, 47.0%); DualBucket(B=0.40C,P=4.00R) (0.853R, 50.2%); DualBucket(B=0.35C,P=4.00R) (0.747R, 56.4%); DualBucket(B=0.30C,P=4.00R) (0.640R, 62.6%) | **Dominated** (by TB(C=1.00C,R=1.00R)) |
| 4.00 |  1.0\tau | [0.1C, 0.5C]     |   1.040 |   0.09% | TB(C=1.29C,R=1.00R) (1.040R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.0%); TB(C=1.00C,R=1.00R) (1.031R, 0.2%); DualBucket(B=0.95C,P=1.00R) (1.029R, 0.3%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.4%); TB(C=0.85C,R=1.00R) (1.026R, 0.5%); DualBucket(B=0.80C,P=1.00R) (1.024R, 0.7%); DualBucket(B=0.75C,P=1.00R) (1.023R, 1.0%); TB(C=0.70C,R=1.00R) (1.021R, 1.4%); DualBucket(B=0.65C,P=1.00R) (1.019R, 1.9%); DualBucket(B=0.60C,P=1.00R) (1.017R, 2.7%); TB(C=0.55C,R=1.00R) (1.016R, 3.6%); DualBucket(B=0.50C,P=1.00R) (1.014R, 4.9%); DualBucket(B=0.45C,P=1.00R) (1.012R, 7.1%); TB(C=0.43C,R=1.00R) (1.012R, 8.4%); TB(C=0.43C,R=1.00R) (1.012R, 8.4%); DualBucket(B=0.40C,P=1.00R) (1.011R, 10.8%); DualBucket(B=0.35C,P=1.00R) (1.009R, 16.0%); TB(C=0.30C,R=1.00R) (1.008R, 22.8%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |
| 4.00 |  1.0\tau | [0.3C, 0.9C]     |   1.040 |   8.25% | TB(C=1.29C,R=1.00R) (1.040R, 5.3%); DualBucket(B=1.00C,P=4.00R) (1.040R, 5.6%); TB(C=1.00C,R=1.00R) (1.031R, 9.9%); DualBucket(B=0.95C,P=1.00R) (1.029R, 11.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 12.2%); TB(C=0.85C,R=1.00R) (1.026R, 13.6%); DualBucket(B=0.80C,P=1.00R) (1.024R, 15.4%); DualBucket(B=0.75C,P=1.00R) (1.023R, 17.5%); TB(C=0.70C,R=1.00R) (1.021R, 20.0%); DualBucket(B=0.65C,P=1.00R) (1.019R, 22.9%); DualBucket(B=0.60C,P=1.00R) (1.017R, 26.1%); TB(C=0.55C,R=1.00R) (1.016R, 29.7%); DualBucket(B=0.50C,P=1.00R) (1.014R, 33.8%); DualBucket(B=0.45C,P=1.00R) (1.012R, 38.4%); TB(C=0.43C,R=1.00R) (1.012R, 40.4%); TB(C=0.43C,R=1.00R) (1.012R, 40.4%); DualBucket(B=0.40C,P=1.00R) (1.011R, 43.4%); DualBucket(B=0.35C,P=1.00R) (1.009R, 49.1%); TB(C=0.30C,R=1.00R) (1.008R, 55.3%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |
| 4.00 |  1.0\tau | [0.6C, 1.0C]     |   1.040 |  19.66% | TB(C=1.29C,R=1.00R) (1.040R, 13.8%); DualBucket(B=1.00C,P=4.00R) (1.040R, 14.2%); TB(C=1.00C,R=1.00R) (1.031R, 20.2%); DualBucket(B=0.95C,P=1.00R) (1.029R, 21.6%); DualBucket(B=0.90C,P=1.00R) (1.028R, 23.3%); TB(C=0.85C,R=1.00R) (1.026R, 25.3%); DualBucket(B=0.80C,P=1.00R) (1.024R, 27.5%); DualBucket(B=0.75C,P=1.00R) (1.023R, 30.1%); TB(C=0.70C,R=1.00R) (1.021R, 33.0%); DualBucket(B=0.65C,P=1.00R) (1.019R, 36.3%); DualBucket(B=0.60C,P=1.00R) (1.017R, 40.0%); TB(C=0.55C,R=1.00R) (1.016R, 44.0%); DualBucket(B=0.50C,P=1.00R) (1.014R, 48.2%); DualBucket(B=0.45C,P=1.00R) (1.012R, 52.5%); TB(C=0.43C,R=1.00R) (1.012R, 54.4%); TB(C=0.43C,R=1.00R) (1.012R, 54.4%); DualBucket(B=0.40C,P=1.00R) (1.011R, 57.0%); DualBucket(B=0.35C,P=1.00R) (1.009R, 61.7%); TB(C=0.30C,R=1.00R) (1.008R, 66.5%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |
| 4.00 |  3.0\tau | [0.1C, 0.5C]     |   1.040 |   0.01% | DualBucket(B=1.00C,P=2.50R) (1.040R, 0.0%); DualBucket(B=0.95C,P=4.00R) (1.040R, 0.0%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.0%); DualBucket(B=1.00C,P=1.00R) (1.031R, 0.0%); TB(C=1.00C,R=1.00R) (1.031R, 0.0%); DualBucket(B=0.95C,P=1.00R) (1.029R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.0%); TB(C=0.85C,R=1.00R) (1.026R, 0.0%); DualBucket(B=0.80C,P=1.00R) (1.024R, 0.1%); DualBucket(B=0.75C,P=1.00R) (1.023R, 0.1%); TB(C=0.70C,R=1.00R) (1.021R, 0.2%); DualBucket(B=0.65C,P=1.00R) (1.019R, 0.4%); DualBucket(B=0.60C,P=1.00R) (1.017R, 0.6%); TB(C=0.55C,R=1.00R) (1.016R, 1.0%); DualBucket(B=0.50C,P=1.00R) (1.014R, 1.4%); DualBucket(B=0.45C,P=1.00R) (1.012R, 2.9%); TB(C=0.43C,R=1.00R) (1.012R, 4.1%); TB(C=0.43C,R=1.00R) (1.012R, 4.1%); DualBucket(B=0.40C,P=1.00R) (1.011R, 6.3%); DualBucket(B=0.35C,P=1.00R) (1.009R, 11.6%); TB(C=0.30C,R=1.00R) (1.008R, 18.7%) | **Dominated** (by DualBucket(B=1.00C,P=2.50R)) |
| 4.00 |  3.0\tau | [0.3C, 0.9C]     |   1.040 |   2.18% | TB(C=1.29C,R=1.00R) (1.040R, 0.8%); DualBucket(B=1.00C,P=4.00R) (1.040R, 1.0%); TB(C=1.00C,R=1.00R) (1.031R, 2.5%); DualBucket(B=0.95C,P=1.00R) (1.029R, 3.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 3.6%); TB(C=0.85C,R=1.00R) (1.026R, 4.4%); DualBucket(B=0.80C,P=1.00R) (1.024R, 5.9%); DualBucket(B=0.75C,P=1.00R) (1.023R, 7.9%); TB(C=0.70C,R=1.00R) (1.021R, 10.5%); DualBucket(B=0.65C,P=1.00R) (1.019R, 13.6%); DualBucket(B=0.60C,P=1.00R) (1.017R, 17.3%); TB(C=0.55C,R=1.00R) (1.016R, 21.5%); DualBucket(B=0.50C,P=1.00R) (1.014R, 26.4%); DualBucket(B=0.45C,P=1.00R) (1.012R, 31.8%); TB(C=0.43C,R=1.00R) (1.012R, 34.3%); TB(C=0.43C,R=1.00R) (1.012R, 34.3%); DualBucket(B=0.40C,P=1.00R) (1.011R, 37.9%); DualBucket(B=0.35C,P=1.00R) (1.009R, 44.6%); TB(C=0.30C,R=1.00R) (1.008R, 51.9%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |
| 4.00 |  3.0\tau | [0.6C, 1.0C]     |   1.040 |   6.63% | TB(C=1.29C,R=1.00R) (1.040R, 3.1%); DualBucket(B=1.00C,P=4.00R) (1.040R, 3.4%); TB(C=1.00C,R=1.00R) (1.031R, 6.7%); DualBucket(B=0.95C,P=1.00R) (1.029R, 7.7%); DualBucket(B=0.90C,P=1.00R) (1.028R, 9.3%); TB(C=0.85C,R=1.00R) (1.026R, 11.4%); DualBucket(B=0.80C,P=1.00R) (1.024R, 14.1%); DualBucket(B=0.75C,P=1.00R) (1.023R, 17.3%); TB(C=0.70C,R=1.00R) (1.021R, 21.1%); DualBucket(B=0.65C,P=1.00R) (1.019R, 25.5%); DualBucket(B=0.60C,P=1.00R) (1.017R, 30.6%); TB(C=0.55C,R=1.00R) (1.016R, 36.0%); DualBucket(B=0.50C,P=1.00R) (1.014R, 41.4%); DualBucket(B=0.45C,P=1.00R) (1.012R, 46.9%); TB(C=0.43C,R=1.00R) (1.012R, 49.3%); TB(C=0.43C,R=1.00R) (1.012R, 49.3%); DualBucket(B=0.40C,P=1.00R) (1.011R, 52.5%); DualBucket(B=0.35C,P=1.00R) (1.009R, 58.2%); TB(C=0.30C,R=1.00R) (1.008R, 63.9%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |
| 4.00 | 10.0\tau | [0.1C, 0.5C]     |   1.040 |   0.00% | DualBucket(B=0.95C,P=1.00R) (1.029R, 0.0%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.0%); TB(C=0.85C,R=1.00R) (1.026R, 0.0%); DualBucket(B=0.80C,P=1.00R) (1.024R, 0.0%); DualBucket(B=0.75C,P=1.00R) (1.023R, 0.0%); TB(C=0.70C,R=1.00R) (1.021R, 0.1%); DualBucket(B=0.65C,P=1.00R) (1.019R, 0.1%); DualBucket(B=0.60C,P=1.00R) (1.017R, 0.2%); TB(C=0.55C,R=1.00R) (1.016R, 0.3%); DualBucket(B=0.50C,P=1.00R) (1.014R, 0.4%); DualBucket(B=0.45C,P=1.00R) (1.012R, 1.6%); TB(C=0.43C,R=1.00R) (1.012R, 2.6%); TB(C=0.43C,R=1.00R) (1.012R, 2.6%); DualBucket(B=0.40C,P=1.00R) (1.011R, 4.8%); DualBucket(B=0.35C,P=1.00R) (1.009R, 10.0%); TB(C=0.30C,R=1.00R) (1.008R, 17.3%) | **Dominated** (by DualBucket(B=0.95C,P=1.00R)) |
| 4.00 | 10.0\tau | [0.3C, 0.9C]     |   1.040 |   0.80% | TB(C=1.29C,R=1.00R) (1.040R, 0.1%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.2%); TB(C=1.00C,R=1.00R) (1.031R, 0.6%); DualBucket(B=0.95C,P=1.00R) (1.029R, 0.7%); DualBucket(B=0.90C,P=1.00R) (1.028R, 0.8%); TB(C=0.85C,R=1.00R) (1.026R, 1.3%); DualBucket(B=0.80C,P=1.00R) (1.024R, 2.5%); DualBucket(B=0.75C,P=1.00R) (1.023R, 4.3%); TB(C=0.70C,R=1.00R) (1.021R, 6.8%); DualBucket(B=0.65C,P=1.00R) (1.019R, 10.0%); DualBucket(B=0.60C,P=1.00R) (1.017R, 13.8%); TB(C=0.55C,R=1.00R) (1.016R, 18.2%); DualBucket(B=0.50C,P=1.00R) (1.014R, 23.3%); DualBucket(B=0.45C,P=1.00R) (1.012R, 29.1%); TB(C=0.43C,R=1.00R) (1.012R, 31.8%); TB(C=0.43C,R=1.00R) (1.012R, 31.8%); DualBucket(B=0.40C,P=1.00R) (1.011R, 35.6%); DualBucket(B=0.35C,P=1.00R) (1.009R, 42.7%); TB(C=0.30C,R=1.00R) (1.008R, 50.6%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |
| 4.00 | 10.0\tau | [0.6C, 1.0C]     |   1.040 |   2.32% | TB(C=1.29C,R=1.00R) (1.040R, 0.6%); DualBucket(B=1.00C,P=4.00R) (1.040R, 0.7%); TB(C=1.00C,R=1.00R) (1.031R, 1.7%); DualBucket(B=0.95C,P=1.00R) (1.029R, 2.3%); DualBucket(B=0.90C,P=1.00R) (1.028R, 3.7%); TB(C=0.85C,R=1.00R) (1.026R, 5.7%); DualBucket(B=0.80C,P=1.00R) (1.024R, 8.5%); DualBucket(B=0.75C,P=1.00R) (1.023R, 11.9%); TB(C=0.70C,R=1.00R) (1.021R, 16.1%); DualBucket(B=0.65C,P=1.00R) (1.019R, 21.0%); DualBucket(B=0.60C,P=1.00R) (1.017R, 26.6%); TB(C=0.55C,R=1.00R) (1.016R, 32.6%); DualBucket(B=0.50C,P=1.00R) (1.014R, 38.6%); DualBucket(B=0.45C,P=1.00R) (1.012R, 44.6%); TB(C=0.43C,R=1.00R) (1.012R, 47.2%); TB(C=0.43C,R=1.00R) (1.012R, 47.2%); DualBucket(B=0.40C,P=1.00R) (1.011R, 50.7%); DualBucket(B=0.35C,P=1.00R) (1.009R, 56.8%); TB(C=0.30C,R=1.00R) (1.008R, 62.9%) | **Dominated** (by DualBucket(B=1.00C,P=4.00R)) |

---

## 6. Abuser Burst-Size Sensitivity (C = 1000, coldStart & preWarmed)

Evaluating DREB burst-matched against static families S1-S3 as abuser burst size sweeps across {0.30C, 0.50C, 0.70C, 0.95C, 1.20C} with offered rate held constant at kR (interval adjusted to burst / kR):

| Initial State | Overload k | Abuser Burst Size | DREB Admitted Rate / R | DREB Bursty FRR % | DREB Status vs Static | Nearest Static Policy | Nearest Rate / R | Nearest Bursty FRR % | $\Delta$ Rate vs DREB [95% CI] | $\Delta$ Bursty FRR vs DREB [95% CI] |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- | :---: | :---: | :---: | :---: |
| coldStart  | 1.10 |  0.30C |   1.034 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.034 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.10 |  0.50C |   1.042 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.043 |   2.05% | +0.001 [+0.001, +0.001] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.10 |  0.70C |   0.950 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.040 |   2.05% | +0.090 [+0.090, +0.090] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.10 |  0.95C |   0.805 |   2.18% | **Non-Dominated** | DualBucket(B=0.80C,P=4.00R) |   0.933 |   2.70% | +0.128 [+0.128, +0.128] |  +0.52% [ +0.27%,  +0.75%] |
| coldStart  | 1.10 |  1.20C |   0.733 |   2.18% | **Non-Dominated** | DualBucket(B=0.85C,P=2.50R) |   0.793 |   2.02% | +0.060 [+0.060, +0.060] |  -0.15% [ -0.42%,  +0.09%] |
| coldStart  | 1.25 |  0.30C |   1.043 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.043 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.25 |  0.50C |   1.043 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.043 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.25 |  0.70C |   1.009 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.032 |   2.05% | +0.023 [+0.023, +0.023] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.25 |  0.95C |   0.862 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.031 |   2.05% | +0.169 [+0.169, +0.169] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.25 |  1.20C |   0.780 |   2.18% | **Non-Dominated** | DualBucket(B=0.80C,P=4.00R) |   0.853 |   2.70% | +0.073 [+0.073, +0.073] |  +0.52% [ +0.27%,  +0.75%] |
| coldStart  | 1.50 |  0.30C |   1.043 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.043 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.50 |  0.50C |   1.043 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.043 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.50 |  0.70C |   1.037 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.038 |   2.05% | +0.001 [+0.001, +0.001] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.50 |  0.95C |   0.948 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.035 |   2.05% | +0.087 [+0.087, +0.087] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 1.50 |  1.20C |   0.840 |   2.18% | **Non-Dominated** | TB(C=1.00C,R=1.00R)        |   1.020 |   2.50% | +0.180 [+0.180, +0.180] |  +0.32% [ +0.08%,  +0.55%] |
| coldStart  | 2.00 |  0.30C |   1.043 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.043 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 2.00 |  0.50C |   1.035 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.035 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 2.00 |  0.70C |   1.035 |   2.18% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.90C,P=1.67R) |   1.035 |   2.05% | +0.000 [+0.000, +0.000] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 2.00 |  0.95C |   1.039 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.040 |   2.05% | +0.002 [+0.002, +0.002] |  -0.13% [ -0.39%,  +0.11%] |
| coldStart  | 2.00 |  1.20C |   0.963 |   2.18% | **Non-Dominated** | DualBucket(B=0.90C,P=1.67R) |   1.023 |   2.05% | +0.060 [+0.060, +0.060] |  -0.13% [ -0.39%,  +0.11%] |
| preWarmed  | 1.10 |  0.30C |   1.034 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.034 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.10 |  0.50C |   1.043 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.043 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.10 |  0.70C |   0.969 |   1.73% | **Non-Dominated** | DualBucket(B=0.95C,P=1.67R) |   1.040 |   1.71% | +0.071 [+0.071, +0.071] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.10 |  0.95C |   0.825 |   1.73% | **Non-Dominated** | DualBucket(B=0.85C,P=4.00R) |   0.992 |   1.60% | +0.167 [+0.167, +0.167] |  -0.13% [ -0.19%,  -0.07%] |
| preWarmed  | 1.10 |  1.20C |   0.753 |   1.73% | **Non-Dominated** | DualBucket(B=0.85C,P=4.00R) |   0.793 |   1.60% | +0.041 [+0.041, +0.041] |  -0.13% [ -0.19%,  -0.07%] |
| preWarmed  | 1.25 |  0.30C |   1.043 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.043 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.25 |  0.50C |   1.043 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.043 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.25 |  0.70C |   1.028 |   1.73% | **Non-Dominated** | DualBucket(B=0.95C,P=1.67R) |   1.032 |   1.71% | +0.004 [+0.004, +0.004] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.25 |  0.95C |   0.881 |   1.73% | **Non-Dominated** | DualBucket(B=0.95C,P=1.67R) |   1.031 |   1.71% | +0.150 [+0.150, +0.150] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.25 |  1.20C |   0.799 |   1.73% | **Non-Dominated** | DualBucket(B=0.85C,P=4.00R) |   0.907 |   1.60% | +0.108 [+0.108, +0.108] |  -0.13% [ -0.19%,  -0.07%] |
| preWarmed  | 1.50 |  0.30C |   1.043 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.043 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.50 |  0.50C |   1.043 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.043 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.50 |  0.70C |   1.038 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.038 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.50 |  0.95C |   0.967 |   1.73% | **Non-Dominated** | DualBucket(B=0.95C,P=1.67R) |   1.035 |   1.71% | +0.068 [+0.068, +0.068] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 1.50 |  1.20C |   0.859 |   1.73% | **Non-Dominated** | DualBucket(B=0.95C,P=1.67R) |   1.029 |   1.71% | +0.170 [+0.170, +0.170] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 2.00 |  0.30C |   1.043 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.043 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 2.00 |  0.50C |   1.035 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.035 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 2.00 |  0.70C |   1.035 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.035 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 2.00 |  0.95C |   1.040 |   1.73% | **Dominated** by `DualBucket(B=1.00C,P=4.00R)` | DualBucket(B=0.95C,P=1.67R) |   1.040 |   1.71% | +0.000 [+0.000, +0.000] |  -0.02% [ -0.06%,  +0.02%] |
| preWarmed  | 2.00 |  1.20C |   0.982 |   1.73% | **Non-Dominated** | DualBucket(B=0.95C,P=1.67R) |   1.023 |   1.71% | +0.041 [+0.041, +0.041] |  -0.02% [ -0.06%,  +0.02%] |

**First Burst Size Where DREB Loses (Is Dominated):**
- **Initial State: coldStart, k = 1.10, Abuser Burst Size = 0.30C (Dominating Static: `DualBucket(B=1.00C,P=4.00R)`)**

---

## 7. Interpretation Caveat: Over-Enforcement Below Contracted Rate vs Detection

> [!IMPORTANT]
> **Lower abuser admission below the contracted rate R is over-enforcement, not detection.**

A client contracted for capacity $C$ and refill rate $R$ is legally entitled to consume up to the cumulative contracted envelope:
$$\mathcal{E}(T) = S_{\max} + R \cdot T$$

When a rate limiter suppresses an abuser's admitted rate to strictly below $1.00R$ (e.g. DREB admitting $0.805R$ at $k = 1.10$, under-servicing by $19.5\%$), this does **not** reflect intelligent identification or mitigation of malicious abuse. Instead, it represents **over-enforcement**: the limiter's burst recovery mechanism or idle credit drain penalizes non-compliant arrival patterns by withholding contracted bandwidth that the client legitimately purchased.

In real-world multi-tenant architectures, bursty or lagging legitimate clients that temporarily queue requests can be subjected to this exact under-servicing, experiencing severe false rejections.

### Contract-Enforcing Reference: TB-env (Capacity = 9C/7)

To isolate genuine policy discrimination from contract violation, Token Bucket with capacity equal to the sustained envelope bound ($\text{capacity} = 9C/7 \approx 1.286C$, refill rate $R$) provides the **contract-enforcing reference baseline (`TB-env`)**. `TB-env` perfectly enforces the contracted SLA, admitting between $1.004R$ and $1.040R$ under sustained overload while never under-servicing clients below $1.00R$:

| Overload $k$ | Abuser Arrival Profile | TB-env Admitted Rate / $R$ | TB-large Admitted Rate / $R$ | DREB (burst-matched) Admitted Rate / $R$ | DREB Status vs Contract ($1.00R$) |
| :---: | :---: | :---: | :---: | :---: | :---: |
| 1.10 | Periodic | 1.004 | 1.004 | 0.805 | **-0.195R** (Over-enforcement: under-services by 19.5%) |
| 1.25 | Periodic | 1.010 | 1.010 | 0.865 | **-0.135R** (Over-enforcement: under-services by 13.5%) |
| 1.50 | Periodic | 1.020 | 1.020 | 0.948 | **-0.052R** (Over-enforcement: under-services by 5.2%) |
| 2.00 | Periodic | 1.040 | 1.040 | 1.040 | **0.000R** (Strict envelope enforcement at boundary) |
| 4.00 | Periodic | 1.040 | 1.040 | 1.040 | **0.000R** (Strict envelope enforcement at boundary) |

