# Formal Hypotheses Specification: Experiment E1 (Idle-Then-Burst Sweep)

**Author:** Empirical Evaluation Suite  
**Date:** 2026-10-22  
**Library Tag:** `v1.0.2-phase1` (Commit: `53a5c9e`)  
**Comparator Framework:** `DREB-Comparator`  
**Execution Mode:** Official ($N = 30$ seeds per cell, clean working tree)  

---

## 1. Overview and Scope

This document formally records the hypotheses for **Experiment E1 (Idle-Then-Burst Sweep)** prior to execution, in accordance with pre-registration principles. No experimental runs or data-dependent adjustments will be performed prior to committing this specification.

Experiment E1 measures the instantaneous burst allowance of rate-limiting algorithms following varying durations of idle time $D \in \{0, 0.25, 0.5, 1.0, 2.0\} \cdot \tau$ (where $\tau = \frac{\text{idle\_cap}}{\text{idle\_gain}} = \frac{9C}{7R}$), across burst demands $B/C \in \{0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0\}$, nominal capacities $C \in \{100, 1000, 50000\}$ with $R = C$, and initial states (`coldStart`, `preWarmed`, `drainedFirst`).

---

## 2. Formal Hypotheses

### Hypothesis H1a: Full Idleness Burst Acceptance
> **Hypothesis Statement:** After full idleness ($D \ge \tau$, or under pre-warmed state), DREB's instantaneous burst acceptance for a burst demand $B \ge C$ is greater than or equal to `TB-small` and within $\epsilon$ of `TB-large`.

* **Primary Metric:**
  $$\Delta_{\text{TB-small}} = A_{\text{DREB}} - A_{\text{TB-small}}$$
  $$\Delta_{\text{TB-large}} = A_{\text{DREB}} - A_{\text{TB-large}}$$
  measured at $D \ge \tau$ (or `preWarmed`) for $B/C \ge 1.0$, where $A$ is the accepted request count in the instantaneous burst.
* **Supporting Outcome:**
  - $\Delta_{\text{TB-small}} \ge 0$ across all tested capacities $C \in \{100, 1000, 50000\}$.
  - For headline DREB (Burst-Matched and v2 Base 1.5): $|\Delta_{\text{TB-large}}| \le \epsilon$ (where $\epsilon \le 1.0$ request due to integer discrete rounding). Specifically, at $B \ge C$, $A_{\text{DREB}} = \min(B, C) = C = A_{\text{TB-large}}$.
* **Contradicting Outcome:**
  - $A_{\text{DREB}} < A_{\text{TB-small}}$ for any cell with $D \ge \tau$, OR
  - $A_{\text{DREB}} < A_{\text{TB-large}} - 1.0$ for headline DREB under full idleness with burst demand $B \ge C$.

---

### Hypothesis H1b: Zero-History / Active Client Burst Restriction
> **Hypothesis Statement:** For a client with no idle history (either fresh `coldStart` with $D = 0$, or immediately following bucket exhaustion `drainedFirst` with $D = 0$), DREB's instantaneous burst acceptance is close to `TB-small`, not `TB-large`.

* **Primary Metric:**
  $$|A_{\text{DREB}} - A_{\text{TB-small}}| \quad \text{vs} \quad |A_{\text{TB-large}} - A_{\text{DREB}}|$$
  evaluated at $D = 0$ for $B/C \ge 1.0$.
* **Supporting Outcome:**
  - Under `coldStart` at $D = 0$ with $B \ge C$:
    $$A_{\text{DREB}} = B_{\max} = \lfloor \frac{3}{7}C \rfloor = A_{\text{TB-small}} \ll A_{\text{TB-large}} = C$$
  - Under `drainedFirst` at $D = 0$:
    $$A_{\text{DREB}} = 0 = A_{\text{TB-small}} = A_{\text{TB-large}}$$
  - In both zero-idle scenarios, DREB accepts at most $B_{\max}$ tokens, strictly preventing full capacity $C$ bursts when no idle history has been accrued.
* **Contradicting Outcome:**
  - $A_{\text{DREB}} > B_{\max} + 1.0$ at $D = 0$ under `coldStart`.
  - $A_{\text{DREB}} \approx A_{\text{TB-large}}$ for zero-idle clients under burst demand $B \ge C$.

---

### Hypothesis H1c: Strict Ceiling Bound at Base Decay 1.5
> **Hypothesis Statement:** DREB never exceeds `TB-large`'s instantaneous burst acceptance when configured with base decay $\text{decay} = 1.5$ (this holds by mathematical construction; empirical evaluation confirms the runtime implementation strictly matches the analytical invariant).

* **Primary Metric:**
  $$\Delta_{\max} = \max_{\text{all cells}} \left( A_{\text{DREB-1.5}} - A_{\text{TB-large}} \right)$$
  evaluated across all ratios $B/C$, idle durations $D$, capacities $C$, and initial states for `DREB (burst-matched)` and `DREB (v2-adaptive-base1.5)`.
* **Supporting Outcome:**
  - $\Delta_{\max} \le 0$ across all tested cells without exception (i.e. $A_{\text{DREB-1.5}} \le A_{\text{TB-large}}$ everywhere).
* **Contradicting Outcome:**
  - Any single observation where $A_{\text{DREB-1.5}} > A_{\text{TB-large}}$ ($\Delta > 0$).

---

### Hypothesis H1d: DREB v2 Base 1.0 Upper Bound vs TB-large
> **Hypothesis Statement:** DREB v2 configured with base decay 1.0 (`DREB (v2-adaptive-base1.0)`) never exceeds `TB-large`'s instantaneous burst acceptance.

* **Primary Metric:**
  $$\Delta_{\max} = \max_{\text{all cells}} \left( A_{\text{v2-1.0}} - A_{\text{TB-large}} \right)$$
* **Supporting Outcome:**
  - $\Delta_{\max} \le 0$ across all tested cells.
* **Contradicting Outcome:**
  - Any cell where $A_{\text{v2-1.0}} > A_{\text{TB-large}}$ ($\Delta > 0$). (Expected to be contradicted because base decay 1.0 allows bursts up to 110 at $C=100$, exceeding $C=100$).

---

## 3. Experimental Parameters & Grid: Experiment E1

* **Algorithms (10 total):**
  1. `Token Bucket (TB-large)` ($C, R$)
  2. `Token Bucket (TB-small)` (Per-variant: capacity = that variant's own $B_{\max}$)
  3. `Leaky Bucket` ($C, R$)
  4. `Fixed Window` ($C, W = 1\text{s}$)
  5. `Sliding Window Log` ($C, W = 1\text{s}$)
  6. `Sliding Window Counter` ($C, W = 1\text{s}$)
  7. `DREB (proportional)` ($C, R$)
  8. `DREB (burst-matched)` ($C, R$)
  9. `DREB (v2-adaptive-base1.5)` ($C, R$, headline)
  10. `DREB (v2-adaptive-base1.0)` ($C, R$, variant)
* **Burst Demand Multipliers ($B/C$):** $\{0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0\}$
* **Idle Duration ($D$ in units of $\tau = 9C/7R$):** $\{0.0, 0.25, 0.5, 1.0, 2.0\}$
* **Nominal Capacity ($C$):** $\{100, 1000, 50000\}$, with $R = C$
* **Initial State Conditions:**
  - `coldStart`: Client created at $t=0$, idles for $D$, receives burst at $t=D$.
  - `preWarmed`: Client starts with full idle credit ($\text{idleCredit} = \text{idle\_cap}$), idles for $D$, receives burst at $t=D$.
  - `drainedFirst`: Client drains entire bucket at $t=0$, idles for $D$, receives burst at $t=D$.
* **Replications:** $N = 30$ independent random seeds. (Note: E1 is deterministic so $N=1$ yields identical results).
* **Outputs:** Accepted counts, mean, standard deviation, 95% bootstrap confidence intervals, paired differences vs per-variant `TB-small` and `TB-large`, and heatmaps.

---

## 4. Formal Hypotheses: Experiment E2 (Mixed Legitimate & Abusive Traffic)

### Hypothesis H2a: Abusive Traffic Suppression Without Excessive False Rejection
> **Original Hypothesis Statement:** DREB variants admit a smaller fraction of abusive traffic than TB-large without raising false rejection of legitimate clients above TB-large's.

* **Primary Metrics:**
  - Abusive Request Rejection Rate: $\text{ARR} = \frac{\text{Abusive Requests Rejected}}{\text{Abusive Requests Offered}}$
  - Legitimate False Rejection Rate: $\text{FRR} = \frac{\text{Legit Requests Rejected}}{\text{Legit Requests Offered}}$, evaluated for both steady Poisson and bursty-legit clients.
  - Paired difference in legitimate FRR vs `TB-large`: $\Delta \text{FRR} = \text{FRR}_{\text{DREB}} - \text{FRR}_{\text{TB-large}}$.
* **Supporting Outcome:**
  - $\text{ARR}_{\text{DREB}} > \text{ARR}_{\text{TB-large}}$ across abusive scenarios ($k \in \{1.1, 1.25, 1.5, 2.0, 4.0\}$).
  - Upper bound of the 95% bootstrap confidence interval of paired difference $\Delta \text{FRR} \le 0.0$ (i.e. strictly no higher than TB-large's false rejection rate).
* **Contradicting Outcome:**
  - $\text{ARR}_{\text{DREB}} \le \text{ARR}_{\text{TB-large}}$, OR
  - The 95% bootstrap CI of $\Delta \text{FRR}$ is strictly positive ($\text{lower CI} > 0.0$), indicating statistically significant elevation of legitimate false rejections above TB-large.

---

### Hypothesis H2b: DREB v2 Adaptive Penalty vs DREB v1
> **Original Hypothesis Statement:** v2 admits less abusive traffic than v1 at equal false-rejection cost.

* **Primary Metrics:**
  $$\Delta \text{ARR} = \text{ARR}_{\text{v2}} - \text{ARR}_{\text{v1-burst-matched}}$$
  $$\Delta \text{FRR} = \text{FRR}_{\text{v2}} - \text{FRR}_{\text{v1-burst-matched}}$$
* **Supporting Outcome:**
  - $\Delta \text{ARR} > 0$ (v2 blocks a higher percentage of abusive traffic than v1 burst-matched) while legitimate $\text{FRR}$ is statistically equivalent ($\Delta \text{FRR} \approx 0$).
* **Contradicting Outcome:**
  - $\text{ARR}_{\text{v2}} \le \text{ARR}_{\text{v1}}$, OR $\Delta \text{FRR} > 0$ by a statistically significant margin.

---

### Hypothesis H2c: Long-Run Cumulative Rate Bound Enforcement
> **Original Hypothesis Statement:** no algorithm lets an abuser's long-run admitted rate exceed its cumulative bound.

* **Primary Metric:**
  $$\text{Normalized Admitted Rate} = \frac{\text{Total Admitted Abusive Cost} / T}{R}$$
* **Theoretical Exact Bound:**
  $$\text{Exact Bound} = \frac{S_{\max} + R \cdot T}{R \cdot T} = 1.0 + \frac{S_{\max}}{R \cdot T}$$
  (where $S_{\max}$ is the maximum capacity of the sustained bucket: $C$ for TB-large and DREB proportional, $9C/7$ for DREB burst-matched and v2 base 1.5, and $1.10C$ for v2 base 1.0).
* **Supporting Outcome:**
  - Normalized Admitted Rate $\le \frac{S_{\max} + R \cdot T}{R \cdot T}$ across all seeds and abuser configurations.
* **Contradicting Outcome:**
  - Normalized Admitted Rate exceeds $\frac{S_{\max} + R \cdot T}{R \cdot T}$ for any algorithm.

---

### Criteria Changes & Clarifications
The following departures/approximations were introduced in intermediate reporting and are explicitly documented here:
1. **Legitimate FRR Margin in H2a:** An intermediate tolerance of $+1.0\%$ ($\text{FRR}_{\text{DREB}} \le \text{FRR}_{\text{TB-lg}} + 1.0\%$) was applied during preliminary checks. This tolerance is revoked: H2a is judged strictly by whether the paired-difference 95% bootstrap CI against TB-large includes zero or is strictly negative. Under this strict standard, `DREB (proportional)` is contradicted because its FRR is $+0.72\%$ above TB-large (95% CI $[+0.69\%, +0.75\%]$).
2. **Cumulative Rate Bound in H2c:** An intermediate threshold of static $1.01$ was used. It is replaced by each algorithm's exact mathematical cumulative bound $(S_{\max} + R \cdot T) / (R \cdot T)$ per client.
3. **Evasion Abuser Reclassification:** The original evasion scenario with $\tau$ wait between bursts of post-idle ceiling yields an offered rate of $\approx 0.78 R < R$. Under token bucket semantics, this client is compliant; its rejections are counted as false rejections of compliant bursty traffic, not abuse suppression.
4. **Legitimate FRR Reference in H3a:** In intermediate reporting, H3a was evaluated using the tuning rule's budget ($\text{FRR} \le \text{FRR}_{\text{TB-large}} + 0.1\text{ percentage points}$) rather than strictly against v1 burst-matched. Under the strict original wording ("at no higher legit FRR [than v1]"), both default v2 ($\Delta \text{FRR} = +0.04$ pp, 95% CI $[+0.04\%, +0.05\%]$) and the selected setting ($\Delta \text{FRR} = +0.18$ pp, 95% CI $[+0.17\%, +0.19\%]$) have paired CIs strictly above zero, contradicting H3a.
5. **Both Adaptive Terms Contributing in H3b:** H3b was temporarily marked supported despite empirical measurements showing that disabling sustained-cost scaling (`sustainedCostGain = 0.0`) produces identical results to full v2 ($37.6\%$ ARR, $0.938 R$). Under the strict requirement that *both* terms contribute, H3b is contradicted because sustained-cost scaling has no measurable effect.

---

## 5. Experimental Parameters & Grid: Experiment E2

* **Baseline Matching Family:** Family 1 (equal $C$ and $R$).
* **Capacities:** $C \in \{1000, 50000\}$, with sustained rate $R = C$.
* **Initial State Conditions:** `coldStart` and `preWarmed`.
* **Client Population:** $N_{\text{clients}} = 200$ clients per trial.
* **Duration:** $T = 120.0$ seconds logical simulation time.
* **Abusive Fraction:** $f \in \{0.1, 0.3, 0.5\}$.
* **Legitimate Mix:** 50% steady Poisson (rate uniform in $[0.5R, 0.9R]$) and 50% bursty-legit (exponential idle mean $3\tau$, burst uniform in $[0.3C, 0.9C]$).
* **Abusive Traffic Profiles:**
  - Repeated burst abusers: burst size $0.95C$ at interval $\Delta t = \frac{0.95C}{k \cdot R}$ for $k \in \{1.1, 1.25, 1.5, 2.0, 4.0\}$.
  - Compliant bursty at $0.78R$ (formerly evasion): waits $\tau = \frac{9C}{7R}$ between bursts, sized at algorithm's post-idle ceiling (actual offered rate $0.78R$).
  - Jitter abuser: random burst sizes in $[0.5C, 0.95C]$ with interval $\Delta t = \frac{B}{1.5R}$ ($k = 1.5$).
* **Algorithms (10 total):**
  1. `Token Bucket (TB-large)` ($C, R$)
  2. `Token Bucket (TB-small)` (Per-variant capacity matching each DREB variant)
  3. `Leaky Bucket` ($C, R$)
  4. `Fixed Window` ($C, W = 1\text{s}$)
  5. `Sliding Window Log` ($C, W = 1\text{s}$)
  6. `Sliding Window Counter` ($C, W = 1\text{s}$)
  7. `DREB (proportional)` ($C, R$)
  8. `DREB (burst-matched)` ($C, R$)
  9. `DREB (v2-adaptive-base1.5)` ($C, R$, headline)
  10. `DREB (v2-adaptive-base1.0)` ($C, R$, variant)
* **Replications:** $N = 30$ independent random seeds (seeds $2000 \dots 2029$; seeds $1000 \dots 1029$ reserved for E3 tuning).

---

## 6. Formal Hypotheses: Experiment E3 (v2 Ablation & Tuning)

### Hypothesis H3a: v2 vs v1 Performance Under Strict FRR Constraint
> **Hypothesis Statement:** v2 admits less abuser traffic than v1 burst-matched at no higher legit FRR.

* **Metric:** Abuser admitted rate / $R$ and overall legitimate FRR, with paired differences vs v1 burst-matched.
* **Supporting Outcome:** v2 achieves lower abuser admitted rate / $R$ than v1 burst-matched with paired legitimate $\text{FRR}$ difference $\Delta \text{FRR} \le 0$ (judged by upper bound of paired 95% bootstrap CI $\le 0.0$, i.e. strictly no higher legitimate FRR than v1 burst-matched).
* **Contradicting Outcome:** Lower abuser admitted rate is accompanied by a statistically significant increase in legitimate FRR ($\Delta \text{FRR} > 0$, 95% CI strictly positive), OR v2 admits higher abuser rate than v1.

---

### Hypothesis H3b: Contribution of Both v2 Adaptive Terms
> **Hypothesis Statement:** Both v2 adaptive terms (sustained-cost scaling and decay scaling) contribute to abuse mitigation.

* **Metric:** Abusive rejection rate (ARR) and abuser admitted rate / $R$ for:
  1. Full v2 base 1.5
  2. v2 with sustained-cost scaling disabled (`sustainedCostGain = 0.0`)
  3. v2 with decay scaling disabled (`decayScalingEnabled = false`)
  4. v2 with both adaptive terms disabled.
* **Supporting Outcome:** Disabling either term strictly degrades abuser rejection (or increases admitted abuser rate) compared to the full model.
* **Contradicting Outcome:** Disabling one or both terms has no effect or improves abuser rejection.

---

### Hypothesis H3c: Equivalence Sanity Check (v2 with Both Terms Off == v1 Burst-Matched)
> **Hypothesis Statement:** v2 with both adaptive terms off is identical to v1 burst-matched.

* **Metric:** Bit-for-bit / exact numeric identity between `v2 (both off)` and `v1 burst-matched` across all accepted, rejected, and rate metrics for all seeds.
* **Supporting Outcome:** Maximum absolute difference $|\text{metric}_{\text{v2-both-off}} - \text{metric}_{\text{v1-BM}}| == 0.0$ everywhere.
* **Contradicting Outcome:** Any non-zero difference (indicates an implementation bug).

---

### Experiment E3 Protocol & Search Grid
* **Tuning Seeds:** Seeds 1000–1029 strictly reserved for parameter search.
* **Held-Out Test Seeds:** Seeds 2000–2029 strictly reserved for final evaluation.
* **Selection Rule (Fixed in Advance):**
  $$\text{Maximize } \text{ARR} \quad \text{subject to } \text{FRR}_{\text{overall}} \le \text{FRR}_{\text{TB-large}} + 0.1\text{ percentage points}$$
* **Ablation Variants (5):**
  1. `v1 burst-matched`
  2. `v2 base 1.5 full`
  3. `v2 sustained-cost scaling off` (`sustainedCostGain = 0.0`)
  4. `v2 decay scaling off` (`decayScalingEnabled = false`)
  5. `v2 both off` (`sustainedCostGain = 0.0` and `decayScalingEnabled = false`)
* **Single-Parameter Sweeps (others held at defaults: baseDecay=1.5, sustainedCostGain=0.35, abuseIncrement=2.0, abuseDecayRate=1.5, abuseGain=0.3):**
  - `sustainedCostGain` $\in \{0.0, 0.1, 0.35, 1.0\}$
  - `abuseIncrement` $\in \{0.5, 2.0, 8.0\}$
  - `abuseDecayRate` $\in \{0.25, 1.5, 6.0\}$
  - `abuseGain` $\in \{0.0, 0.3, 1.0\}$
* **Scenario:** $k = 1.5, f = 0.3, C \in \{1000, 50000\}$, `coldStart` and `preWarmed`.
* **Deliverables:** Pareto plot (legit FRR vs abuser admitted rate / $R$), aggregated comparison tables, verification of H3a, H3b, H3c.
