# Experiment E1: Idle-Then-Burst Sweep

## 1. Overview
Experiment E1 measures the instantaneous unit-cost burst acceptance following varying idle durations $D \in \{0, 0.25, 0.5, 1.0, 2.0\}\tau$ across burst demand ratios $B/C \in \{0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0\}$, nominal capacities $C \in \{100, 1000, 50000\}$ ($R = C$), and three initial states:
- `coldStart`: Client starts at $t=0$ with full burst bucket and empty idle credit pool ($\text{idleCredit} = 0$).
- `preWarmed`: Client starts with full idle credit pool ($\text{idleCredit} = \text{idle\_cap}$).
- `drainedFirst`: Client completely exhausts token and idle pools at $t=0$, idles for $D$, and then bursts.

## 2. Determinism and Sample Size
**Notice on Determinism:**
Experiment E1 is strictly deterministic. All requests in a burst are delivered at the exact same logical timestamp ($t = D$), with unit cost ($1$), executed sequentially against stateful rate-limiting algorithms governed by deterministic clock updates and token-bucket mathematics.

Because there is zero stochasticity or arrival jitter in Experiment E1:
- Every trial for a given cell produces identical accepted counts across all seeds.
- The sample standard deviation is $0.0$, and the 95% bootstrap confidence intervals are exact point intervals (zero-width: $\text{CI}_{\text{low}} = \text{CI}_{\text{high}} = \text{mean}$).
- Evaluating $N = 1$ seed would mathematically suffice to obtain identical results; executing $N = 30$ seeds confirms variance zero across independent executions.

## 3. Per-Variant Baseline Matching (TB-small)
To ensure apples-to-apples baseline comparisons, each DREB variant is evaluated against its own per-variant `TB-small`:
- For `DREB (proportional)`: $B_{\max} = \lfloor C / 3 \rfloor$ ($33$ at $C=100$, $333$ at $C=1000$, $16666$ at $C=50000$).
- For `DREB (burst-matched)`: $B_{\max} = \lfloor 3C / 7 \rfloor$ ($42$ at $C=100$, $428$ at $C=1000$, $21428$ at $C=50000$).
- For `DREB (v2-adaptive-base1.5)`: $B_{\max} = \lfloor 3C / 7 \rfloor$.
- For `DREB (v2-adaptive-base1.0)`: $B_{\max} = \lfloor 3C / 7 \rfloor$.
- `Token Bucket (TB-large)` has capacity $C$ and refill rate $R$ across all comparisons.

## 4. Key Artifacts
- `e1_raw_results.csv`: All 94,500 individual trial executions with full provenance (git commit, library commit, dirty flag, seed).
- `e1_summary_table.csv`: Aggregated means, SDs, 95% bootstrap CIs, and paired differences vs per-variant `TB-small` and `TB-large`.
- `graphs/`: Acceptance heatmaps for each DREB variant as a function of idle duration $D$ and burst demand ratio $B/C$.
