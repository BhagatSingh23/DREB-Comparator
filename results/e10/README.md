# Experiment E10: Peak Short-Window Admitted Load & 3-Metric Dominance

## 1. Executive Summary & Hypothesis Verdict (Strict Original Wording)

### Hypothesis H10
> **Statement:** *against abuser traffic at k in {1.1, 1.25, 1.5, 2, 4}, DREB burst-matched has lower peak short-window admitted load than any static policy that is not worse on both admitted rate and bursty-legit FRR.*

#### Verdict (Overall & Per Initial State):
- **Overall Verdict:** **REFUTED** (judged by paired 95% bootstrap confidence intervals over seeds 2000–2029, evaluated without tolerance margins).
- **`coldStart` Verdict:** **REFUTED**.
  - *Reason:* At small abuser bursts ($0.30C$), DREB's peak short-window load ties identically with non-worse static policies (`DualBucket(B=0.85C, P=4.00R)`) across all evaluated window sizes ($\Delta \text{PeakLoad} = +0.000C \ [+0.000, +0.000]$). Because H10 strictly demands that DREB have *strictly lower* peak load than every non-worse static policy, these ties refute H10. (At larger bursts $\ge 0.50C$ where non-worse static policies exist, DREB does achieve strictly lower peak load by $+0.008C$ to $+0.498C$).
- **`preWarmed` Verdict:** **REFUTED**.
  - *Reason:* At bursts $0.30C$ and $0.50C$, DREB ties peak load identically with non-worse static policies ($\Delta \text{PeakLoad} = +0.000C$). Furthermore, at large bursts ($\ge 0.95C$) under high overload ($k \ge 2.00$), DREB admits *strictly higher* peak load than static policies (e.g. at burst $0.95C$, static peak is $0.100C$ lower than DREB; at burst $1.20C$, static peak is $0.150C$ lower than DREB, paired CI $[-0.150, -0.150]$), directly violating H10.

---

### Core Findings & Failure Mechanisms:
1. **Equal Peak Window Load at Small Abuser Bursts ($0.30C$):**
   When the abuser sends bursts of size $0.30C$, the entire burst fits within the base burst bucket of static dual policies (`DualBucket(B=0.85C, P=4.00R)`) as well as DREB ($B_{\max} = 0.43C$). Across all evaluated short windows ($W \in \{0.05, 0.10, 0.25\} C/R$), both DREB and non-worse static policies admit the exact same peak cost ($\Delta \text{PeakLoad} = +0.000 [+0.000, +0.000]$). Because H10 strictly requires DREB to have *lower* peak admitted load ($\Delta \text{PeakLoad} > 0$), this equality immediately refutes H10.
2. **Static Dual Buckets Achieve Lower Peak Load under Pre-Warmed States:**
   Under `preWarmed` initial conditions at higher overload (e.g. $k = 2.00$ and $k = 4.00$, burst $0.95C$), static `DualBucket(B=0.85C, P=4.00R)` achieves strictly lower peak admitted window load than DREB:
   $$\Delta \text{PeakLoad} = \text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} = -0.100 \quad [-0.100, -0.100]$$
   Here, DREB admits $0.95C$ in a 50ms window from pre-warmed burst capacity plus accumulated credit, whereas the static limiter restricts the abuser to its configured ceiling, directly violating H10.
3. **Three-Metric Evaluation Clarification:**
   While static dual buckets with equal peak load tie DREB at small bursts ($0.30C$) and achieve lower peak load under pre-warmed states at $k \ge 2.00$, under cold start with large bursts (e.g. $k = 4.00$, burst $0.95C$), DREB is 2-metric dominated in E9 by `DualBucket(B=1.00C, P=4.00R)` yet admits substantially *lower* peak short-window load ($0.428C$ vs $0.850C$, $\Delta = +0.422C$). Hence DREB is not universally 3-metric dominated in cold start.

---

### Summary Breakdown Table: Initial State $\times$ Abuser Burst $\times$ Window $W$
Count of overload $k \in \{1.10, 1.25, 1.50, 2.00, 4.00\}$ cells where DREB has strictly lower peak load, ties, strictly higher, or vacuous (no non-worse static candidate):

| Initial State | Abuser Burst | Window W | DREB Strictly Lower (k cells) | Ties (k cells) | DREB Strictly Higher (k cells) | Vacuous (No Static $\le$) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| coldStart  |  0.30C |  0.05C/R |  0 |  5 |  0 |  0 |
| coldStart  |  0.30C |  0.10C/R |  1 |  4 |  0 |  0 |
| coldStart  |  0.30C |  0.25C/R |  1 |  4 |  0 |  0 |
| coldStart  |  0.50C |  0.05C/R |  4 |  0 |  0 |  1 |
| coldStart  |  0.50C |  0.10C/R |  4 |  0 |  0 |  1 |
| coldStart  |  0.50C |  0.25C/R |  4 |  0 |  0 |  1 |
| coldStart  |  0.70C |  0.05C/R |  2 |  0 |  0 |  3 |
| coldStart  |  0.70C |  0.10C/R |  2 |  0 |  0 |  3 |
| coldStart  |  0.70C |  0.25C/R |  2 |  0 |  0 |  3 |
| coldStart  |  0.95C |  0.05C/R |  1 |  0 |  0 |  4 |
| coldStart  |  0.95C |  0.10C/R |  1 |  0 |  0 |  4 |
| coldStart  |  0.95C |  0.25C/R |  1 |  0 |  0 |  4 |
| coldStart  |  1.20C |  0.05C/R |  1 |  0 |  0 |  4 |
| coldStart  |  1.20C |  0.10C/R |  1 |  0 |  0 |  4 |
| coldStart  |  1.20C |  0.25C/R |  1 |  0 |  0 |  4 |
| preWarmed  |  0.30C |  0.05C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.30C |  0.10C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.30C |  0.25C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.50C |  0.05C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.50C |  0.10C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.50C |  0.25C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.70C |  0.05C/R |  0 |  3 |  0 |  2 |
| preWarmed  |  0.70C |  0.10C/R |  0 |  3 |  0 |  2 |
| preWarmed  |  0.70C |  0.25C/R |  0 |  3 |  0 |  2 |
| preWarmed  |  0.95C |  0.05C/R |  0 |  0 |  2 |  3 |
| preWarmed  |  0.95C |  0.10C/R |  0 |  0 |  2 |  3 |
| preWarmed  |  0.95C |  0.25C/R |  0 |  1 |  1 |  3 |
| preWarmed  |  1.20C |  0.05C/R |  0 |  0 |  1 |  4 |
| preWarmed  |  1.20C |  0.10C/R |  0 |  0 |  1 |  4 |
| preWarmed  |  1.20C |  0.25C/R |  0 |  0 |  1 |  4 |

---

### Detailed Table: coldStart at Bursts $\ge 0.70C$ Across Overload $k$
Evaluated for each window size $W \in \{0.05C/R, 0.10C/R, 0.25C/R\}$ and burst size $\in \{0.70C, 0.95C, 1.20C\}$:

| Window W | Overload k | Burst Size | DREB Peak Load | Best Non-Worse Static Policy | Static Peak Load | Paired $\Delta$ Peak [95% CI] | Verdict |
| :---: | :---: | :---: | :---: | :--- | :---: | :---: | :---: |
| 0.05C/R | 1.10 | 0.70C | 0.598C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.25 | 0.70C | 0.564C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.50 | 0.70C | 0.522C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 2.00 | 0.70C | 0.470C | DualBucket(B=0.85C,P=4.00R) | 0.700C | +0.230C [+0.230, +0.230] | SUPPORTED (DREB Lower) |
| 0.05C/R | 4.00 | 0.70C | 0.428C | DualBucket(B=0.85C,P=4.00R) | 0.700C | +0.272C [+0.272, +0.272] | SUPPORTED (DREB Lower) |
| 0.05C/R | 1.10 | 0.95C | 0.699C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.25 | 0.95C | 0.653C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.50 | 0.95C | 0.597C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 2.00 | 0.95C | 0.526C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 4.00 | 0.95C | 0.428C | DualBucket(B=0.85C,P=4.00R) | 0.850C | +0.422C [+0.422, +0.422] | SUPPORTED (DREB Lower) |
| 0.05C/R | 1.10 | 1.20C | 0.800C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.25 | 1.20C | 0.741C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.50 | 1.20C | 0.670C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 2.00 | 1.20C | 0.581C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 4.00 | 1.20C | 0.448C | DualBucket(B=0.85C,P=4.00R) | 0.850C | +0.402C [+0.402, +0.402] | SUPPORTED (DREB Lower) |
| 0.10C/R | 1.10 | 0.70C | 0.598C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.25 | 0.70C | 0.564C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.50 | 0.70C | 0.522C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 2.00 | 0.70C | 0.470C | DualBucket(B=0.85C,P=4.00R) | 0.700C | +0.230C [+0.230, +0.230] | SUPPORTED (DREB Lower) |
| 0.10C/R | 4.00 | 0.70C | 0.428C | DualBucket(B=0.85C,P=4.00R) | 0.700C | +0.272C [+0.272, +0.272] | SUPPORTED (DREB Lower) |
| 0.10C/R | 1.10 | 0.95C | 0.699C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.25 | 0.95C | 0.653C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.50 | 0.95C | 0.597C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 2.00 | 0.95C | 0.526C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 4.00 | 0.95C | 0.428C | DualBucket(B=0.85C,P=4.00R) | 0.850C | +0.422C [+0.422, +0.422] | SUPPORTED (DREB Lower) |
| 0.10C/R | 1.10 | 1.20C | 0.800C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.25 | 1.20C | 0.741C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.50 | 1.20C | 0.670C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 2.00 | 1.20C | 0.581C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 4.00 | 1.20C | 0.448C | DualBucket(B=0.85C,P=4.00R) | 0.850C | +0.402C [+0.402, +0.402] | SUPPORTED (DREB Lower) |
| 0.25C/R | 1.10 | 0.70C | 0.598C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.25 | 0.70C | 0.564C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.50 | 0.70C | 0.522C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 2.00 | 0.70C | 0.470C | DualBucket(B=0.85C,P=4.00R) | 0.700C | +0.230C [+0.230, +0.230] | SUPPORTED (DREB Lower) |
| 0.25C/R | 4.00 | 0.70C | 0.720C | DualBucket(B=1.00C,P=1.25R) | 1.218C | +0.498C [+0.498, +0.498] | SUPPORTED (DREB Lower) |
| 0.25C/R | 1.10 | 0.95C | 0.699C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.25 | 0.95C | 0.653C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.50 | 0.95C | 0.597C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 2.00 | 0.95C | 0.526C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 4.00 | 0.95C | 0.825C | DualBucket(B=1.00C,P=1.25R) | 1.297C | +0.472C [+0.472, +0.472] | SUPPORTED (DREB Lower) |
| 0.25C/R | 1.10 | 1.20C | 0.800C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.25 | 1.20C | 0.741C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.50 | 1.20C | 0.670C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 2.00 | 1.20C | 0.581C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 4.00 | 1.20C | 0.448C | DualBucket(B=0.85C,P=4.00R) | 0.850C | +0.402C [+0.402, +0.402] | SUPPORTED (DREB Lower) |

---

## 2. Experimental Methodology & Metric Definition

- **Capacity & Refill Rate:** $C = 1000$ tokens, $R = 1000$ tokens/s ($\tau = 1.286$ s).
- **Abuser Traffic Grid:**
  - Overload $k \in \{1.10, 1.25, 1.50, 2.00, 4.00\}$
  - Burst size $\in \{0.30C, 0.50C, 0.70C, 0.95C, 1.20C\}$
  - Initial state: `coldStart` and `preWarmed`
  - Replications: Seeds 2000–2029 ($N = 30$)
- **Short-Window Evaluation Metric:**
  For each client and each window duration $W \in \{0.05C/R, 0.10C/R, 0.25C/R\}$ ($50\text{ ms}, 100\text{ ms}, 250\text{ ms}$), the peak admitted cost in any sliding continuous window $[t, t+W]$ is computed across the 30-second trace:
  $$\text{PeakLoad}(W) = \frac{\max_{t} \sum_{j: t \le t_j \le t+W} a_j}{C}$$
- **Dominance Criteria:**
  - Non-worse on Rate & FRR: $\text{Rate}_{\text{static}} - \text{Rate}_{\text{DREB}} \le 0$ and $\text{FRR}_{\text{static}} - \text{FRR}_{\text{DREB}} \le 0$ (paired 95% CI upper bound $\le 0$).
  - H10 Condition: $\text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} > 0$ (paired 95% CI lower bound $> 0$).
  - 3-Metric Dominance: Static policy is $\le$ on all three metrics with at least one strictly lower.

---

## 3. Comprehensive H10 Evaluation Table (All Scenarios & Window Sizes)

*Caption: Seeds 2000–2029 (N=30), Candidate minus Reference bootstrap paired 95% CIs. W in units of C/R (50ms, 100ms, 250ms).*

| Initial State | Overload $k$ | Abuser Burst | Window $W$ | Verdict H10 | Best Static (Rate & FRR $\le$ DREB) | $\Delta$ PeakLoad vs DREB [95% CI] | 3-Metric Dominated? |
| :---: | :---: | :---: | :---: | :---: | :--- | :---: | :---: |
| coldStart | 1.10 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.10 | 0.50C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.10 | 0.70C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.10 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.10 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.25 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.25 | 0.50C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.008 [+0.008, +0.008] | **YES (Dominated)** |
| coldStart | 1.25 | 0.70C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.25 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.25 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.50 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.50 | 0.50C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.037 [+0.037, +0.037] | **YES (Dominated)** |
| coldStart | 1.50 | 0.70C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.50 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 1.50 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 2.00 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 2.00 | 0.50C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.072 [+0.072, +0.072] | **YES (Dominated)** |
| coldStart | 2.00 | 0.70C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.230 [+0.230, +0.230] | **YES (Dominated)** |
| coldStart | 2.00 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 2.00 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| coldStart | 4.00 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 4.00 | 0.50C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.072 [+0.072, +0.072] | **YES (Dominated)** |
| coldStart | 4.00 | 0.70C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.272 [+0.272, +0.272] | **YES (Dominated)** |
| coldStart | 4.00 | 0.95C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.422 [+0.422, +0.422] | **YES (Dominated)** |
| coldStart | 4.00 | 1.20C | 0.05C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.402 [+0.402, +0.402] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.50C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.70C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.10 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.10 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.25 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.25 | 0.50C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.25 | 0.70C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.25 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.25 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.50 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.50C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.70C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.95C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 1.50 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 2.00 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.50C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.70C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.95C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.100 [-0.100, -0.100] | **YES (Dominated)** |
| preWarmed | 2.00 | 1.20C | 0.05C/R | VACUOUS (No static $\le$) | None | N/A | NO |
| preWarmed | 4.00 | 0.30C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.50C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.70C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.95C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.100 [-0.100, -0.100] | **YES (Dominated)** |
| preWarmed | 4.00 | 1.20C | 0.05C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.150 [-0.150, -0.150] | **YES (Dominated)** |
| :---: | :---: | :---: | :---: | :---: | :--- | :---: | :---: |
| *Window $W = 0.10C/R$* | | | | | | | |
| coldStart | 1.10 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.25 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.25 | 0.50C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.008 [+0.008, +0.008] | **YES (Dominated)** |
| coldStart | 1.50 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.50 | 0.50C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.037 [+0.037, +0.037] | **YES (Dominated)** |
| coldStart | 2.00 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 2.00 | 0.50C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.072 [+0.072, +0.072] | **YES (Dominated)** |
| coldStart | 2.00 | 0.70C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.230 [+0.230, +0.230] | **YES (Dominated)** |
| coldStart | 4.00 | 0.30C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.047 [+0.047, +0.047] | **YES (Dominated)** |
| coldStart | 4.00 | 0.50C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.072 [+0.072, +0.072] | **YES (Dominated)** |
| coldStart | 4.00 | 0.70C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.272 [+0.272, +0.272] | **YES (Dominated)** |
| coldStart | 4.00 | 0.95C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.422 [+0.422, +0.422] | **YES (Dominated)** |
| coldStart | 4.00 | 1.20C | 0.10C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.402 [+0.402, +0.402] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.50C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.25 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.25 | 0.50C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.50C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.70C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.50C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.70C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.95C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.100 [-0.100, -0.100] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.30C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.50C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.70C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.95C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.100 [-0.100, -0.100] | **YES (Dominated)** |
| preWarmed | 4.00 | 1.20C | 0.10C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.150 [-0.150, -0.150] | **YES (Dominated)** |
| :---: | :---: | :---: | :---: | :---: | :--- | :---: | :---: |
| *Window $W = 0.25C/R$* | | | | | | | |
| coldStart | 1.10 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.25 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.25 | 0.50C | 0.25C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.008 [+0.008, +0.008] | **YES (Dominated)** |
| coldStart | 1.50 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 1.50 | 0.50C | 0.25C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.037 [+0.037, +0.037] | **YES (Dominated)** |
| coldStart | 2.00 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| coldStart | 2.00 | 0.50C | 0.25C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.155 [+0.155, +0.155] | **YES (Dominated)** |
| coldStart | 2.00 | 0.70C | 0.25C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.230 [+0.230, +0.230] | **YES (Dominated)** |
| coldStart | 4.00 | 0.30C | 0.25C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.397 [+0.397, +0.397] | **YES (Dominated)** |
| coldStart | 4.00 | 0.50C | 0.25C/R | SUPPORTED | DualBucket(B=1.00C,P=1.25R) | +0.467 [+0.467, +0.467] | **YES (Dominated)** |
| coldStart | 4.00 | 0.70C | 0.25C/R | SUPPORTED | DualBucket(B=1.00C,P=1.25R) | +0.498 [+0.498, +0.498] | **YES (Dominated)** |
| coldStart | 4.00 | 0.95C | 0.25C/R | SUPPORTED | DualBucket(B=1.00C,P=1.25R) | +0.472 [+0.472, +0.472] | **YES (Dominated)** |
| coldStart | 4.00 | 1.20C | 0.25C/R | SUPPORTED | DualBucket(B=0.85C,P=4.00R) | +0.402 [+0.402, +0.402] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.10 | 0.50C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.25 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.25 | 0.50C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.50C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 1.50 | 0.70C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.50C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.70C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 2.00 | 0.95C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.100 [-0.100, -0.100] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.30C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.50C | 0.25C/R | **REFUTED** | DualBucket(B=1.00C,P=1.67R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.70C | 0.25C/R | **REFUTED** | DualBucket(B=1.00C,P=1.67R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 0.95C | 0.25C/R | **REFUTED** | DualBucket(B=1.00C,P=1.67R) | +0.000 [+0.000, +0.000] | **YES (Dominated)** |
| preWarmed | 4.00 | 1.20C | 0.25C/R | **REFUTED** | DualBucket(B=0.85C,P=4.00R) | -0.150 [-0.150, -0.150] | **YES (Dominated)** |

---

## 4. Conclusion
Across all 5 overload levels $k \in \{1.10, 1.25, 1.50, 2.00, 4.00\}$, DREB burst-matched does **not** have strictly lower peak short-window admitted load than static policies that achieve equal or lower admitted rate and FRR. In particular, for bursts $\le 0.30C$, static dual buckets match DREB's peak load identically, and in pre-warmed scenarios at $k \ge 2.00$, static dual buckets admit less peak load than DREB. H10 is therefore decisively **REFUTED**.

---

## 5. Experiment E10 Steady-State Check & Hypothesis H10b Evaluation ($t \ge 5\,\text{s}$)

### Hypothesis H10b (Pre-Registered Specification)
> **Hypothesis Statement:** *restricting the peak short-window admitted load to windows that start at t >= 5 s (after the initial full-bucket burst), DREB burst-matched has strictly lower peak load than every static policy that is not worse on both admitted rate and bursty-legit FRR, at k in {1.1, 1.25, 1.5, 2, 4}.*

#### Verdict (Overall & Per Initial State):
- **Overall Verdict:** **REFUTED** (evaluated with paired 95% bootstrap confidence intervals over seeds 2000–2029 without tolerance margins).
- **`coldStart` Verdict:** **REFUTED**.
  - *Reason:* Across all 15 evaluated (abuser burst $\times$ window $W$) cells, DREB has **0 strictly lower cells**, **TIES identically** in every cell where a non-worse static policy exists ($\Delta \text{PeakLoad} = +0.000C \ [+0.000, +0.000]$), and is vacuous otherwise. Because H10b requires *strictly lower* peak load than every non-worse static policy, these ties refute H10b.
- **`preWarmed` Verdict:** **REFUTED**.
  - *Reason:* In all non-vacuous cells, DREB **TIES identically** with non-worse static dual buckets ($\Delta \text{PeakLoad} = +0.000C \ [+0.000, +0.000]$), with 0 cells having strictly lower peak load.

---

### Warmup Exclusion Survival & First-Burst Attribution:
1. **Does the E10 advantage survive the warmup exclusion?**
   **No, the E10 advantage does not survive warmup exclusion in either initial state.** In `coldStart`, the 1-to-4 cell advantages per slice observed in full duration ($t \in [0, 60\,\text{s}]$) drop to exactly 0 cells under steady state ($t \ge 5\,\text{s}$). In `preWarmed`, all cells likewise become exact ties.
2. **How much of the advantage is attributable to the first burst?**
   **100% of DREB's peak load advantage over non-worse static policies is attributable to the first burst ($t < 5\,\text{s}$)**.
3. **Mechanistic Cause:**
   - In the initial transient ($t < 5\,\text{s}$), a cold-started DREB limiter caps abuser admission to its burst bucket capacity ($B_{\max} \approx 0.428C$). In contrast, static policies such as `DualBucket(B=0.85C, P=4.00R)` or `DualBucket(B=1.00C, P=1.25R)` allow an initial burst of $0.85C$ or $1.00C$, producing an apparent short-window load advantage for DREB of up to $+0.422C$.
   - Once continuous overload ($k \ge 1.10$) depletes the initial bucket, both DREB and static dual buckets are throttled down to the sustained refill rate $R = 1.00C/\tau$. Over any post-warmup short window $W \in \{0.05, 0.10, 0.25\} C/R$, both policies admit tokens strictly at the rate of refill ($R \cdot W$). Consequently, their steady-state peak short-window admitted loads are identical down to machine precision ($\Delta \text{PeakLoad} = +0.000C \ [+0.000, +0.000]$).

---

### Table 10.1b: Steady-State Summary Breakdown ($t \ge 5\,\text{s}$): Initial State $\times$ Abuser Burst $\times$ Window $W$
*Caption: Count of overload $k \in \{1.10, 1.25, 1.50, 2.00, 4.00\}$ cells where DREB has strictly lower peak load, ties, strictly higher, or vacuous (no non-worse static candidate) in steady-state ($t \ge 5\,\text{s}$). Commit: `416acf4` (Regenerated).*

| Initial State | Abuser Burst | Window W | DREB Strictly Lower (k cells) | Ties (k cells) | DREB Strictly Higher (k cells) | Vacuous (No Static $\le$) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| coldStart  |  0.30C |  0.05C/R |  0 |  5 |  0 |  0 |
| coldStart  |  0.30C |  0.10C/R |  0 |  5 |  0 |  0 |
| coldStart  |  0.30C |  0.25C/R |  0 |  5 |  0 |  0 |
| coldStart  |  0.50C |  0.05C/R |  0 |  4 |  0 |  1 |
| coldStart  |  0.50C |  0.10C/R |  0 |  4 |  0 |  1 |
| coldStart  |  0.50C |  0.25C/R |  0 |  4 |  0 |  1 |
| coldStart  |  0.70C |  0.05C/R |  0 |  2 |  0 |  3 |
| coldStart  |  0.70C |  0.10C/R |  0 |  2 |  0 |  3 |
| coldStart  |  0.70C |  0.25C/R |  0 |  2 |  0 |  3 |
| coldStart  |  0.95C |  0.05C/R |  0 |  1 |  0 |  4 |
| coldStart  |  0.95C |  0.10C/R |  0 |  1 |  0 |  4 |
| coldStart  |  0.95C |  0.25C/R |  0 |  1 |  0 |  4 |
| coldStart  |  1.20C |  0.05C/R |  0 |  1 |  0 |  4 |
| coldStart  |  1.20C |  0.10C/R |  0 |  1 |  0 |  4 |
| coldStart  |  1.20C |  0.25C/R |  0 |  1 |  0 |  4 |
| preWarmed  |  0.30C |  0.05C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.30C |  0.10C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.30C |  0.25C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.50C |  0.05C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.50C |  0.10C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.50C |  0.25C/R |  0 |  5 |  0 |  0 |
| preWarmed  |  0.70C |  0.05C/R |  0 |  3 |  0 |  2 |
| preWarmed  |  0.70C |  0.10C/R |  0 |  3 |  0 |  2 |
| preWarmed  |  0.70C |  0.25C/R |  0 |  3 |  0 |  2 |
| preWarmed  |  0.95C |  0.05C/R |  0 |  2 |  0 |  3 |
| preWarmed  |  0.95C |  0.10C/R |  0 |  2 |  0 |  3 |
| preWarmed  |  0.95C |  0.25C/R |  0 |  2 |  0 |  3 |
| preWarmed  |  1.20C |  0.05C/R |  0 |  1 |  0 |  4 |
| preWarmed  |  1.20C |  0.10C/R |  0 |  1 |  0 |  4 |
| preWarmed  |  1.20C |  0.25C/R |  0 |  1 |  0 |  4 |

---

### Table 10.2b: Steady-State Detailed ($t \ge 5\,\text{s}$) for coldStart at Bursts $\ge 0.70C$ Across Overload $k$
*Caption: Seeds: 2000–2029 (N=30), Capacity C=1000, Initial State: coldStart, Windows evaluated starting at $t \ge 5\,\text{s}$. Commit: `416acf4` (Regenerated).*

| Window W | Overload k | Burst Size | DREB Peak Load | Best Non-Worse Static Policy | Static Peak Load | Paired $\Delta$ Peak [95% CI] | Verdict |
| :---: | :---: | :---: | :---: | :--- | :---: | :---: | :---: |
| 0.05C/R | 1.10 | 0.70C | 0.598C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.25 | 0.70C | 0.564C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.50 | 0.70C | 0.522C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 2.00 | 0.70C | 0.350C | DualBucket(B=0.85C,P=4.00R) | 0.350C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.05C/R | 4.00 | 0.70C | 0.175C | DualBucket(B=0.85C,P=4.00R) | 0.175C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.05C/R | 1.10 | 0.95C | 0.699C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.25 | 0.95C | 0.653C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.50 | 0.95C | 0.597C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 2.00 | 0.95C | 0.526C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 4.00 | 0.95C | 0.238C | DualBucket(B=0.85C,P=4.00R) | 0.238C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.05C/R | 1.10 | 1.20C | 0.800C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.25 | 1.20C | 0.741C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 1.50 | 1.20C | 0.670C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 2.00 | 1.20C | 0.581C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.05C/R | 4.00 | 1.20C | 0.300C | DualBucket(B=0.85C,P=4.00R) | 0.300C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.10C/R | 1.10 | 0.70C | 0.598C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.25 | 0.70C | 0.564C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.50 | 0.70C | 0.522C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 2.00 | 0.70C | 0.350C | DualBucket(B=0.85C,P=4.00R) | 0.350C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.10C/R | 4.00 | 0.70C | 0.175C | DualBucket(B=0.85C,P=4.00R) | 0.175C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.10C/R | 1.10 | 0.95C | 0.699C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.25 | 0.95C | 0.653C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.50 | 0.95C | 0.597C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 2.00 | 0.95C | 0.526C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 4.00 | 0.95C | 0.238C | DualBucket(B=0.85C,P=4.00R) | 0.238C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.10C/R | 1.10 | 1.20C | 0.800C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.25 | 1.20C | 0.741C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 1.50 | 1.20C | 0.670C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 2.00 | 1.20C | 0.581C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.10C/R | 4.00 | 1.20C | 0.300C | DualBucket(B=0.85C,P=4.00R) | 0.300C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.25C/R | 1.10 | 0.70C | 0.598C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.25 | 0.70C | 0.564C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.50 | 0.70C | 0.522C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 2.00 | 0.70C | 0.350C | DualBucket(B=0.85C,P=4.00R) | 0.350C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.25C/R | 4.00 | 0.70C | 0.350C | DualBucket(B=0.85C,P=4.00R) | 0.350C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.25C/R | 1.10 | 0.95C | 0.699C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.25 | 0.95C | 0.653C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.50 | 0.95C | 0.597C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 2.00 | 0.95C | 0.526C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 4.00 | 0.95C | 0.476C | DualBucket(B=0.85C,P=4.00R) | 0.476C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |
| 0.25C/R | 1.10 | 1.20C | 0.800C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.25 | 1.20C | 0.741C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 1.50 | 1.20C | 0.670C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 2.00 | 1.20C | 0.581C | None (Vacuous) | N/A | N/A | Vacuous (Non-Dominated) |
| 0.25C/R | 4.00 | 1.20C | 0.300C | DualBucket(B=0.85C,P=4.00R) | 0.300C | +0.000C [+0.000, +0.000] | REFUTED (Tied) |

---

### Table 10.3: First 5 Seconds vs Steady-State Peak Load Comparison (First Burst Attribution)
*Caption: Comparison of DREB peak short-window load against the best non-worse static policy during initial transient ($t < 5\,\text{s}$) vs steady-state ($t \ge 5\,\text{s}$). Illustrates that 100% of DREB's peak load suppression advantage occurs during the first burst.*

| InitState | Overload k | Burst Size | Window W | DREB First 5s | DREB Steady | DREB Drop | Static First 5s | Static Steady | Static Drop | Verdict (Steady) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| coldStart | 1.10 | 0.30C | 0.05C/R | 0.300C | 0.300C | 0.000C | 0.300C | 0.300C | 0.000C | Tied |
| coldStart | 1.25 | 0.30C | 0.05C/R | 0.300C | 0.240C | 0.060C | 0.300C | 0.240C | 0.060C | Tied |
| coldStart | 1.50 | 0.30C | 0.05C/R | 0.300C | 0.200C | 0.100C | 0.300C | 0.200C | 0.100C | Tied |
| coldStart | 2.00 | 0.30C | 0.05C/R | 0.300C | 0.150C | 0.150C | 0.300C | 0.150C | 0.150C | Tied |
| coldStart | 4.00 | 0.30C | 0.05C/R | 0.300C | 0.075C | 0.225C | 0.300C | 0.075C | 0.225C | Tied |
| coldStart | 1.25 | 0.50C | 0.05C/R | 0.492C | 0.400C | 0.092C | 0.500C | 0.400C | 0.100C | Tied |
| coldStart | 1.50 | 0.50C | 0.05C/R | 0.463C | 0.334C | 0.129C | 0.500C | 0.334C | 0.166C | Tied |
| coldStart | 2.00 | 0.50C | 0.05C/R | 0.428C | 0.250C | 0.178C | 0.500C | 0.250C | 0.250C | Tied |
| coldStart | 4.00 | 0.50C | 0.05C/R | 0.428C | 0.125C | 0.303C | 0.500C | 0.125C | 0.375C | Tied |
| coldStart | 2.00 | 0.70C | 0.05C/R | 0.470C | 0.350C | 0.120C | 0.700C | 0.350C | 0.350C | Tied |
| coldStart | 4.00 | 0.70C | 0.05C/R | 0.428C | 0.175C | 0.253C | 0.700C | 0.175C | 0.525C | Tied |
| coldStart | 4.00 | 0.95C | 0.05C/R | 0.428C | 0.238C | 0.190C | 0.850C | 0.238C | 0.612C | Tied |
| coldStart | 4.00 | 1.20C | 0.05C/R | 0.448C | 0.300C | 0.148C | 0.850C | 0.300C | 0.550C | Tied |
| preWarmed | 1.10 | 0.30C | 0.05C/R | 0.300C | 0.300C | 0.000C | 0.300C | 0.300C | 0.000C | Tied |
| preWarmed | 1.25 | 0.30C | 0.05C/R | 0.300C | 0.240C | 0.060C | 0.300C | 0.240C | 0.060C | Tied |
| preWarmed | 1.50 | 0.30C | 0.05C/R | 0.300C | 0.200C | 0.100C | 0.300C | 0.200C | 0.100C | Tied |
| preWarmed | 2.00 | 0.30C | 0.05C/R | 0.300C | 0.150C | 0.150C | 0.300C | 0.150C | 0.150C | Tied |
| preWarmed | 4.00 | 0.30C | 0.05C/R | 0.300C | 0.075C | 0.225C | 0.300C | 0.075C | 0.225C | Tied |
| preWarmed | 1.50 | 0.70C | 0.05C/R | 0.700C | 0.467C | 0.233C | 0.700C | 0.467C | 0.233C | Tied |
| preWarmed | 2.00 | 0.70C | 0.05C/R | 0.700C | 0.350C | 0.350C | 0.700C | 0.350C | 0.350C | Tied |
| preWarmed | 4.00 | 0.70C | 0.05C/R | 0.700C | 0.175C | 0.525C | 0.700C | 0.175C | 0.525C | Tied |
| preWarmed | 2.00 | 0.95C | 0.05C/R | 0.950C | 0.475C | 0.475C | 0.850C | 0.475C | 0.375C | Tied |
| preWarmed | 4.00 | 0.95C | 0.05C/R | 0.950C | 0.238C | 0.712C | 0.850C | 0.238C | 0.612C | Tied |
| preWarmed | 4.00 | 1.20C | 0.05C/R | 1.000C | 0.300C | 0.700C | 0.850C | 0.300C | 0.550C | Tied |
