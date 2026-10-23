# Experiment E6: Variable Cost Requests and Atomic Overdraw Evaluation

Evaluating Token Bucket, DREB burst-matched with P4 fix (`legacyOverdraw=false`), and DREB burst-matched with `legacyOverdraw=true` under variable cost requests drawn from $\{1, 5, 20, 60, 200\}$ at capacity $C = 1000$ across seeds 2000–2029 ($N=30$).

## 1. Executive Summary & Hypothesis Verdicts

### Hypothesis H6a: Strict Capacity Ceiling Under Variable Costs
> **Statement:** *under variable cost requests drawn from {1, 5, 20, 60, 200} at C = 1000, DREB burst-matched with the P4 fix (legacyOverdraw = false) never admits any single request whose cost exceeds available capacity, and the largest single admitted request never exceeds C.*

- **Largest Single Admitted Request under {1, 5, 20, 60, 200}:** **200** (Capacity C = 1000) — strictly $\le C$.
- **Post-Idle Capacity Boundary Check (Request Cost = 1200 > C):** **REJECTED** (admitted = false) — strictly capped at $C$.
- **Verdict:** **SUPPORTED**.

### Hypothesis H6b: Overdraw Elimination
> **Statement:** *DREB burst-matched with legacyOverdraw = true admits requests when available tokens are strictly less than request cost, causing token balances to drop below zero or overdrawing capacity beyond C, whereas legacyOverdraw = false strictly eliminates overdraw beyond C.*

- **Post-Idle Overdraw Beyond C (Cost = 1200):**
  - `legacyOverdraw = true`: **ADMITTED** (overdraws beyond $C = 1000$, utilizing raw unscaled idle credit up to $9C/7 = 1286$).
  - `legacyOverdraw = false`: **REJECTED** (strictly enforces atomic capacity ceiling $\le C = 1000$).
- **Verdict:** **SUPPORTED**.

---

## 2. Post-Idle Single-Request Admission Boundary Test (Pre-Warmed, C = 1000)

| Limiter Implementation | Cost = 200 | Cost = 500 | Cost = 1000 (= C) | Cost = 1200 (> C) | C-Ceiling Enforced? |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Token Bucket                           | ALLOW | ALLOW | ALLOW | DENY | **Yes (<= C)** |
| DREB BM (P4 fix: legacyOverdraw=false) | ALLOW | ALLOW | ALLOW | DENY | **Yes (<= C)** |
| DREB BM (legacyOverdraw=true)          | ALLOW | ALLOW | ALLOW | **ALLOW (Overdraw!)** | *No (Overdraws to 9C/7)* |

---

## 3. Comparative Performance Overview under Variable Cost Traces

| Limiter Implementation | Admitted Cost / Offered Cost % | Largest Single Admitted Cost | Minimum Observed Balance |
| :--- | :---: | :---: | :---: |
| Token Bucket                           |  97.47% [ 97.41%,  97.53%] |  200 |       0 |    0.000 |
| DREB BM (P4 fix: legacyOverdraw=false) |  98.35% [ 98.30%,  98.42%] |  200 |       0 |    0.714 |
| DREB BM (legacyOverdraw=true)          |  98.35% [ 98.30%,  98.42%] |  200 |       0 |    0.714 |

---

## 3. False Rejection Rate (FRR) by Request Cost Class

| Limiter Implementation | Cost = 1 FRR % | Cost = 5 FRR % | Cost = 20 FRR % | Cost = 60 FRR % | Cost = 200 FRR % |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Token Bucket                           |  0.00% |  0.00% |  0.00% |  0.00% |  3.61% |
| DREB BM (P4 fix: legacyOverdraw=false) |  0.00% |  0.00% |  0.00% |  0.00% |  2.35% |
| DREB BM (legacyOverdraw=true)          |  0.00% |  0.00% |  0.00% |  0.00% |  2.35% |
