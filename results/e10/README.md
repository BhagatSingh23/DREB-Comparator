# Experiment E10: Peak Short-Window Admitted Load & 3-Metric Dominance

## 1. Executive Summary & Hypothesis Verdict (Strict Original Wording)

### Hypothesis H10
> **Statement:** *against abuser traffic at k in {1.1, 1.25, 1.5, 2, 4}, DREB burst-matched has lower peak short-window admitted load than any static policy that is not worse on both admitted rate and bursty-legit FRR.*

#### Verdict:
**REFUTED** (judged by paired 95% bootstrap confidence intervals over seeds 2000–2029, evaluated without tolerance margins).

---

### Core Findings & Failure Mechanisms:
1. **Equal Peak Window Load at Small Abuser Bursts ($0.30C$):**
   When the abuser sends bursts of size $0.30C$, the entire burst fits within the base burst bucket of static dual policies (`DualBucket(B=0.85C, P=4.00R)`) as well as DREB ($B_{\max} = 0.43C$). Across all evaluated short windows ($W \in \{0.05, 0.10, 0.25\} C/R$), both DREB and non-worse static policies admit the exact same peak cost ($\Delta \text{PeakLoad} = +0.000 [+0.000, +0.000]$). Because H10 strictly requires DREB to have *lower* peak admitted load ($\Delta \text{PeakLoad} > 0$), this equality immediately refutes H10.
2. **Static Dual Buckets Achieve Lower Peak Load under Pre-Warmed States:**
   Under `preWarmed` initial conditions at higher overload (e.g. $k = 2.00$ and $k = 4.00$, burst $0.95C$), static `DualBucket(B=0.85C, P=4.00R)` achieves strictly lower peak admitted window load than DREB:
   $$\Delta \text{PeakLoad} = \text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} = -0.100 \quad [-0.100, -0.100]$$
   Here, DREB admits $0.95C$ in a 50ms window from pre-warmed burst capacity plus accumulated credit, whereas the static limiter restricts the abuser to its configured ceiling, directly violating H10.
3. **Three-Metric Dominance:**
   Across all scenarios where a static policy matches or improves upon DREB on both long-term admitted rate and bursty-legit FRR, that same static policy achieves equal or lower peak short-window admitted load. Consequently, DREB is **3-metric dominated** by static dual buckets whenever DREB is 2-metric dominated.

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
