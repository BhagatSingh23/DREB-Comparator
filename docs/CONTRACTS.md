# Rate Limiter Algorithm Contracts and Violation Predicates

This document defines the formal admission contract and mathematical violation predicates implemented by each rate-limiting algorithm in `com.ratelimiter.core.AlgorithmContract`.

Rather than comparing all algorithms against an arbitrary uniform window (which penalizes algorithms with distinct structural semantics, such as burst allowances or aligned windows), each algorithm is verified against its own stated contract.

---

## 1. Formal Algorithm Invariants

### 1.1 Token Bucket
A token bucket with capacity $C$ and refill rate $R$ (tokens/sec) maintains tokens $B(t) \in [0, C]$.
- **Instantaneous Burst Invariant:** For any single timestamp $t$:
  $$\sum_{i: t_i = t, \text{allowed}} \text{cost}_i \le C$$
- **Cumulative Envelope Invariant:** Over any arbitrary closed time interval $[t_1, t_2]$:
  $$A(t_1, t_2) \le C + R \cdot (t_2 - t_1)$$
  *Violation Condition:* If either $\max_{t} A(t, t) > C$ or $\max_{t_1 \le t_2} [A(t_1, t_2) - R \cdot (t_2 - t_1)] > C$, a contract violation is recorded.

---

### 1.2 Leaky Bucket (Meter / Dropping Mode)
An unbuffered leaky bucket leaking at rate $R$ with capacity $C$:
- **Cumulative Envelope Invariant:** Over any time interval $[t_1, t_2]$:
  $$A(t_1, t_2) \le C + R \cdot (t_2 - t_1)$$
  *Violation Condition:* Recorded whenever cumulative admission exceeds $C + R \cdot (t_2 - t_1)$ for any $[t_1, t_2]$.

---

### 1.3 Fixed Window Counter
Fixed window partitions timeline into discrete aligned intervals $[k \cdot W, (k+1) \cdot W)$ of length $W$:
- **Aligned Window Invariant:** In every aligned window $k$:
  $$A(k \cdot W, (k+1) \cdot W) \le C$$
  *Violation Condition:* If the total admitted requests within any single window index $k = \lfloor t / W \rfloor$ exceeds $C$, a violation is recorded.
  *(Note: fixed window permits up to $2C$ across window boundaries without violating its own contract).*

---

### 1.4 Sliding Window Log
Sliding Window Log maintains exact request timestamps:
- **Sliding Window Invariant:** For every timestamp $t$:
  $$A(t - W, t) \le C$$
  *Violation Condition:* For any admitted request at timestamp $t$, if the sum of admissions in $(t - W, t]$ exceeds $C$, a violation is recorded.

---

### 1.5 Sliding Window Counter
Approximates sliding window using a weighted sum of the previous and current aligned windows:
- **Weighted Estimate Invariant:** For every request arriving at timestamp $t$ within current window $k = \lfloor t / W \rfloor$:
  $$\text{estimated}(t) = \text{count}_{\text{current}} + \text{count}_{\text{previous}} \cdot \frac{W - (t \bmod W)}{W} \le C$$
  *Violation Condition:* Admitting a request when $\text{estimated}(t) + \text{cost} > C$.

---

### 1.6 DREB (Dual-Rate Elastic Bucket)

DREB operates dual buckets (burst capacity $B_{\max}$, sustained capacity $S_{\max}$, sustained rate $S_{\text{rate}} = R$) and an idle credit pool ($\text{idle\_cap}$, effective decay $\gamma$).

- **Instantaneous Burst Invariant:** The maximum unit-cost requests admitted instantaneously from idle/full state cannot exceed the burst ceiling:
  $$A(t, t) \le B_{\max} + \frac{\text{idle\_cap}}{\gamma}$$
  - For **Proportional Mapping**: $B_{\max} = C/3$, $\text{idle\_cap} = 2C/3$, $\gamma = 1.5 \implies \text{Burst Ceiling} = \frac{7}{9}C \approx 0.778 C$.
  - For **Burst-Matched Mapping**: $B_{\max} = 3C/7$, $\text{idle\_cap} = 6C/7$, $\gamma = 1.5 \implies \text{Burst Ceiling} = C$.
  - For **v2 Adaptive** ($\text{baseDecay} = 1.0$): $\text{Burst Ceiling} = \frac{3}{7}C + \frac{6}{7}C = \frac{9}{7}C \approx 1.286 C$.
  - For **v2 Adaptive** ($\text{baseDecay} = 1.5$): $\text{Burst Ceiling} = C$.

- **Cumulative Admission Envelope Invariant:** Governed by the sustained bucket $S_{\max}$ and refill rate $S_{\text{rate}} = R$. Over any interval $[t_1, t_2]$:
  $$A(t_1, t_2) \le S_{\max} + S_{\text{rate}} \cdot (t_2 - t_1)$$
  - For **Proportional Mapping**: $S_{\max} = C, S_{\text{rate}} = R \implies A(t_1, t_2) \le C + R \cdot (t_2 - t_1)$.
  - For **Burst-Matched & v2**: $S_{\max} = \frac{9}{7}C, S_{\text{rate}} = R \implies A(t_1, t_2) \le \frac{9}{7}C + R \cdot (t_2 - t_1)$.

*Violation Condition:* Any interval violating either the instantaneous burst ceiling or the sustained bucket envelope generates a contract violation.

---

## 2. Zero-Violation Verification

In all benchmark evaluations, each algorithm's full request-response trace is evaluated against its respective contract validator (`AlgorithmContract.evaluateViolations()`). A properly functioning algorithm operating without race conditions or bugs achieves:
$$\text{violationCount} = 0$$
Any non-zero violation count flags a conformance failure.
