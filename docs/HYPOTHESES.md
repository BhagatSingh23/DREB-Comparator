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
  - Any single observation where $A_{\text{DREB-1.5}} > A_{\text{TB-large}}$ ($\Delta > 0$). (Note: `DREB (v2-adaptive-base1.0)` is expected to exceed `TB-large` up to $9C/7$ by design, but decay 1.5 variants must never exceed $C$).

---

## 3. Experimental Parameters & Grid

* **Algorithms (10 total):**
  1. `Token Bucket (TB-large)` ($C, R$)
  2. `Token Bucket (TB-small)` ($B_{\max} = 3C/7, R$)
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
* **Replications:** $N = 30$ independent random seeds.
* **Outputs:** Accepted counts, mean, standard deviation, 95% bootstrap confidence intervals, paired differences vs `TB-small` and `TB-large`, and heatmaps per DREB variant.
