# DREB Parameter Derivations and Strategy Specifications

This document defines the formal mappings from external rate-limiting parameters (capacity $C$ and sustained refill rate $R$) to all internal DREB parameters across both algorithm versions (v1 and v2), maintained via `com.ratelimiter.algorithms.DrebParameters`.

---

## 1. Mathematical Mappings (Functions of $C$ and $R$)

| Parameter | Meaning | Unit | DREB Proportional (v1) | DREB Burst-Matched (v1) | DREB v2 (`v2_adaptive_base_1_5`) [Headline] | DREB v2 (`v2_adaptive_base_1_0`) [Variant] |
|:---|:---|:---:|:---:|:---:|:---:|:---:|
| **$S_{\max}$** | Sustained capacity | cost units | $C$ | $\frac{9}{7}C \approx 1.286 C$ | $\frac{9}{7}C$ | $\frac{9}{7}C$ |
| **$S_{\text{rate}}$** | Sustained refill rate | cost / s | $R$ | $R$ | $R$ | $R$ |
| **$B_{\max}$** | Burst capacity | cost units | $\frac{1}{3}C \approx 0.333 C$ | $\frac{3}{7}C \approx 0.429 C$ | $\frac{3}{7}C$ | $\frac{3}{7}C$ |
| **$B_{\text{rate}}$** | Burst refill rate | cost / s | $\frac{5}{3}R \approx 1.667 R$ | $\frac{5}{3}R \approx 1.667 R$ | $\frac{5}{3}R$ | $\frac{5}{3}R$ |
| **$\text{idle\_cap}$** | Max idle credit pool | cost units | $\frac{2}{3}C \approx 0.667 C$ | $\frac{6}{7}C \approx 0.857 C$ | $\frac{6}{7}C$ | $\frac{6}{7}C$ |
| **$\text{idle\_gain}$** | Credit accrual rate | credit / s | $\frac{2}{3}R \approx 0.667 R$ | $\frac{2}{3}R \approx 0.667 R$ | $\frac{2}{3}R$ | $\frac{2}{3}R$ |
| **$\text{idle\_decay}$** / **$\text{baseDecay}$** | Base debit multiplier | dimensionless | $1.5$ | $1.5$ | **$1.5$** | **$1.0$** |
| **$\text{abuseGain}$** | Decay escalator per score | dimensionless | N/A | N/A | $0.3$ | $0.3$ |
| **$\text{abuseIncrement}$** | Score rise per full draw | dimensionless | N/A | N/A | $2.0$ | $2.0$ |
| **$\text{abuseDecayRate}$** | Score recovery rate | score / s | N/A | N/A | $1.5$ | $1.5$ |
| **$\text{sustainedCostGain}$**| Sustained cost escalator | dimensionless | N/A | N/A | $0.35$ | $0.35$ |
| **Cold-Start Max Burst** | Fresh client ($B_{\max}$, credit=0) | requests | $\frac{1}{3}C \approx 0.333 C$ | $\frac{3}{7}C \approx 0.429 C$ | $\frac{3}{7}C \approx 0.429 C$ | $\frac{3}{7}C \approx 0.429 C$ |
| **Full-Credit Theoretical Burst** | Static bound ($B_{\max} + \frac{\text{idle\_cap}}{\text{decay}}$) | requests | $\frac{7}{9}C \approx 0.778 C$ | $C$ | $C$ | $\frac{9}{7}C \approx 1.286 C$ |
| **Full-Credit Measured Burst ($C=100$)** | Actual empirical instantaneous burst | requests | $77$ | $99$ ($\approx C$) | **$94$** (intra-burst decay) | **$110$** (intra-burst decay) |
| **Cumulative Bound ($T$)** | Envelope over interval $T$ | cost units | $C + R \cdot T$ | $\frac{9}{7}C + R \cdot T$ | $\frac{9}{7}C + R \cdot T$ | $\frac{9}{7}C + R \cdot T$ |

---

## 2. Invariants, Initial State & Verified Identities

1. **Cold-Start vs Pre-Warmed Burst Limits:**
   - A fresh client starting with no idle history begins with full token buckets but an empty idle credit pool ($\text{idleCredit} = 0$). Its maximum instantaneous burst is strictly bounded by the burst bucket capacity $B_{\max}$ (e.g. 33 for proportional, 42 for burst-matched at $C=100$).
   - "Full-credit max burst" (or "Max Initial Burst" in pre-warmed tables) applies strictly to a client that has accrued maximum idle credit ($\text{idleCredit} = \text{idle\_cap}$).
2. **Burst Ceiling Identity (Burst-Matched & v2 Base 1.5):**
   $$B_{\max} + \frac{\text{idle\_cap}}{\text{idle\_decay}} = \frac{3}{7}C + \frac{\frac{6}{7}C}{1.5} = \frac{3}{7}C + \frac{4}{7}C = C$$
3. **Sustained Capacity Identity (Proportional):**
   $$S_{\max} = C$$
4. **Refill Rate Equality:**
   $$S_{\text{rate}} = R$$
5. **Internal Ratios:**
   - $\text{idle\_cap} = 2 \cdot B_{\max}$
   - $S_{\max} = 3 \cdot B_{\max}$
   - $B_{\text{rate}} = \frac{5}{3} S_{\text{rate}}$
   - $\text{idle\_gain} = \frac{2}{3} S_{\text{rate}}$

---

## 3. Policy Choice: Base Decay for DREB v2 (`baseDecay`) and Intra-Burst Abuse Escalation

In DREB v2, the effective idle debit multiplier applied to borrowed idle credit is dynamic:
$$\text{effectiveDecay} = \text{baseDecay} + \text{abuseGain} \cdot \text{abuseScore}$$

### Intra-Burst Abuse Escalation Mechanism
In static formulas, one might expect an instantaneous burst from full idle credit to admit $B_{\max} + \frac{\text{idle\_cap}}{\text{baseDecay}}$. However, in `DREBLimiterV2`, whenever idle credit is borrowed (after the burst bucket $B_{\max}$ is exhausted), each request draws idle credit and increments `abuseScore` by $\text{abuseIncrement} \cdot \frac{\text{cost}}{\text{idle\_cap}}$. Because all requests in an instantaneous burst arrive at the exact same timestamp ($t = D$), `abuseScore` does not have time to decay.

As a direct consequence, `effectiveDecay` progressively escalates within the burst itself:
- Each subsequent request debits more idle credit than the previous one.
- The idle credit pool is exhausted earlier than predicted by the static base decay formula.

### Empirical Burst Ceilings ($C = 100$):
- **`v2_adaptive_base_1_5` (Headline):**
  - Static theoretical ceiling: $B_{\max} + \frac{\text{idle\_cap}}{1.5} = 42 + 57 = 100 = C$.
  - **Empirical measured burst:** Exactly **$94$** requests (at $C=1000$: $939$, at $C=50000$: $46,958$). The intra-burst escalation of `effectiveDecay` above $1.5$ reduces the first-burst admission by $\approx 6\%$.
  - Role: Headline comparison policy ensuring rigorous burst control without exceeding $C$.
- **`v2_adaptive_base_1_0` (Named Variant):**
  - Static theoretical ceiling: $\frac{9}{7}C \approx 128$ requests.
  - **Empirical measured burst:** Exactly **$110$** requests (at $C=1000$: $1,098$, at $C=50000$: $54,930$). The static "$\frac{9}{7}C$" claim is incorrect because intra-burst abuse escalation raises decay from $1.0$ upwards, capping the burst at $110$ rather than $128$.
  - Note: While capped below $128$, $110$ strictly exceeds `TB-large` ($100$), confirming that base decay 1.0 exceeds nominal capacity $C$.

Both options are exposed as first-class named configurations in `DrebParameters.Strategy` and in `config/experiments.yaml`.
