# Experiment E11: Adaptive Credit-Farming Adversary Evaluation

## 1. Executive Summary & Pre-Registered Hypothesis Verdicts

Experiment E11 evaluates rate-limiting algorithms against targeted adversaries designed to exploit idle credit mechanisms and transient burst dynamics:
- **Environment:** Single client (`client_0`), $C = 1000, R = 1000$, $T = 120.0\,\text{s}$, initial states `coldStart` and `preWarmed`, $N = 30$ seeds (2000–2029).
- **Adversaries:** `periodic` (baseline abuser, burst $0.95C$ at $k=1.5$), `farmer-100` (idle $\ge \tau = 1.2857\,\text{s}$, fires full burst), `farmer-75` (interval $0.75\tau$, 75% credit), `farmer-50` (interval $0.50\tau$, 50% credit), `alternator` (10s Poisson $0.5R$ alternating with 10s burst train at $k=2$), and `jittered` (periodic abuser with $\pm 50\%$ interval jitter).

### Formal Hypothesis Verdicts (Strict Original Wording)

1. **Hypothesis H11a:** *"no adversary raises DREB's long-run admitted rate above its cumulative bound (Prop. 1)."*
   - **Verdict:** **SUPPORTED** (judged across 30 seeds, no tolerance margins).
   - **Finding:** Proposition 1 proves that cumulative admissions over duration $T$ cannot exceed $S_{\max} + R \cdot T = 121,285.7$ tokens (long-run admitted rate $/ R \le 1.010714$). Across all evaluated adversaries and initial states, DREB's maximum observed cumulative admission was **115,498 tokens** (rate $0.9625R$ under `jittered`, preWarmed, seed 2003), remaining strictly within the Proposition 1 envelope with a minimum safety margin of $-5,788$ tokens.

2. **Hypothesis H11b:** *"the farmer obtains strictly higher admitted rate from DREB than from the static bucket with B = 3C/7."*
   - **Verdict:** **SUPPORTED** (judged across all farmer variants and initial states by paired 95% bootstrap CIs).
   - **Finding:** Under `farmer-100`, DREB burst-matched admits $0.6900R$ (cold) / $0.6948R$ (pre) vs $0.3353R$ for `DualBucket(B=3C/7)` (paired $\Delta = +0.3547R$ $[+0.3547, +0.3547]$ and $+0.3595R$ $[+0.3595, +0.3595]$). Under `farmer-75`, DREB admits $0.7712R$ / $0.7760R$ vs $0.4458R$ (paired $\Delta = +0.3254R$ / $+0.3301R$). Under `farmer-50`, DREB admits $0.9336R$ / $0.9383R$ vs $0.6670R$ (paired $\Delta = +0.2666R$ / $+0.2714R$). DREB's idle credit allows the farming adversary to achieve substantially higher long-run throughput than under a static bucket restricted to $B=3C/7$.

3. **Hypothesis H11c:** *"DREB's peak window load under the farmer exceeds TB-large's."*
   - **Verdict:** **CONTRADICTED** (judged across all window sizes $W \in \{0.05, 0.10, 0.25\}\,C/R$ and initial states by paired 95% bootstrap CIs).
   - **Finding:** In `preWarmed`, DREB's peak short-window load ties TB-large identically across all windows ($\\Delta = +0.000C$ $[+0.000, +0.000]$) because TB-large admits $1.000C$ at $X=100\%$, $0.857C$ at $X=75\%$, and $0.714C$ at $X=50\%$, exactly matching DREB. In `coldStart`, DREB's peak load ($0.886C$, $0.744C$, $0.601C$) is strictly *lower* than TB-large's ($1.000C$, $0.857C$, $0.714C$) with paired differences $\Delta = -0.114C$ and $-0.113C$. Because H11c strictly claims that DREB's peak load *exceeds* TB-large's ($\Delta > 0$), and all 18 evaluation cells exhibit $\Delta \le 0$, H11c is strictly contradicted.

---

## 2. Table 11.1: Admitted Rate / R Across Adversaries and Algorithms

| State | Adversary | Algorithm | Offered Rate / R | Admitted Rate / R | Total Admitted Tokens |
| :--- | :--- | :--- | :---: | :---: | :---: |
| coldStart | periodic         | Token Bucket (TB-large)      |      1.504R |      1.0058R |     120700.0 |
| coldStart | periodic         | Token Bucket (TB-env)        |      1.504R |      1.0082R |     120986.0 |
| coldStart | periodic         | DREB (burst-matched)         |      1.504R |      0.9419R |     113028.0 |
| coldStart | periodic         | DREB (v2 base 1.5)           |      1.504R |      0.9356R |     112267.0 |
| coldStart | periodic         | DualBucket (B=3C/7, P=5R/3)  |      1.504R |      0.6777R |      81320.0 |
| coldStart | periodic         | DualBucket (B=0.85C, P=4.00R) |      1.504R |      1.0082R |     120985.0 |
| coldStart | periodic         | DualBucket (B=1.00C, P=4.00R) |      1.504R |      1.0082R |     120985.0 |
| coldStart | farmer_100       | Token Bucket (TB-large)      |      0.783R |      0.7833R |      94000.0 |
| coldStart | farmer_100       | Token Bucket (TB-env)        |      0.783R |      0.7833R |      94000.0 |
| coldStart | farmer_100       | DREB (burst-matched)         |      0.783R |      0.6900R |      82799.0 |
| coldStart | farmer_100       | DREB (v2 base 1.5)           |      0.783R |      0.6696R |      80348.0 |
| coldStart | farmer_100       | DualBucket (B=3C/7, P=5R/3)  |      0.783R |      0.3353R |      40232.0 |
| coldStart | farmer_100       | DualBucket (B=0.85C, P=4.00R) |      0.783R |      0.6658R |      79900.0 |
| coldStart | farmer_100       | DualBucket (B=1.00C, P=4.00R) |      0.783R |      0.7833R |      94000.0 |
| coldStart | farmer_75        | Token Bucket (TB-large)      |      0.893R |      0.8927R |     107125.0 |
| coldStart | farmer_75        | Token Bucket (TB-env)        |      0.893R |      0.8927R |     107125.0 |
| coldStart | farmer_75        | DREB (burst-matched)         |      0.893R |      0.7712R |      92542.0 |
| coldStart | farmer_75        | DREB (v2 base 1.5)           |      0.893R |      0.7557R |      90689.0 |
| coldStart | farmer_75        | DualBucket (B=3C/7, P=5R/3)  |      0.893R |      0.4458R |      53500.0 |
| coldStart | farmer_75        | DualBucket (B=0.85C, P=4.00R) |      0.893R |      0.8854R |     106250.0 |
| coldStart | farmer_75        | DualBucket (B=1.00C, P=4.00R) |      0.893R |      0.8927R |     107125.0 |
| coldStart | farmer_50        | Token Bucket (TB-large)      |      1.113R |      1.0048R |     120571.0 |
| coldStart | farmer_50        | Token Bucket (TB-env)        |      1.113R |      1.0071R |     120857.0 |
| coldStart | farmer_50        | DREB (burst-matched)         |      1.113R |      0.9336R |     112027.0 |
| coldStart | farmer_50        | DREB (v2 base 1.5)           |      1.113R |      0.9270R |     111244.0 |
| coldStart | farmer_50        | DualBucket (B=3C/7, P=5R/3)  |      1.113R |      0.6670R |      80036.0 |
| coldStart | farmer_50        | DualBucket (B=0.85C, P=4.00R) |      1.113R |      1.0071R |     120856.0 |
| coldStart | farmer_50        | DualBucket (B=1.00C, P=4.00R) |      1.113R |      1.0071R |     120856.0 |
| coldStart | alternator       | Token Bucket (TB-large)      |      1.295R |      0.7984R |      95813.3 |
| coldStart | alternator       | Token Bucket (TB-env)        |      1.295R |      0.8127R |      97529.3 |
| coldStart | alternator       | DREB (burst-matched)         |      1.295R |      0.8127R |      97523.3 |
| coldStart | alternator       | DREB (v2 base 1.5)           |      1.295R |      0.8053R |      96635.3 |
| coldStart | alternator       | DualBucket (B=3C/7, P=5R/3)  |      1.295R |      0.7206R |      86470.7 |
| coldStart | alternator       | DualBucket (B=0.85C, P=4.00R) |      1.295R |      0.8127R |      97523.3 |
| coldStart | alternator       | DualBucket (B=1.00C, P=4.00R) |      1.295R |      0.8127R |      97523.3 |
| coldStart | jittered         | Token Bucket (TB-large)      |      1.507R |      1.0057R |     120689.9 |
| coldStart | jittered         | Token Bucket (TB-env)        |      1.507R |      1.0081R |     120975.9 |
| coldStart | jittered         | DREB (burst-matched)         |      1.507R |      0.9425R |     113105.9 |
| coldStart | jittered         | DREB (v2 base 1.5)           |      1.507R |      0.9351R |     112212.3 |
| coldStart | jittered         | DualBucket (B=3C/7, P=5R/3)  |      1.507R |      0.6787R |      81448.4 |
| coldStart | jittered         | DualBucket (B=0.85C, P=4.00R) |      1.507R |      1.0080R |     120964.7 |
| coldStart | jittered         | DualBucket (B=1.00C, P=4.00R) |      1.507R |      1.0081R |     120974.9 |
| preWarmed | periodic         | Token Bucket (TB-large)      |      1.504R |      1.0058R |     120700.0 |
| preWarmed | periodic         | Token Bucket (TB-env)        |      1.504R |      1.0082R |     120986.0 |
| preWarmed | periodic         | DREB (burst-matched)         |      1.504R |      0.9467R |     113599.0 |
| preWarmed | periodic         | DREB (v2 base 1.5)           |      1.504R |      0.9401R |     112813.0 |
| preWarmed | periodic         | DualBucket (B=3C/7, P=5R/3)  |      1.504R |      0.6777R |      81320.0 |
| preWarmed | periodic         | DualBucket (B=0.85C, P=4.00R) |      1.504R |      1.0082R |     120985.0 |
| preWarmed | periodic         | DualBucket (B=1.00C, P=4.00R) |      1.504R |      1.0082R |     120985.0 |
| preWarmed | farmer_100       | Token Bucket (TB-large)      |      0.783R |      0.7833R |      94000.0 |
| preWarmed | farmer_100       | Token Bucket (TB-env)        |      0.783R |      0.7833R |      94000.0 |
| preWarmed | farmer_100       | DREB (burst-matched)         |      0.783R |      0.6948R |      83371.0 |
| preWarmed | farmer_100       | DREB (v2 base 1.5)           |      0.783R |      0.6740R |      80875.0 |
| preWarmed | farmer_100       | DualBucket (B=3C/7, P=5R/3)  |      0.783R |      0.3353R |      40232.0 |
| preWarmed | farmer_100       | DualBucket (B=0.85C, P=4.00R) |      0.783R |      0.6658R |      79900.0 |
| preWarmed | farmer_100       | DualBucket (B=1.00C, P=4.00R) |      0.783R |      0.7833R |      94000.0 |
| preWarmed | farmer_75        | Token Bucket (TB-large)      |      0.893R |      0.8927R |     107125.0 |
| preWarmed | farmer_75        | Token Bucket (TB-env)        |      0.893R |      0.8927R |     107125.0 |
| preWarmed | farmer_75        | DREB (burst-matched)         |      0.893R |      0.7760R |      93114.0 |
| preWarmed | farmer_75        | DREB (v2 base 1.5)           |      0.893R |      0.7604R |      91252.0 |
| preWarmed | farmer_75        | DualBucket (B=3C/7, P=5R/3)  |      0.893R |      0.4458R |      53500.0 |
| preWarmed | farmer_75        | DualBucket (B=0.85C, P=4.00R) |      0.893R |      0.8854R |     106250.0 |
| preWarmed | farmer_75        | DualBucket (B=1.00C, P=4.00R) |      0.893R |      0.8927R |     107125.0 |
| preWarmed | farmer_50        | Token Bucket (TB-large)      |      1.113R |      1.0048R |     120571.0 |
| preWarmed | farmer_50        | Token Bucket (TB-env)        |      1.113R |      1.0071R |     120857.0 |
| preWarmed | farmer_50        | DREB (burst-matched)         |      1.113R |      0.9383R |     112599.0 |
| preWarmed | farmer_50        | DREB (v2 base 1.5)           |      1.113R |      0.9317R |     111803.0 |
| preWarmed | farmer_50        | DualBucket (B=3C/7, P=5R/3)  |      1.113R |      0.6670R |      80036.0 |
| preWarmed | farmer_50        | DualBucket (B=0.85C, P=4.00R) |      1.113R |      1.0071R |     120856.0 |
| preWarmed | farmer_50        | DualBucket (B=1.00C, P=4.00R) |      1.113R |      1.0071R |     120856.0 |
| preWarmed | alternator       | Token Bucket (TB-large)      |      1.295R |      0.7984R |      95813.3 |
| preWarmed | alternator       | Token Bucket (TB-env)        |      1.295R |      0.8127R |      97529.3 |
| preWarmed | alternator       | DREB (burst-matched)         |      1.295R |      0.8127R |      97523.3 |
| preWarmed | alternator       | DREB (v2 base 1.5)           |      1.295R |      0.8053R |      96635.3 |
| preWarmed | alternator       | DualBucket (B=3C/7, P=5R/3)  |      1.295R |      0.7206R |      86470.7 |
| preWarmed | alternator       | DualBucket (B=0.85C, P=4.00R) |      1.295R |      0.8127R |      97523.3 |
| preWarmed | alternator       | DualBucket (B=1.00C, P=4.00R) |      1.295R |      0.8127R |      97523.3 |
| preWarmed | jittered         | Token Bucket (TB-large)      |      1.507R |      1.0057R |     120689.9 |
| preWarmed | jittered         | Token Bucket (TB-env)        |      1.507R |      1.0081R |     120975.9 |
| preWarmed | jittered         | DREB (burst-matched)         |      1.507R |      0.9473R |     113677.8 |
| preWarmed | jittered         | DREB (v2 base 1.5)           |      1.507R |      0.9397R |     112760.3 |
| preWarmed | jittered         | DualBucket (B=3C/7, P=5R/3)  |      1.507R |      0.6787R |      81448.4 |
| preWarmed | jittered         | DualBucket (B=0.85C, P=4.00R) |      1.507R |      1.0080R |     120964.7 |
| preWarmed | jittered         | DualBucket (B=1.00C, P=4.00R) |      1.507R |      1.0081R |     120974.9 |

---

## 3. Table 11.2: Peak Short-Window Admitted Cost (W in {0.05, 0.10, 0.25} C/R)

| State | Adversary | Algorithm | Peak (W=0.05s) | Peak (W=0.10s) | Peak (W=0.25s) |
| :--- | :--- | :--- | :---: | :---: | :---: |
| coldStart | periodic         | Token Bucket (TB-large)      |      0.950C |      0.950C |      0.950C |
| coldStart | periodic         | Token Bucket (TB-env)        |      0.950C |      0.950C |      0.950C |
| coldStart | periodic         | DREB (burst-matched)         |      0.597C |      0.597C |      0.597C |
| coldStart | periodic         | DREB (v2 base 1.5)           |      0.593C |      0.593C |      0.593C |
| coldStart | periodic         | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| coldStart | periodic         | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| coldStart | periodic         | DualBucket (B=1.00C, P=4.00R) |      0.950C |      0.950C |      0.950C |
| coldStart | farmer_100       | Token Bucket (TB-large)      |      1.000C |      1.000C |      1.000C |
| coldStart | farmer_100       | Token Bucket (TB-env)        |      1.000C |      1.000C |      1.000C |
| coldStart | farmer_100       | DREB (burst-matched)         |      0.886C |      0.886C |      0.886C |
| coldStart | farmer_100       | DREB (v2 base 1.5)           |      0.860C |      0.860C |      0.860C |
| coldStart | farmer_100       | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| coldStart | farmer_100       | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| coldStart | farmer_100       | DualBucket (B=1.00C, P=4.00R) |      1.000C |      1.000C |      1.000C |
| coldStart | farmer_75        | Token Bucket (TB-large)      |      0.857C |      0.857C |      0.857C |
| coldStart | farmer_75        | Token Bucket (TB-env)        |      0.857C |      0.857C |      0.857C |
| coldStart | farmer_75        | DREB (burst-matched)         |      0.744C |      0.744C |      0.744C |
| coldStart | farmer_75        | DREB (v2 base 1.5)           |      0.729C |      0.729C |      0.729C |
| coldStart | farmer_75        | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| coldStart | farmer_75        | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| coldStart | farmer_75        | DualBucket (B=1.00C, P=4.00R) |      0.857C |      0.857C |      0.857C |
| coldStart | farmer_50        | Token Bucket (TB-large)      |      0.714C |      0.714C |      0.714C |
| coldStart | farmer_50        | Token Bucket (TB-env)        |      0.714C |      0.714C |      0.714C |
| coldStart | farmer_50        | DREB (burst-matched)         |      0.601C |      0.601C |      0.601C |
| coldStart | farmer_50        | DREB (v2 base 1.5)           |      0.596C |      0.596C |      0.596C |
| coldStart | farmer_50        | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| coldStart | farmer_50        | DualBucket (B=0.85C, P=4.00R) |      0.714C |      0.714C |      0.714C |
| coldStart | farmer_50        | DualBucket (B=1.00C, P=4.00R) |      0.714C |      0.714C |      0.714C |
| coldStart | alternator       | Token Bucket (TB-large)      |      0.980C |      1.008C |      1.087C |
| coldStart | alternator       | Token Bucket (TB-env)        |      0.980C |      1.008C |      1.087C |
| coldStart | alternator       | DREB (burst-matched)         |      0.980C |      1.008C |      1.087C |
| coldStart | alternator       | DREB (v2 base 1.5)           |      0.980C |      1.008C |      1.087C |
| coldStart | alternator       | DualBucket (B=3C/7, P=5R/3)  |      0.461C |      0.488C |      0.571C |
| coldStart | alternator       | DualBucket (B=0.85C, P=4.00R) |      0.880C |      0.908C |      0.987C |
| coldStart | alternator       | DualBucket (B=1.00C, P=4.00R) |      0.980C |      1.008C |      1.087C |
| coldStart | jittered         | Token Bucket (TB-large)      |      0.950C |      0.950C |      0.950C |
| coldStart | jittered         | Token Bucket (TB-env)        |      0.950C |      0.950C |      0.950C |
| coldStart | jittered         | DREB (burst-matched)         |      0.739C |      0.739C |      0.739C |
| coldStart | jittered         | DREB (v2 base 1.5)           |      0.730C |      0.730C |      0.730C |
| coldStart | jittered         | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| coldStart | jittered         | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| coldStart | jittered         | DualBucket (B=1.00C, P=4.00R) |      0.950C |      0.950C |      0.950C |
| preWarmed | periodic         | Token Bucket (TB-large)      |      0.950C |      0.950C |      0.950C |
| preWarmed | periodic         | Token Bucket (TB-env)        |      0.950C |      0.950C |      0.950C |
| preWarmed | periodic         | DREB (burst-matched)         |      0.950C |      0.950C |      0.950C |
| preWarmed | periodic         | DREB (v2 base 1.5)           |      0.950C |      0.950C |      0.950C |
| preWarmed | periodic         | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| preWarmed | periodic         | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| preWarmed | periodic         | DualBucket (B=1.00C, P=4.00R) |      0.950C |      0.950C |      0.950C |
| preWarmed | farmer_100       | Token Bucket (TB-large)      |      1.000C |      1.000C |      1.000C |
| preWarmed | farmer_100       | Token Bucket (TB-env)        |      1.000C |      1.000C |      1.000C |
| preWarmed | farmer_100       | DREB (burst-matched)         |      1.000C |      1.000C |      1.000C |
| preWarmed | farmer_100       | DREB (v2 base 1.5)           |      0.955C |      0.955C |      0.955C |
| preWarmed | farmer_100       | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| preWarmed | farmer_100       | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| preWarmed | farmer_100       | DualBucket (B=1.00C, P=4.00R) |      1.000C |      1.000C |      1.000C |
| preWarmed | farmer_75        | Token Bucket (TB-large)      |      0.857C |      0.857C |      0.857C |
| preWarmed | farmer_75        | Token Bucket (TB-env)        |      0.857C |      0.857C |      0.857C |
| preWarmed | farmer_75        | DREB (burst-matched)         |      0.857C |      0.857C |      0.857C |
| preWarmed | farmer_75        | DREB (v2 base 1.5)           |      0.857C |      0.857C |      0.857C |
| preWarmed | farmer_75        | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| preWarmed | farmer_75        | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| preWarmed | farmer_75        | DualBucket (B=1.00C, P=4.00R) |      0.857C |      0.857C |      0.857C |
| preWarmed | farmer_50        | Token Bucket (TB-large)      |      0.714C |      0.714C |      0.714C |
| preWarmed | farmer_50        | Token Bucket (TB-env)        |      0.714C |      0.714C |      0.714C |
| preWarmed | farmer_50        | DREB (burst-matched)         |      0.714C |      0.714C |      0.714C |
| preWarmed | farmer_50        | DREB (v2 base 1.5)           |      0.714C |      0.714C |      0.714C |
| preWarmed | farmer_50        | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| preWarmed | farmer_50        | DualBucket (B=0.85C, P=4.00R) |      0.714C |      0.714C |      0.714C |
| preWarmed | farmer_50        | DualBucket (B=1.00C, P=4.00R) |      0.714C |      0.714C |      0.714C |
| preWarmed | alternator       | Token Bucket (TB-large)      |      0.980C |      1.008C |      1.087C |
| preWarmed | alternator       | Token Bucket (TB-env)        |      0.980C |      1.008C |      1.087C |
| preWarmed | alternator       | DREB (burst-matched)         |      0.980C |      1.008C |      1.087C |
| preWarmed | alternator       | DREB (v2 base 1.5)           |      0.980C |      1.008C |      1.087C |
| preWarmed | alternator       | DualBucket (B=3C/7, P=5R/3)  |      0.461C |      0.488C |      0.571C |
| preWarmed | alternator       | DualBucket (B=0.85C, P=4.00R) |      0.880C |      0.908C |      0.987C |
| preWarmed | alternator       | DualBucket (B=1.00C, P=4.00R) |      0.980C |      1.008C |      1.087C |
| preWarmed | jittered         | Token Bucket (TB-large)      |      0.950C |      0.950C |      0.950C |
| preWarmed | jittered         | Token Bucket (TB-env)        |      0.950C |      0.950C |      0.950C |
| preWarmed | jittered         | DREB (burst-matched)         |      0.950C |      0.950C |      0.950C |
| preWarmed | jittered         | DREB (v2 base 1.5)           |      0.950C |      0.950C |      0.950C |
| preWarmed | jittered         | DualBucket (B=3C/7, P=5R/3)  |      0.428C |      0.428C |      0.428C |
| preWarmed | jittered         | DualBucket (B=0.85C, P=4.00R) |      0.850C |      0.850C |      0.850C |
| preWarmed | jittered         | DualBucket (B=1.00C, P=4.00R) |      0.950C |      0.950C |      0.950C |

---

## 4. Table 11.3: Hypotheses H11a, H11b, and H11c Evaluation Summary

| Hypothesis | Pre-Registered Statement | Evaluation Metric | Measured Result / Paired Delta | Verdict |
| :--- | :--- | :--- | :--- | :---: |
| **H11a** | *"no adversary raises DREB's long-run admitted rate above its cumulative bound (Prop. 1)."* | Max Admitted Tokens vs Prop 1 Bound ($121,285.7$) | Max Tokens = $115,498$ (Rate $= 0.9625R \le 1.0107R$, Margin $-5,788$) | **SUPPORTED** |
| **H11b** | *"the farmer obtains strictly higher admitted rate from DREB than from the static bucket with B = 3C/7."* | Paired Delta Rate (DREB $-$ DB 3C/7) | $\Delta = +0.2666R$ to $+0.3595R$ across all farmer variants (95% CI > 0) | **SUPPORTED** |
| **H11c** | *"DREB's peak window load under the farmer exceeds TB-large's."* | Paired Delta Peak (DREB $-$ TB-lg) across $W$ | $\Delta = -0.114C$ to $-0.113C$ (coldStart), $\Delta = +0.000C$ (preWarmed) | **CONTRADICTED** |
