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
> **Hypothesis Statement:** DREB variants admit a smaller fraction of abusive traffic than `TB-large` (higher abusive rejection rate) without raising false rejection of legitimate clients above `TB-large`'s false rejection rate.

* **Primary Metrics:**
  - Abusive Request Rejection Rate: $\text{ARR} = \frac{\text{Abusive Requests Rejected}}{\text{Abusive Requests Offered}}$
  - Legitimate False Rejection Rate: $\text{FRR} = \frac{\text{Legit Requests Rejected}}{\text{Legit Requests Offered}}$, evaluated for both steady Poisson and bursty-legit clients.
* **Supporting Outcome:**
  - $\text{ARR}_{\text{DREB}} > \text{ARR}_{\text{TB-large}}$ across abusive scenarios ($k \in \{1.1, 1.25, 1.5, 2.0, 4.0\}$, evasion, jitter).
  - $\text{FRR}_{\text{DREB}} \le \text{FRR}_{\text{TB-large}} + \epsilon$ (where $\epsilon \le 0.01$ or within bootstrap 95% CI).
* **Contradicting Outcome:**
  - $\text{ARR}_{\text{DREB}} \le \text{ARR}_{\text{TB-large}}$ (DREB fails to suppress abusive traffic better than TB-large), OR
  - $\text{FRR}_{\text{DREB}} > \text{FRR}_{\text{TB-large}} + 0.01$ (DREB penalizes legitimate traffic more than TB-large).

---

### Hypothesis H2b: DREB v2 Adaptive Penalty vs DREB v1
> **Hypothesis Statement:** DREB v2 admits less abusive traffic (achieves higher abusive rejection rate) than DREB v1 at equal or lower legitimate false-rejection cost.

* **Primary Metrics:**
  $$\Delta \text{ARR} = \text{ARR}_{\text{v2}} - \text{ARR}_{\text{v1-burst-matched}}$$
  $$\Delta \text{FRR} = \text{FRR}_{\text{v2}} - \text{FRR}_{\text{v1-burst-matched}}$$
* **Supporting Outcome:**
  - $\Delta \text{ARR} > 0$ for abusive clients (particularly repeat burst and evasion abusers).
  - $\Delta \text{FRR} \le \epsilon$ for legitimate clients (steady and bursty).
* **Contradicting Outcome:**
  - $\text{ARR}_{\text{v2}} \le \text{ARR}_{\text{v1}}$, OR $\text{FRR}_{\text{v2}} > \text{FRR}_{\text{v1}} + 0.01$.

---

### Hypothesis H2c: Long-Run Cumulative Rate Bound Enforcement
> **Hypothesis Statement:** No algorithm lets an abusive client's long-run admitted rate exceed its cumulative bound $R$ (i.e. $\frac{\text{Admitted Abusive Rate}}{R} \le 1.0 + \frac{S_{\max}}{R \cdot T} \approx 1.01$ over logical duration $T = 120\text{s}$).

* **Primary Metric:**
  $$\text{Normalized Admitted Rate} = \frac{\text{Total Admitted Abusive Cost} / T}{R}$$
* **Supporting Outcome:**
  - Normalized Admitted Rate $\le 1.0 + \frac{S_{\max}}{R \cdot T}$ for all algorithms and all abuser types ($k \in \{1.1, 1.25, 1.5, 2, 4\}$, evasion, jitter).
* **Contradicting Outcome:**
  - Normalized Admitted Rate exceeds the theoretical cumulative bound for any algorithm.

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
  - Evasion abuser: waits $\tau = \frac{9C}{7R}$ between bursts, sized at algorithm's post-idle ceiling, with offered rate target $k = 1.5$.
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
* **Metrics:** Abusive rejection rate, normalized abuser admitted rate ($\text{Rate}/R$), legitimate false-rejection rate (steady and bursty), total admitted cost, and trade-off plot (FRR vs ARR) at $C=1000$.
