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
- **Trade-off Plot:** FRR (x-axis) vs ARR (y-axis) for $C=1000$.
