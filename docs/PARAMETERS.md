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
| **Full-Credit Max Burst** | Pre-warmed ($B_{\max} + \frac{\text{idle\_cap}}{\text{decay}}$) | requests | $\frac{7}{9}C \approx 0.778 C$ | **$C$** | **$C$** | $\frac{3}{7}C + \frac{6}{7}C = \frac{9}{7}C \approx 1.286 C$ |
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

## 3. Policy Choice: Base Decay for DREB v2 (`baseDecay`)

In DREB v2, the effective idle debit multiplier applied to borrowed idle credit is dynamic:
$$\text{effectiveDecay} = \text{baseDecay} + \text{abuseGain} \cdot \text{abuseScore}$$

There are two distinct parameterizations:

### Headline Comparison Policy: `baseDecay = 1.5` (`V2_ADAPTIVE_BASE_1_5`)
- **Specification:** At zero abuse ($\text{abuseScore} = 0$), the base decay matches v1's burst-matched $\text{idle\_decay} = 1.5$ exactly.
- **Burst Ceiling:** The maximum instantaneous burst from full state is strictly equal to $C$:
  $$B_{\max} + \frac{\text{idle\_cap}}{1.5} = \frac{3}{7}C + \frac{4}{7}C = C$$
- **Role:** Serves as the primary headline v2 algorithm in benchmark comparisons, ensuring that any burst allowance differences vs v1 or Token Bucket arise strictly from dynamic abuse penalty scaling rather than an inflated base credit multiplier.

### Named Variant Policy: `baseDecay = 1.0` (`V2_ADAPTIVE_BASE_1_0`)
- **Specification:** A client bursting after genuine idleness pays no extra penalty multiplier on idle credit ($\text{baseDecay} = 1.0$).
- **Burst Ceiling:** Base 1.0 plainly admits up to $\frac{9}{7}C$ in one burst (e.g. 128 unit requests at $C=100$):
  $$B_{\max} + \frac{\text{idle\_cap}}{1.0} = \frac{3}{7}C + \frac{6}{7}C = \frac{9}{7}C \approx 1.286 C$$
- **Role:** Maintained as an explicit named variant for evaluating the maximum possible credit reward for demonstrated idleness under zero abuse.

Both options are exposed as first-class named configurations in `DrebParameters.Strategy` and in `config/experiments.yaml`.
