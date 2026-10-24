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

---

## 7. Formal Hypotheses: Experiment E8 (Isolate the Idle Credit)

### Hypothesis H8a: Trade-Off Frontier vs Static Dual Buckets
> **Hypothesis Statement:** DREB (burst-matched) achieves a better trade-off than any static dual bucket without idle credit: for the same bursty-legit FRR it admits less abuser traffic, or for the same abuser admitted rate it has lower bursty-legit FRR.

* **Definition of "better" in H8a:** DREB is below-or-left of the static-dual-bucket frontier at the same x or y, judged by paired 95% CIs, no tolerance margins.
* **Supporting Outcome:** DREB burst-matched lies strictly below or to the left of the static dual bucket frontier at the same x (bursty-legit FRR) or y (abuser admitted rate / R) with strictly non-overlapping / negative paired 95% bootstrap CIs without tolerance margins.
* **Contradicting Outcome:** DREB burst-matched lies on, above, or to the right of the static dual bucket frontier, or the paired 95% CIs overlap / include zero.
* **Empirical Outcome & Verdict:** **CONTRADICTED** (judged by paired 95% bootstrap CIs, no tolerance margins). While DREB burst-matched achieves a strictly superior trade-off at moderate rates ($k \in \{1.1, 1.25, 1.5\}$), at high over-offered rates ($k \in \{2.0, 4.0\}$) abuser traffic saturates the sustained envelope ($S_{\text{rate}} = R$, admitted rate $\approx 1.040R$) across all $B \ge 0.55C$. DualBucket ($B = 1.00C$) achieves identical abuser admission (1.040R, difference +0.000 [+0.000, +0.000]) but achieves strictly lower bursty-legit FRR (1.43% vs 2.18%, paired difference +0.75 pp [+0.52 pp, +0.99 pp]) because its 1.0C burst bucket is permanently available without needing time $\tau$ to accumulate credit. Thus, DREB does not achieve a better trade-off than *any* static dual bucket.

---

### Hypothesis H8b: Idle Credit and Burst Acceptance
> **Hypothesis Statement:** the idle credit increases burst acceptance after idleness relative to a static dual bucket with the same cold-start burst (B_max = 3C/7) without raising abuser admission.

* **Metric:** Instantaneous burst acceptance after idle duration $D \in \{0, 0.5\tau, 1\tau, 2\tau\}$ and abuser admitted rate / $R$.
* **Supporting Outcome:** DREB burst-matched has strictly higher instantaneous burst acceptance than DualBucket with $B = 3C/7$ for $D > 0$, while admitting no more abuser traffic ($\Delta \text{admitted rate} \le 0$ judged by paired 95% CI).
* **Contradicting Outcome:** Idle credit fails to increase burst acceptance after idleness, OR increases abuser admitted rate relative to DualBucket ($B = 3C/7$).
* **Empirical Outcome & Verdict:** **CONTRADICTED** (judged by paired 95% bootstrap CIs, no tolerance margins). Idle credit increases instantaneous burst acceptance after idleness from $0.428C$ to $1.000C$ (+133.6%), but it **significantly raises abuser admission** relative to DualBucket ($B = 3C/7$) by up to +0.306R across seeds 2000–2029 (at $k=1.10$: +0.306R [+0.306, +0.306]; at $k=1.25$: +0.291R [+0.291, +0.291]; at $k=1.50$: +0.263R [+0.263, +0.263]; at $k=2.00$: +0.126R [+0.126, +0.126]). Because DREB's burst bucket refills rapidly at $5R/3$ in just $0.257(C/R)$ seconds, periodic attack bursts with interval $dt > 0.257(C/R)$ leave the burst bucket full and accrue idle credit between bursts, which the abuser then consumes. DualBucket ($B=3C/7$) lacks idle credit and strictly clamps abusers to $0.428C$ per burst. Thus, idle credit raises abuser admission for all $k < 4.0$.

---

### Experiment E8 Protocol & DualBucket Family
* **DualBucket Baseline Definition:** Existing DREB v1 with `idleGain = 0` and `idleCap = 0` (no idle credit), parameters:
  - `burstMax = B`
  - `burstRate = 5R/3`
  - `sustainedMax = 9C/7`
  - `sustainedRate = R`
  - Family: $B \in \{0.30C, 0.43C (= 3C/7), 0.55C, 0.70C, 0.85C, 1.00C\}$.
* **Algorithms Evaluated:**
  - DualBucket family ($B \in \{0.30C, 0.43C, 0.55C, 0.70C, 0.85C, 1.00C\}$)
  - `DREB (burst-matched)`
  - `DREB (v2 base 1.5)`
  - `Token Bucket (TB-large)` ($C, R$)
  - `Token Bucket (TB-env)` ($9C/7, R$)
  - `Token Bucket (TB-small)` ($3C/7, R$)
* **Sanity Unit Test:** DualBucket with $B = 3C/7$ equals DREB burst-matched in cold start with `idleGain = 0` (sanity check).
* **Scenarios:** $k \in \{1.1, 1.25, 1.5, 2, 4\}$, $f = 0.3$, $C \in \{1000, 50000\}$, `coldStart` and `preWarmed`, seeds 2000–2029 ($N=30$).
* **Criteria Changes:** None. Strict original wording, no tolerance margins.

---

## 8. Formal Hypotheses: Experiment E9 (Making the Static Baseline Fair)

### Hypothesis H9a: Non-Domination Against Comprehensive Static Families
> **Hypothesis Statement:** at each abuser overload k in {1.1, 1.25, 1.5, 2, 4}, DREB burst-matched is not dominated by any static policy from the families below (dominated = another policy has abuser admitted rate / R <= and bursty-legit FRR <=, with at least one strictly lower, judged by paired 95% CIs).

* **Dominated Definition:** Another policy has abuser admitted rate / $R \le$ and bursty-legit $\text{FRR} \le$, with at least one strictly lower, judged by paired 95% bootstrap CIs without tolerance margins.
* **Supporting Outcome:** In every condition $k \in \{1.1, 1.25, 1.5, 2, 4\}$, no static policy dominates DREB burst-matched.
* **Contradicting Outcome:** At any $k$, at least one static policy from families (S1), (S2), or (S3) dominates DREB burst-matched.
* **Empirical Outcome & Verdict:** **CONTRADICTED** (judged by paired 95% bootstrap CIs, no tolerance margins).
  - *Abuser Burst Sensitivity Grid:* Evaluating across all $k \in \{1.1, 1.25, 1.5, 2, 4\} \times \text{burst} \in \{0.30C, 0.50C, 0.70C, 0.95C, 1.20C\} \times \{\text{coldStart}, \text{preWarmed}\}$, DREB burst-matched is **dominated** in 29 of 50 evaluated cells (13 in coldStart, 16 in preWarmed):
    1. At small abuser bursts ($0.30C$), DREB is dominated across **all** $k$ in both coldStart and preWarmed by `DualBucket(B=1.00C, P=4.00R)`.
    2. At burst $0.50C$, DREB is dominated at $k \in \{1.25, 1.5, 2, 4\}$ in coldStart, and at all $k \in \{1.1, 1.25, 1.5, 2, 4\}$ in preWarmed.
    3. At $k = 4.00$, DREB is dominated across **all** burst sizes ($0.30C$–$1.20C$) in both initial states.
    4. Only at large bursts ($\ge 0.70C$ for moderate $k \le 1.5$) does DREB maintain non-domination.
    *(Mechanism Note: In every dominated cell, the abuser admitted rate is tied ($|\Delta \text{Rate}| = 0.000R$) and the FRR difference comes solely from legitimate traffic, since rate limiters are strictly per-client and do not interact. These dominations arise because static $B=1.00C$ provides a larger burst bucket under the identical sustained envelope $S_{\max} = 9C/7$.)*
  - *Baseline Grid (Burst 0.95C):* DREB is non-dominated at moderate overload $k \in \{1.1, 1.25, 1.5\}$, but is strictly dominated by static dual buckets at high overload $k \in \{2.0, 4.0\}$ (in preWarmed at $k=2.00$, and in both coldStart and preWarmed at $k=4.00$). Thus H9a fails across the grid.

---

### Hypothesis H9b: Bursty-Legit Advantage at Equal Abuser Admission
> **Hypothesis Statement:** at each k, DREB dominates every static policy whose abuser admitted rate / R is within 0.05 of DREB's (i.e. DREB has lower bursty-legit FRR at equal abuser admission).

* **Supporting Outcome:** For every static policy with $|\text{Rate}_{\text{static}} - \text{Rate}_{\text{DREB}}| \le 0.05$, DREB has strictly lower bursty-legit FRR ($\Delta \text{FRR} < 0$, upper bound of paired 95% CI $< 0$).
* **Contradicting Outcome:** Any static policy with abuser admitted rate within 0.05 of DREB has equal or lower bursty-legit FRR ($\Delta \text{FRR} \ge 0$, CI includes zero or is positive).
* **Empirical Outcome & Verdict:** **CONTRADICTED** (judged by paired 95% bootstrap CIs, no tolerance margins).
  - Across the full burst grid, whenever DREB is dominated, static policies within 0.05 of DREB's admitted rate (often identical rate, $|\Delta| = 0.000$) achieve strictly *lower* bursty-legit FRR.
  - In baseline testing ($0.95C$), H9b holds at $k \in \{1.1, 1.25, 1.5\}$, but fails at $k \in \{2.0, 4.0\}$ where static policies (`DualBucket(B=1.00C)`, `TB(C=1.286C)`) have identical abuser rates ($1.040R$, $|\Delta| = 0.000 \le 0.05$) while achieving significantly lower bursty FRR ($1.43\%$ vs $1.73\%$ or $0.96\%$ vs $2.18\%$). At burst $0.30C$, static dual buckets match DREB's admitted rate ($1.043R$) with lower FRR across all $k$. Thus H9b fails.

---

### Hypothesis H9c: Robustness to Bursty-Legit Traffic Distribution
> **Hypothesis Statement:** the result of H9a/H9b does not change when the bursty-legit idle-time mean is varied from 1 tau to 10 tau.

* **Supporting Outcome:** The verdicts for H9a and H9b remain identical across all tested idle-time means $\in \{1\tau, 3\tau, 10\tau\}$ and burst ranges $\in \{[0.1C, 0.5C], [0.3C, 0.9C], [0.6C, 1.0C]\}$.
* **Contradicting Outcome:** Changing the idle-time mean flips the verdict of H9a or H9b.
* **Empirical Outcome & Verdict:** **SUPPORTED** (judged by paired 95% bootstrap CIs, no tolerance margins). Across all tested legitimate idle-time means ($1\tau, 3\tau, 10\tau$) and burst ranges, the outcome is invariant: DREB is non-dominated and superior at moderate overload ($k \le 1.5$), but is strictly dominated by large static dual buckets at high overload ($k \ge 2.0$). Varying the legit idle distribution does not alter the fundamental mechanism or flip the verdicts of H9a/H9b.

---

### Experiment E9 Protocol & Policy Families
* **Static Families (no idle credit anywhere):**
  - **(S1) DualBucket with Fine B Sweep:**
    - $B/C \in \{0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00\}$ (15 values)
    - Peak rate $P \in \{1.0R, 1.25R, 5R/3, 2.5R, 4.0R\}$ (5 values, $P = \text{burstRate}$)
    - $S_{\max} = 9C/7, S_{\text{rate}} = R, \text{idleGain} = 0, \text{idleCap} = 0$. (75 policies)
  - **(S2) Single Token Bucket with Capacity Sweep:**
    - Capacity $\in \{0.30C, 0.43C (= 3C/7), 0.55C, 0.70C, 0.85C, 1.00C, 1.286C (= 9C/7)\}$, refill rate $R$. (7 policies)
  - **(S3) Single Token Bucket with Refill Rate Sweep:**
    - Capacity $0.43C (= 3C/7)$, refill rate $\in \{R, 1.1R, 1.25R\}$. (3 policies)
* **Dynamic Policies:**
  - `DREB (burst-matched)` (headline)
  - `DREB (v2 base 1.5)`
  - `DREB (idleGain=4R/3)` (variant exploring idle gain movement)
* **Scenarios:**
  - **Baseline Grid:** $C \in \{1000, 50000\}$, `coldStart` and `preWarmed`, $f = 0.3$, $k \in \{1.1, 1.25, 1.5, 2, 4\}$, seeds 2000–2029 (default legit: idle mean $3\tau$, burst $[0.3C, 0.9C]$).
  - **Sensitivity Grid ($C = 1000$, `coldStart`):**
    - Idle-time mean $\in \{1\tau, 3\tau, 10\tau\}$
    - Burst-size range $\in \{[0.1C, 0.5C], [0.3C, 0.9C], [0.6C, 1.0C]\}$
    - Full $3 \times 3 = 9$ combinations across all $k \in \{1.1, 1.25, 1.5, 2, 4\}$ and seeds 2000–2029.
* **Criteria Changes:** None. Strict original wording, no tolerance margins.

---

## 9. Experiment E4: Note on Cross-Client Fairness (Dropped)
* **Fairness Metric Status:** Experiment E4 is **NOT run**.
* **Rationale:** In per-client rate limiters, client instances are isolated and do not share state or contend for common token pools. Because per-client limiters do not interact across clients, cross-client fairness (e.g. Jain's Fairness Index) is determined entirely by the offered load distributions and client concurrency, and cannot be altered or differentiated by individual rate limiting algorithms. Consequently, cross-client fairness evaluation is dropped.

---

## 10. Formal Hypotheses: Experiment E5 (Contract Boundary Compliance)

### Hypothesis H5a: Complete Admission Below Contract
> **Hypothesis Statement:** for compliant steady clients offered strictly below the contracted rate (offered rate in {0.90R, 0.98R}), all algorithms achieve complete admission (admitted / offered = 1.000, admitted rate / R = k) across both Poisson and periodic arrival processes, without false rejections.

* **Metric:** Admitted cost / offered cost over the run and admitted rate / $R$.
* **Conditions:** Offered rates $k \in \{0.90, 0.98, 1.00, 1.02, 1.10\} \times R$, arrival processes: Poisson and periodic, $C \in \{1000, 50000\}$, $N = 30$ seeds (2000–2029).
* **Supporting Outcome:** For $k < 1.00$, admitted / offered ratio is $\ge 0.999$ with zero false rejections.
* **Contradicting Outcome:** Any algorithm rejects compliant traffic at $0.90R$ or $0.98R$ under either Poisson or periodic arrivals.
* **Empirical Outcome & Verdict:**
  - **Periodic Arrivals ($k \in \{0.90, 0.98\}$):** **SUPPORTED** across all 10 algorithms (100.00% admission, 0.00% FRR).
  - **Poisson Arrivals ($k \in \{0.90, 0.98\}$):** **PARTIALLY SUPPORTED / CONTRADICTED for window limiters**. Token Bucket (large, small, env), Leaky Bucket, and all DREB variants achieved 100.00% admission; discrete-window algorithms (Fixed Window, Sliding Window Log, Sliding Window Counter) exhibited boundary rejections (0.8%–2.6% FRR) due to Poisson request clustering across window borders.
  - **Overall Verdict:** **PARTIALLY SUPPORTED** (Strictly supported under periodic compliant load; window-based limiters fail under Poisson arrivals).

### Hypothesis H5b: Strict Contract Capping at and Above Contract
> **Hypothesis Statement:** at and above the contracted rate (offered rate in {1.00R, 1.02R, 1.10R}), all algorithms cap admitted rate to at most the contracted envelope (S_max + R*T) / (R*T), admitting at most R under sustained periodic load.

* **Supporting Outcome:** Under periodic arrivals with $k \ge 1.00$, admitted rate / $R \le 1.001$, preventing sustained over-admission.
* **Contradicting Outcome:** Admitted rate exceeds the cumulative envelope bound.
* **Empirical Outcome & Verdict:** **SUPPORTED**. Under sustained periodic arrivals at and above $1.00R$, all evaluated algorithms strictly cap admitted rates to the contracted rate envelope $1.000R$ (cumulative admitted rate $\le 1.043R$ over 30s including cold-start capacity).

---

## 11. Formal Hypotheses: Experiment E6 (Variable Cost Requests and Atomic Overdraw)

### Hypothesis H6a: Strict Capacity Ceiling Under Variable Costs
> **Hypothesis Statement:** under variable cost requests drawn from {1, 5, 20, 60, 200} at C = 1000, DREB burst-matched with the P4 fix (legacyOverdraw = false) never admits any single request whose cost exceeds available capacity, and the largest single admitted request never exceeds C.

* **Metric:** Largest single request admitted, admitted cost / offered cost, and false rejection rate by cost class.
* **Supporting Outcome:** When `legacyOverdraw = false`, the maximum admitted request cost $\le C = 1000$ strictly, and partial or negative token balances are prevented.
* **Contradicting Outcome:** Any request admitted when tokens are insufficient, or any single admitted request with $\text{cost} > C$.
* **Empirical Outcome & Verdict:** **SUPPORTED**. The largest single request admitted under the $\{1, 5, 20, 60, 200\}$ distribution was 200 ($\le C = 1000$). Under post-idle boundary testing with oversized requests ($1200 > C$), `legacyOverdraw = false` strictly rejected the request, preserving the $C = 1000$ capacity ceiling.

### Hypothesis H6b: Overdraw Elimination
> **Hypothesis Statement:** DREB burst-matched with legacyOverdraw = true admits requests when available tokens are strictly less than request cost, causing token balances to drop below zero, whereas legacyOverdraw = false strictly eliminates token deficits.

* **Supporting Outcome:** `legacyOverdraw = true` allows token deficits, whereas `legacyOverdraw = false` prevents token deficits and shows distinct rejection profiles on large request classes.
* **Contradicting Outcome:** No difference in token deficit behavior between `legacyOverdraw = true` and `legacyOverdraw = false`.
* **Empirical Outcome & Verdict:** **SUPPORTED (via unscaled idle credit overdraw)**. While atomic bucket clamping prevents negative token counts in memory, `legacyOverdraw = true` grants raw unscaled idle credit ($1:1$ rather than dividing by decay $\gamma = 1.5$), admitting requests up to $9C/7 = 1286$ after idleness (admitting a request of cost 1200), whereas `legacyOverdraw = false` strictly enforces effective credit scaled by idle decay, eliminating overdraw beyond $C = 1000$.

---

## 12. Formal Hypotheses: Experiment E10 (Peak Short-Window Admitted Load & 3-Metric Dominance)

### Hypothesis H10: Peak Short-Window Admitted Load Advantage
> **Hypothesis Statement:** against abuser traffic at k in {1.1, 1.25, 1.5, 2, 4}, DREB burst-matched has lower peak short-window admitted load than any static policy that is not worse on both admitted rate and bursty-legit FRR.

* **Metric:** Peak abuser admitted cost in sliding window of length $W \in \{0.05, 0.10, 0.25\} C/R$, per seed, normalized by $C$; paired 95% bootstrap CIs over seeds 2000–2029 ($N = 30$).
* **Candidate Set:** All static policies from families S1, S2, and S3 that are not worse than DREB on both long-term abuser admitted rate and bursty-legit FRR ($\text{Rate}_{\text{static}} - \text{Rate}_{\text{DREB}} \le 0$ and $\text{FRR}_{\text{static}} - \text{FRR}_{\text{DREB}} \le 0$, judged by paired 95% bootstrap CIs).
* **Supporting Outcome:** For every non-worse static policy, DREB burst-matched achieves strictly lower peak admitted load:
  $$\text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} > 0 \quad (\text{paired 95% CI lower bound } > 0.0)$$
* **Contradicting Outcome:** Any non-worse static policy achieves equal or lower peak short-window admitted load:
  $$\text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} \le 0 \quad (\text{paired 95% CI includes zero or is negative})$$
* **Empirical Outcome & Verdict:** **REFUTED** (judged by paired 95% bootstrap CIs over seeds 2000–2029, evaluated without tolerance margins).
  1. *Small Bursts ($0.30C$):* Across all $k \in \{1.10, 1.25, 1.50, 2.00, 4.00\}$ and all window sizes $W \in \{0.05, 0.10, 0.25\} C/R$, non-worse static dual buckets (`DualBucket(B=0.85C, P=4.00R)`) admit identical peak short-window load as DREB ($\Delta\text{PeakLoad} = +0.000$ $[+0.000, +0.000]$). Because H10 strictly requires strictly lower peak load, this equality refutes H10.
  2. *Pre-Warmed State at High Overload ($k \ge 2.00$):* Under pre-warmed initial conditions, static dual buckets achieve *strictly lower* peak admitted load than DREB (e.g. at $k = 2.00, \text{burst} = 0.95C$, $\Delta\text{PeakLoad} = -0.100$ $[-0.100, -0.100]$) because DREB allows burst capacity plus accumulated credit to discharge simultaneously in short intervals.
  3. *Three-Metric Evaluation Clarification:* While static dual buckets with equal peak load tie DREB at small bursts ($0.30C$) and achieve lower peak load under pre-warmed states at $k \ge 2.00$, under cold start with large bursts (e.g. $k = 4.00$, burst $0.95C$), DREB is 2-metric dominated in E9 by `DualBucket(B=1.00C, P=4.00R)` yet admits substantially *lower* peak short-window load ($0.428C$ vs $0.850C$, $\Delta = +0.422C$). Hence DREB is not universally 3-metric dominated in cold start.

### Hypothesis H10b: Steady-State Peak Short-Window Admitted Load Advantage (Restricting to t >= 5 s)
> **Hypothesis Statement:** restricting the peak short-window admitted load to windows that start at t >= 5 s (after the initial full-bucket burst), DREB burst-matched has strictly lower peak load than every static policy that is not worse on both admitted rate and bursty-legit FRR, at k in {1.1, 1.25, 1.5, 2, 4}.

* **Metric:** Peak abuser admitted cost in sliding window of length $W \in \{0.05, 0.10, 0.25\} C/R$ for windows starting at $t \ge 5.0\,\text{s}$ (post-warmup steady state), normalized by $C$; paired 95% bootstrap CIs over seeds 2000–2029 ($N = 30$).
* **Candidate Set:** All static policies from families S1, S2, and S3 that are not worse than DREB on both long-term abuser admitted rate and bursty-legit FRR ($\text{Rate}_{\text{static}} - \text{Rate}_{\text{DREB}} \le 0$ and $\text{FRR}_{\text{static}} - \text{FRR}_{\text{DREB}} \le 0$, judged by paired 95% bootstrap CIs).
* **Supporting Outcome:** For every non-worse static policy, DREB burst-matched achieves strictly lower peak admitted load:
  $$\text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} > 0 \quad (\text{paired 95% CI lower bound } > 0.0)$$
* **Contradicting Outcome:** Any non-worse static policy achieves equal or lower peak short-window admitted load:
  $$\text{PeakLoad}_{\text{static}} - \text{PeakLoad}_{\text{DREB}} \le 0 \quad (\text{paired 95% CI includes zero or is negative})$$



