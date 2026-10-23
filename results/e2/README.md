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
  - Compliant bursty at $0.78R$ (formerly evasion): waits $\tau = 9C/7R$ between bursts, sized at algorithm's post-idle ceiling.
  - Jitter abuser: random burst sizes in $[0.5C, 0.95C]$ at $k=1.5$.
- **Replications:** $N = 30$ independent random seeds (seeds 2000–2029; seeds 1000–1029 reserved for E3 tuning).

## 2. Key Metrics & Population Independence
- **Per-Client Independence:** Per-client limiters do not interact across clients (state is keyed strictly by `clientId`). Consequently, the mixed population of 200 clients is mathematically and empirically equivalent to concurrent independent per-client experiments run in parallel.
- **Deterministic Abusive Traffic:** Abusive traces follow deterministic burst schedules ($0.95C$ bursts spaced by $(0.95C)/(kR)$), so trial-to-trial abuser outcomes have zero sample variance. As a result, abusive-side confidence intervals are zero-width ($N=1$ would suffice for the abusive metrics). Legitimate traffic, by contrast, contains stochastic Poisson and exponential inter-arrival jitter, generating non-zero width bootstrap CIs.
- **Abusive Rejection Rate (ARR):** Fraction of abusive requests blocked.
- **Normalized Abuser Admitted Rate:** Average admitted abusive request rate divided by $R$ (exact cumulative bound $(S_{\max} + R \cdot T) / (R \cdot T)$).
- **Legitimate False Rejection Rate (FRR):** Fraction of legitimate requests rejected (reported overall, steady, and bursty-legit).
- **Trade-off Plot:** `graphs/tradeoff_C1000.png` (ARR on y-axis, FRR on x-axis for $C=1000$).
- **Full Parameter Grid Tables:** See [FULL_TABLES.md](FULL_TABLES.md) for all 60 parameter grid tables ($C \in \{1000, 50000\} \times \text{initialState} \times f \times k$).

---

## 3. Core Results Tables ($C=1000, f=0.3, \text{coldStart}$)

### Table 1: Standard Abuser ($k=1.5, f=0.3, C=1000, \text{coldStart}$)
| Algorithm | Abuse Rej % | Diff vs TB-lg | Norm Rate / R | Diff vs TB-lg | Legit FRR % | Diff vs TB-lg | FRR Steady | Diff Steady | FRR Bursty | Diff Bursty | Total Admitted |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| *Ideal Contract Limiter* | *33.3%* | *ref* | *1.000* | *ref* | *0.00%* | *ref* | *0.00%* | *ref* | *0.00%* | *ref* | *—* |
| Token Bucket (TB-large) | 33.1% | +0.0% [+0.0, +0.0] | 1.006 | +0.000 [+0.000, +0.000] | 0.45% | +0.00% [+0.00, +0.00] | 0.00% | +0.00% [+0.00, +0.00] | 2.42% | +0.00% [+0.00, +0.00] | 14,378,148 |
| Token Bucket (TB-env, cap=9C/7) | 33.0% | -0.1% [-0.1, -0.1] | 1.008 | +0.002 [+0.002, +0.002] | 0.14% | -0.31% [-0.33, -0.29] | 0.00% | +0.00% [+0.00, +0.00] | 0.74% | -1.68% [-1.75, -1.60] | 14,410,250 |
| Token Bucket (TB-small) | 54.9% | +21.8% [+21.8, +21.8] | 0.678 | -0.328 [-0.328, -0.328] | 6.29% | +5.84% [+5.59, +6.08] | 0.00% | +0.00% [+0.00, +0.00] | 34.22% | +31.80% [+31.69, +31.90] | 11,601,371 |
| Leaky Bucket | 33.1% | +0.0% [+0.0, +0.0] | 1.006 | +0.000 [+0.000, +0.000] | 0.45% | +0.00% [+0.00, +0.00] | 0.00% | +0.00% [+0.00, +0.00] | 2.42% | +0.00% [+0.00, +0.00] | 14,378,148 |
| Fixed Window | 34.9% | +1.8% [+1.8, +1.8] | 0.979 | -0.027 [-0.027, -0.027] | 0.93% | +0.48% [+0.46, +0.51] | 0.00% | +0.00% [+0.00, +0.00] | 5.06% | +2.64% [+2.53, +2.74] | 14,151,705 |
| Sliding Window Log | 47.4% | +14.2% [+14.2, +14.2] | 0.792 | -0.214 [-0.214, -0.214] | 1.75% | +1.30% [+1.24, +1.35] | 0.00% | +0.00% [+0.00, +0.00] | 9.51% | +7.09% [+6.93, +7.24] | 12,743,749 |
| Sliding Window Counter | 50.6% | +17.5% [+17.5, +17.5] | 0.743 | -0.263 [-0.263, -0.263] | 1.49% | +1.04% [+0.99, +1.08] | 0.00% | +0.00% [+0.00, +0.01] | 8.07% | +5.65% [+5.51, +5.79] | 12,410,099 |
| DREB (proportional) | 44.7% | +11.6% [+11.6, +11.6] | 0.831 | -0.175 [-0.175, -0.175] | 1.17% | +0.72% [+0.69, +0.75] | 0.00% | +0.00% [+0.00, +0.00] | 6.35% | +3.93% [+3.84, +4.02] | 13,068,964 |
| DREB (burst-matched) | 37.4% | +4.3% [+4.3, +4.3] | 0.942 | -0.064 [-0.064, -0.064] | 0.34% | -0.11% [-0.12, -0.10] | 0.00% | +0.00% [+0.00, +0.00] | 1.82% | -0.60% [-0.66, -0.54] | 13,925,670 |
| DREB (v2-adaptive-base1.5) | 37.9% | +4.8% [+4.8, +4.8] | 0.934 | -0.072 [-0.072, -0.072] | 0.39% | -0.06% [-0.07, -0.05] | 0.00% | +0.00% [+0.00, +0.00] | 2.10% | -0.32% [-0.37, -0.27] | 13,865,165 |
| DREB (v2-adaptive-base1.0) | 34.5% | +1.3% [+1.3, +1.3] | 0.986 | -0.020 [-0.020, -0.020] | 0.25% | -0.19% [-0.21, -0.18] | 0.00% | +0.00% [+0.00, +0.00] | 1.37% | -1.05% [-1.10, -0.99] | 14,247,140 |

---

### Table 2: Reclassified Evasion Scenario: Compliant Bursty Traffic ($f=0.3, C=1000, \text{coldStart}$)
*Offered rate is $\approx 0.78R \le R$ (compliant). Blocked requests represent **false rejections** of compliant traffic.*

| Algorithm | Actual Offered Rate / R | Admitted Rate / R | Compliant False Rejection % | Diff vs TB-lg (95% CI) | Status Under Contract |
| :--- | :---: | :---: | :---: | :---: | :--- |
| Token Bucket (TB-large) | 0.783 | 0.783 | 0.0% | +0.0% [+0.0, +0.0] | Compliant (0% rejected) |
| Token Bucket (TB-env, cap=9C/7) | 1.007 | 1.007 | 0.0% | +0.0% [+0.0, +0.0] | Compliant (0% rejected) |
| Token Bucket (TB-small) | 0.335 | 0.335 | 0.0% | +0.0% [+0.0, +0.0] | Compliant (0% rejected) |
| Leaky Bucket | 0.783 | 0.783 | 0.0% | +0.0% [+0.0, +0.0] | Compliant (0% rejected) |
| Fixed Window | 0.783 | 0.783 | 0.0% | +0.0% [+0.0, +0.0] | Compliant (0% rejected) |
| Sliding Window Log | 0.783 | 0.783 | 0.0% | +0.0% [+0.0, +0.0] | Compliant (0% rejected) |
| Sliding Window Counter | 0.783 | 0.597 | 23.8% | +23.8% [+23.8, +23.8] | False Rejection (+23.8%) |
| DREB (proportional) | 0.609 | 0.605 | 0.7% | +0.7% [+0.7, +0.7] | False Rejection (+0.7%) |
| DREB (burst-matched) | 0.783 | 0.690 | 11.9% | +11.9% [+11.9, +11.9] | False Rejection (+11.9%) |
| DREB (v2-adaptive-base1.5) | 0.737 | 0.661 | 10.3% | +10.3% [+10.3, +10.3] | False Rejection (+10.3%) |
| DREB (v2-adaptive-base1.0) | 0.862 | 0.856 | 0.6% | +0.6% [+0.6, +0.6] | False Rejection (+0.6%) |

> **Note on Evasion Feasibility:** An abuser timing bursts at intervals $T_{\text{gap}} \approx \tau = \frac{9C}{7R} \approx 1.2857 \frac{C}{R}$ with burst size bounded by post-idle ceiling $B \le C_{\text{ceiling}}$ has an offered rate $\frac{B}{\tau} \le \frac{1.10 C}{1.2857 C/R} \approx 0.856 R$. It is mathematically impossible to offer rate $\ge 1.5 R$ under these timing and ceiling constraints.

---

### Table 3 (A2): Does DREB's Lower Legit FRR Survive Against TB-env?
*Paired differences of DREB variants against TB-env (capacity $= 9C/7 \approx 1286$ for BM and v2; capacity $= C$ for proportional).*

| Algorithm | Legit FRR (Overall) | Paired Diff vs TB-env (95% CI) | Legit FRR (Bursty) | Paired Diff vs TB-env (95% CI) | Does FRR Advantage Survive? |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **TB-env** | 0.14% | reference | 0.74% | reference | — |
| **DREB (burst-matched)** | 0.34% | **+0.20% [+0.18%, +0.22%]** | 1.82% | **+1.08% [+1.01%, +1.15%]** | **NO (Inverted: higher FRR)** |
| **DREB (v2 base 1.5)** | 0.38% | **+0.24% [+0.23%, +0.26%]** | 2.05% | **+1.31% [+1.24%, +1.38%]** | **NO (Inverted: higher FRR)** |
| **DREB (proportional)** | 1.17% | **+1.03% [+0.99%, +1.07%]** | 6.35% | **+5.61% [+5.48%, +5.74%]** | **NO (Inverted: higher FRR)** |

> **Critical Finding on Baseline Matching:** Against `TB-large` ($C=1000$), DREB burst-matched showed lower legitimate FRR (−0.11 pp overall, −0.60 pp bursty-legit). However, against `TB-env` whose capacity equals DREB's sustainedMax ($9C/7 \approx 1286$), DREB's lower false rejection **DOES NOT SURVIVE**. TB-env achieves an overall FRR of only 0.14% and bursty-legit FRR of 0.74%. DREB burst-matched has **higher** overall FRR (+0.20 pp) and **higher** bursty FRR (+1.08 pp) than TB-env. The apparent advantage over TB-large was an artifact of comparing DREB's 1286-envelope against a 1000-capacity Token Bucket.

---

### Table 4 (A5): DREB Admitted Rate vs TB-large and Contract ($C=1000, f=0.3, \text{coldStart}$)
*Shows paired difference in abuser admitted rate / $R$ vs `TB-large` with 95% bootstrap CI, and comparison against contracted rate $1.0R$.*

| DREB Variant | k | Admitted Rate / R | TB-large Rate / R | Diff vs TB-large (95% CI) | Status vs Contract (1.0R) |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **DREB (proportional)** | 1.10 | 0.725 | 1.002 | -0.276 [-0.276, -0.276] | Over-enforces: admits 27.5% below contracted rate |
| **DREB (proportional)** | 1.25 | 0.765 | 1.003 | -0.238 [-0.238, -0.238] | Over-enforces: admits 23.5% below contracted rate |
| **DREB (proportional)** | 1.50 | 0.831 | 1.006 | -0.175 [-0.175, -0.175] | Over-enforces: admits 16.9% below contracted rate |
| **DREB (proportional)** | 2.00 | 0.959 | 1.006 | -0.046 [-0.046, -0.046] | Over-enforces: admits 4.1% below contracted rate |
| **DREB (proportional)** | 4.00 | 1.008 | 1.008 | -0.000 [-0.000, -0.000] | Admits contracted rate |
| **DREB (burst-matched)** | 1.10 | 0.806 | 1.002 | -0.195 [-0.195, -0.195] | Over-enforces: admits 19.4% below contracted rate |
| **DREB (burst-matched)** | 1.25 | 0.857 | 1.003 | -0.146 [-0.146, -0.146] | Over-enforces: admits 14.3% below contracted rate |
| **DREB (burst-matched)** | 1.50 | 0.942 | 1.006 | -0.064 [-0.064, -0.064] | Over-enforces: admits 5.8% below contracted rate |
| **DREB (burst-matched)** | 2.00 | 1.008 | 1.006 | +0.002 [+0.002, +0.002] | Admits contracted rate |
| **DREB (burst-matched)** | 4.00 | 1.010 | 1.008 | +0.002 [+0.002, +0.002] | Admits contracted rate |
| **DREB (v2 base 1.5)** | 1.10 | 0.791 | 1.002 | -0.211 [-0.211, -0.211] | Over-enforces: admits 20.9% below contracted rate |
| **DREB (v2 base 1.5)** | 1.25 | 0.844 | 1.003 | -0.158 [-0.158, -0.158] | Over-enforces: admits 15.6% below contracted rate |
| **DREB (v2 base 1.5)** | 1.50 | 0.934 | 1.006 | -0.072 [-0.072, -0.072] | Over-enforces: admits 6.6% below contracted rate |
| **DREB (v2 base 1.5)** | 2.00 | 1.007 | 1.006 | +0.001 [+0.001, +0.001] | Admits contracted rate |
| **DREB (v2 base 1.5)** | 4.00 | 1.010 | 1.008 | +0.002 [+0.002, +0.002] | Admits contracted rate |
| **DREB (v2 base 1.0)** | 1.10 | 0.913 | 1.002 | -0.089 [-0.089, -0.089] | Over-enforces: admits 8.7% below contracted rate |
| **DREB (v2 base 1.0)** | 1.25 | 0.958 | 1.003 | -0.044 [-0.044, -0.044] | Over-enforces: admits 4.2% below contracted rate |
| **DREB (v2 base 1.0)** | 1.50 | 0.986 | 1.006 | -0.020 [-0.020, -0.020] | Over-enforces: admits 1.4% below contracted rate |
| **DREB (v2 base 1.0)** | 2.00 | 1.006 | 1.006 | +0.001 [+0.001, +0.001] | Admits contracted rate |
| **DREB (v2 base 1.0)** | 4.00 | 1.010 | 1.008 | +0.002 [+0.002, +0.002] | Admits contracted rate |

---

## 4. Hypothesis Verdicts Summary (Original Wording)

- **H2a: "DREB variants admit a smaller fraction of abusive traffic than TB-large without raising false rejection of legitimate clients above TB-large's."**
  - **DREB (burst-matched):** **SUPPORTED**. $\text{ARR} = 37.4\%$ vs $33.1\%$ ($+4.3\%$). Paired $\Delta \text{FRR} = -0.11\%$ with 95% bootstrap CI $[-0.12\%, -0.10\%]$ (upper bound $< 0$; strictly lower false rejection than TB-large).
  - **DREB (v2 base 1.5):** **SUPPORTED**. $\text{ARR} = 37.9\%$ vs $33.1\%$ ($+4.8\%$). Paired $\Delta \text{FRR} = -0.06\%$ with 95% bootstrap CI $[-0.07\%, -0.05\%]$ (upper bound $< 0$; strictly lower false rejection than TB-large).
  - **DREB (v2 base 1.0):** **SUPPORTED**. $\text{ARR} = 34.5\%$ vs $33.1\%$ ($+1.3\%$). Paired $\Delta \text{FRR} = -0.19\%$ with 95% bootstrap CI $[-0.21\%, -0.18\%]$ (upper bound $< 0$).
  - **DREB (proportional):** **CONTRADICTED**. While $\text{ARR} = 44.7\%$ ($+11.6\%$), its paired $\Delta \text{FRR} = +0.72\%$ with 95% bootstrap CI $[+0.69\%, +0.75\%]$ (strictly $> 0$). Under the original strict wording, it raises false rejection above TB-large.

- **H2b: "v2 admits less abusive traffic than v1 at equal false-rejection cost."**
  - **DREB (v2 base 1.5):** **SUPPORTED**. At $k=1.5, f=0.3$, it admits $0.934 R$ vs $0.942 R$ ($\text{ARR} = 37.9\%$ vs $37.4\%$, $+0.5\%$ more abusive traffic rejected) at equivalent false-rejection cost ($0.39\%$ vs $0.34\%$, $\Delta = +0.05\%$).
  - **DREB (v2 base 1.0):** **CONTRADICTED**. Admits $0.986 R$ vs $0.942 R$ ($\text{ARR} = 34.5\%$ vs $37.4\%$, $-2.9\%$ less abusive traffic rejected).
  - **v1 variants:** **UNTESTED** (the hypothesis explicitly tests v2 against v1).

- **H2c: "no algorithm lets an abuser's long-run admitted rate exceed its cumulative bound."**
  - **SUPPORTED for all 4 DREB variants**. Evaluated against each algorithm's exact mathematical cumulative bound $(S_{\max} + R \cdot T) / (R \cdot T)$ per client:
    - TB-large bound: $(1000 + 120000) / 120000 = 1.00833$; observed $\le 1.008$.
    - TB-env bound: $(1286 + 120000) / 120000 = 1.0107$; observed $\le 1.008$.
    - DREB burst-matched bound: $(1286 + 120000) / 120000 = 1.0107$; observed $\le 1.010$.
    - DREB v2 base 1.5 bound: $(1286 + 120000) / 120000 = 1.0107$; observed $\le 1.010$.
    - DREB v2 base 1.0 bound: $(1286 + 120000) / 120000 = 1.0107$; observed $\le 1.010$.
    - DREB proportional bound: $(1000 + 120000) / 120000 = 1.00833$; observed $\le 1.008$.
