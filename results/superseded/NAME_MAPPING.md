# Algorithm Name & Parameter Mapping (Superseded to Phase 2)

This document provides the definitive cross-reference mapping the legacy algorithm identifiers used in the superseded pre-fix benchmark runs (`results/superseded/20260901_144038` and `results/superseded/20260902_030333`) to the standardized identifiers and configurations established in Phase 2.

---

## 1. Algorithm Name Mapping

| Superseded Legacy Name | Phase 2 Standard Name | Code Enum / Strategy Key | Configured Max Burst (Full Credit) | Headline Role |
|:---|:---|:---|:---:|:---|
| `Custom (proportional)` | `DREB (proportional)` | `PROPORTIONAL` | $\frac{7}{9}C \approx 0.778C$ | v1 proportional capacity baseline |
| `Custom (burst-matched)` | `DREB (burst-matched)` | `BURST_CEILING_MATCHED` | $C$ | v1 burst-matched baseline |
| `Custom (v2-adaptive)` | `DREB (v2-adaptive-base1.0)` | `V2_ADAPTIVE_BASE_1_0` | $\frac{9}{7}C \approx 1.286C$ | Named variant (unpenalized base decay 1.0) |
| *(None - New in Phase 2)* | `DREB (v2-adaptive-base1.5)` | `V2_ADAPTIVE_BASE_1_5` | $C$ | **Headline v2 Policy** (matched burst ceiling $C$) |
| `Token Bucket` | `Token Bucket (TB-large)` | N/A | $C$ | Standard full-capacity baseline |
| *(None - New in Phase 2)* | `Token Bucket (TB-small)` | N/A | $B_{\max} = \frac{3}{7}C$ | Restricted-capacity Token Bucket baseline |
| `Leaky Bucket` | `Leaky Bucket` | N/A | $0$ (queued) | Standard traffic-shaping baseline |
| `Fixed Window` | `Fixed Window` | N/A | $C$ | Aligned window counter baseline |
| `Sliding Window Log` | `Sliding Window Log` | N/A | $C$ | Exact memory-intensive sliding baseline |
| `Sliding Window Counter` | `Sliding Window Counter` | N/A | $C$ | Approximated weighted sliding baseline |

---

## 2. Key Parameter & Behavior Differences

1. **`Custom (v2-adaptive)` vs `DREB (v2-adaptive-base1.5)`:**
   - In superseded runs, `Custom (v2-adaptive)` used hardcoded $\text{baseDecay} = 1.0$. Because a clean client with $\text{abuseScore} = 0$ paid no decay multiplier on idle credit, its instantaneous burst ceiling reached:
     $$B_{\max} + \frac{\text{idle\_cap}}{1.0} = \frac{3}{7}C + \frac{6}{7}C = \frac{9}{7}C \approx 1.286C$$
   - In Phase 2, the headline comparison policy is `DREB (v2-adaptive-base1.5)` with $\text{baseDecay} = 1.5$. This ensures the clean burst ceiling strictly equals $C$ ($3C/7 + (6C/7)/1.5 = C$), isolating the impact of abuse-dependent penalization from an inflated base credit allowance.
   - The old 1.0 configuration is retained under the explicit name `DREB (v2-adaptive-base1.0)`.

2. **Superseded Library Defects (Pre-Fix vs `v1.0.2-phase1`):**
   - The superseded results were generated with an internal unreleased prototype that suffered from:
     - Missing mid-gap idle credit accrual (idle credit accrual was skipped during interval transitions).
     - Permissive admission check for requests exceeding bucket boundaries.
     - Timestamp regression on clock rollback.
     - Dropped `abuseScore` in Redis state persistence.
   - All Phase 2 benchmark runs utilize `com.github.BhagatSingh23:dreb-ratelimiter:v1.0.2-phase1` (commit `53a5c9e`), resolving all aforementioned defects.
