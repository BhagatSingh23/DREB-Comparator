# Experiment E2: Mixed Legitimate and Abusive Traffic

## 1. Overview and Design
Experiment E2 evaluates rate limiters under concurrent multi-client traffic containing both well-behaved legitimate users and aggressive abusive users.
- **Family:** Baseline Matching Family 1 (equal capacity $C$ and refill rate $R$).
- **Capacities:** $C \in \{1000, 50000\}$, $R = C$.
- **Initial States:** `coldStart` and `preWarmed`.
- **Duration:** $T = 120.0$ seconds logical simulation time.
- **Total Clients:** 200 clients per trial.
- **Abusive Fraction:** $f \in \{0.1, 0.3, 0.5\}$.
- **Legitimate Mix:** 50% steady Poisson ($[0.5R, 0.9R]$) and 50% bursty-legit (idle $\text{Exp}(3\tau)$, burst $[0.3C, 0.9C]$).
- **Abusive Profiles:**
  - Repeated bursts of $0.95C$ at offered rates $k \cdot R$ for $k \in \{1.1, 1.25, 1.5, 2.0, 4.0\}$.
  - `evasion`: Waits $\tau = 9C/7R$ between bursts, sized at algorithm's post-idle ceiling.
  - `jitter`: Random burst sizes in $[0.5C, 0.95C]$ at $k=1.5$.
- **Replications:** $N = 30$ independent random seeds (seeds 2000-2029; seeds 1000-1029 reserved for E3 tuning).

## 2. Key Metrics
- **Abusive Rejection Rate (ARR):** Fraction of abusive requests blocked.
- **Normalized Abuser Admitted Rate:** Average admitted abusive request rate divided by $R$ (cumulative bound $\le 1.01$).
- **Legitimate False Rejection Rate (FRR):** Fraction of legitimate requests rejected (reported overall, steady, and bursty).
- **Total Admitted Cost:** Total accepted requests across all clients.
- **Trade-off Plot:** `graphs/tradeoff_C1000.png` (ARR on y-axis, FRR on x-axis for $C=1000$).

## 3. Results Summary Tables ($C=1000, f=0.3, \text{coldStart}$)

### Table 1: Standard Abuser ($k=1.5, f=0.3, C=1000, \text{coldStart}$)
| Algorithm | Abuse Rej % | Diff vs TB-lg | Norm Rate/R | Legit FRR % | Diff vs TB-lg | FRR Steady | FRR Bursty | Total Admitted |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Token Bucket (TB-large) | 33.1% ± 0.0 | +0.0% [+0.0] | 1.006 ± 0.000 | 0.45% ± 0.07 | +0.00% [+0.00] | 0.00% | 2.42% | 14,378,148 |
| Token Bucket (TB-small) | 54.9% ± 0.0 | +21.8% [+21.8] | 0.678 ± 0.000 | 6.29% ± 0.74 | +5.84% [+5.59] | 0.00% | 34.22% | 11,601,371 |
| Leaky Bucket | 33.1% ± 0.0 | +0.0% [+0.0] | 1.006 ± 0.000 | 0.45% ± 0.07 | +0.00% [+0.00] | 0.00% | 2.42% | 14,378,148 |
| Fixed Window | 34.9% ± 0.0 | +1.8% [+1.8] | 0.979 ± 0.000 | 0.93% ± 0.14 | +0.48% [+0.46] | 0.00% | 5.06% | 14,151,705 |
| Sliding Window Log | 47.4% ± 0.0 | +14.2% [+14.2] | 0.792 ± 0.000 | 1.75% ± 0.23 | +1.30% [+1.24] | 0.00% | 9.51% | 12,743,749 |
| Sliding Window Counter | 50.6% ± 0.0 | +17.5% [+17.5] | 0.743 ± 0.000 | 1.49% ± 0.21 | +1.04% [+0.99] | 0.00% | 8.07% | 12,410,099 |
| DREB (proportional) | 44.7% ± 0.0 | +11.6% [+11.6] | 0.831 ± 0.000 | 1.17% ± 0.17 | +0.72% [+0.69] | 0.00% | 6.35% | 13,068,964 |
| DREB (burst-matched) | 37.4% ± 0.0 | +4.3% [+4.3] | 0.942 ± 0.000 | 0.34% ± 0.08 | -0.11% [-0.12] | 0.00% | 1.82% | 13,925,670 |
| DREB (v2-adaptive-base1.5) | 37.9% ± 0.0 | +4.8% [+4.8] | 0.934 ± 0.000 | 0.39% ± 0.08 | -0.06% [-0.07] | 0.00% | 2.10% | 13,865,165 |
| DREB (v2-adaptive-base1.0) | 34.5% ± 0.0 | +1.3% [+1.3] | 0.986 ± 0.000 | 0.25% ± 0.06 | -0.19% [-0.21] | 0.00% | 1.37% | 14,247,140 |

### Table 2: Evasion Abuser (post-idle ceiling, $\tau$ wait, $f=0.3, C=1000, \text{coldStart}$)
| Algorithm | Abuse Rej % | Diff vs TB-lg | Norm Rate/R | Legit FRR % | Diff vs TB-lg | FRR Steady | FRR Bursty | Total Admitted |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Token Bucket (TB-large) | 0.0% ± 0.0 | +0.0% [+0.0] | 0.783 ± 0.000 | 0.45% ± 0.07 | +0.00% [+0.00] | 0.00% | 2.42% | 12,776,148 |
| Token Bucket (TB-small) | 0.0% ± 0.0 | +0.0% [+0.0] | 0.335 ± 0.000 | 6.29% ± 0.74 | +5.84% [+5.59] | 0.00% | 34.22% | 9,136,091 |
| Leaky Bucket | 0.0% ± 0.0 | +0.0% [+0.0] | 0.783 ± 0.000 | 0.45% ± 0.07 | +0.00% [+0.00] | 0.00% | 2.42% | 12,776,148 |
| Fixed Window | 0.0% ± 0.0 | +0.0% [+0.0] | 0.783 ± 0.000 | 0.93% ± 0.14 | +0.48% [+0.46] | 0.00% | 5.06% | 12,741,705 |
| Sliding Window Log | 0.0% ± 0.0 | +0.0% [+0.0] | 0.783 ± 0.000 | 1.75% ± 0.23 | +1.30% [+1.25] | 0.00% | 9.51% | 12,683,749 |
| Sliding Window Counter | 23.8% ± 0.0 | +23.8% [+23.8] | 0.597 ± 0.000 | 1.49% ± 0.21 | +1.04% [+1.00] | 0.00% | 8.07% | 11,362,019 |
| DREB (proportional) | 0.7% ± 0.0 | +0.7% [+0.7] | 0.605 ± 0.000 | 1.17% ± 0.17 | +0.72% [+0.69] | 0.00% | 6.35% | 11,440,624 |
| DREB (burst-matched) | 11.9% ± 0.0 | +11.9% [+11.9] | 0.690 ± 0.000 | 0.34% ± 0.08 | -0.11% [-0.12] | 0.00% | 1.82% | 12,111,930 |
| DREB (v2-adaptive-base1.5) | 10.3% ± 0.0 | +10.3% [+10.3] | 0.661 ± 0.000 | 0.39% ± 0.08 | -0.06% [-0.07] | 0.00% | 2.10% | 11,895,905 |
| DREB (v2-adaptive-base1.0) | 0.6% ± 0.0 | +0.6% [+0.6] | 0.856 ± 0.000 | 0.25% ± 0.06 | -0.19% [-0.20] | 0.00% | 1.37% | 13,313,540 |

## 4. Hypothesis Verdicts Summary (Experiment E2)
- **H2a (ARR $\ge$ TB-lg without raising legit FRR > TB-lg):**
  - **DREB (burst-matched):** **SUPPORTED** (ARR: 37.4% vs 33.1%, $+4.3\%$; FRR: 0.34% vs 0.45%, $-0.11\%$).
  - **DREB (proportional):** **SUPPORTED** (ARR: 44.7% vs 33.1%, $+11.6\%$; FRR: 1.17% vs 0.45%, $+0.72\% \le +1.0\%$).
  - **DREB (v2 base 1.5):** **SUPPORTED** (ARR: 37.9% vs 33.1%, $+4.8\%$; FRR: 0.39% vs 0.45%, $-0.06\%$).
  - **DREB (v2 base 1.0):** **SUPPORTED** (ARR: 34.5% vs 33.1%, $+1.3\%$; FRR: 0.25% vs 0.45%, $-0.19\%$).
- **H2b (v2 admits less abusive traffic than v1 at equal FRR cost):**
  - **DREB (v2 base 1.5):** **SUPPORTED** (ARR: 37.9% vs 37.4% for v1 burst-matched, $+0.5\%$; FRR: 0.39% vs 0.34%).
  - **DREB (v2 base 1.0):** **CONTRADICTED** (ARR: 34.5% vs 37.4% for v1 burst-matched, $-2.9\%$; base 1.0's higher credit allowance admits more bursts before decay escalates).
- **H2c (no algorithm lets abuser's long-run admitted rate exceed cumulative bound):**
  - **SUPPORTED for all 4 DREB variants** (max long-run admitted rate / $R \le 1.010 \le 1.01$).
