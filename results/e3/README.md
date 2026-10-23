# Experiment E3: DREB v2 Ablation & Parameter Sweeps

## 1. Overview and Protocol
Experiment E3 investigates the individual mechanisms of DREB v2's adaptive penalties and sweeps hyperparameters.
- **Tuning Phase (Seeds 1000–1029):** Swept `sustainedCostGain` $\in \{0, 0.1, 0.35, 1.0\}$, `abuseIncrement` $\in \{0.5, 2, 8\}$, `abuseDecayRate` $\in \{0.25, 1.5, 6\}$, and `abuseGain` $\in \{0, 0.3, 1\}$.
- **Selection Rule (Fixed in Advance):** Maximize abuser rejection rate subject to legit $\text{FRR}_{\text{overall}} \le \text{FRR}_{\text{TB-large}} + 0.1\text{ percentage points}$.
- **Selected Setting:** `v2 selected-by-rule (abuseDecayRate=0.25)`.
  > **Note on Contract Enforcement:** While this setting maximizes abuser rejection (47.6%), it admits only **0.788 R** to a 1.5 R abuser (at $C=1000, \text{coldStart}$), meaning it **over-enforces below the contracted rate 1.0 R**.
- **Held-Out Test Phase (Seeds 2000–2029):** Independent evaluation of baselines, ablations, TB-env, and the chosen configuration.

---

## 2. Evaluation Results Summary (Held-Out Test Seeds 2000–2029)
*Scenario: $k = 1.5, f = 0.30$. TB-env capacity is $9C/7 \approx 1286$ for burst-matched and v2.*

| Configuration | Capacity | Initial State | Abuse Rej % | Norm Rate / R | Diff vs TB-lg | Diff vs TB-env | Diff vs v1 BM | Legit FRR % | Diff vs TB-lg | Diff vs TB-env | Diff vs v1 BM |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| TB-large | 1000 | coldStart | 33.1% | 1.006 | +0.000 [+0.000, +0.000] | -0.002 [-0.002, -0.002] | +0.064 [+0.064, +0.064] | 0.45% | +0.00% [+0.00%, +0.00%] | +0.31% [+0.29%, +0.33%] | +0.11% [+0.10, +0.12] |
| TB-env (cap=9C/7) | 1000 | coldStart | 33.0% | 1.008 | +0.002 [+0.002, +0.002] | +0.000 [+0.000, +0.000] | +0.066 [+0.066, +0.066] | 0.14% | -0.31% [-0.33%, -0.29%] | +0.00% [+0.00%, +0.00%] | -0.20% [-0.22, -0.18] |
| TB-small | 1000 | coldStart | 54.9% | 0.678 | -0.328 [-0.328, -0.328] | -0.330 [-0.330, -0.330] | -0.264 [-0.264, -0.264] | 6.29% | +5.84% [+5.62%, +6.06%] | +6.15% [+5.93%, +6.37%] | +5.95% [+5.73, +6.21] |
| v1 burst-matched | 1000 | coldStart | 37.4% | 0.942 | -0.064 [-0.064, -0.064] | -0.066 [-0.066, -0.066] | +0.000 [+0.000, +0.000] | 0.34% | -0.11% [-0.12%, -0.10%] | +0.20% [+0.18%, +0.22%] | +0.00% [+0.00, +0.00] |
| v2 base 1.5 full | 1000 | coldStart | 37.6% | 0.938 | -0.068 [-0.068, -0.068] | -0.070 [-0.070, -0.070] | -0.004 [-0.004, -0.004] | 0.38% | -0.07% [-0.08%, -0.06%] | +0.24% [+0.23%, +0.25%] | +0.04% [+0.04, +0.05] |
| v2 sustained-cost off | 1000 | coldStart | 37.6% | 0.938 | -0.068 [-0.068, -0.068] | -0.070 [-0.070, -0.070] | -0.004 [-0.004, -0.004] | 0.37% | -0.07% [-0.09%, -0.05%] | +0.23% [+0.22%, +0.24%] | +0.03% [+0.03, +0.04] |
| v2 decay scaling off | 1000 | coldStart | 37.4% | 0.942 | -0.064 [-0.064, -0.064] | -0.066 [-0.066, -0.066] | +0.000 [+0.000, +0.000] | 0.35% | -0.10% [-0.11%, -0.09%] | +0.21% [+0.19%, +0.23%] | +0.01% [+0.01, +0.01] |
| v2 both off | 1000 | coldStart | 37.4% | 0.942 | -0.064 [-0.064, -0.064] | -0.066 [-0.066, -0.066] | +0.000 [+0.000, +0.000] | 0.34% | -0.11% [-0.12%, -0.10%] | +0.20% [+0.18%, +0.22%] | +0.00% [+0.00, +0.00] |
| v2 selected-by-rule (abuseDecayRate=0.25) | 1000 | coldStart | 47.6% | 0.788 | -0.218 [-0.218, -0.218] | -0.220 [-0.220, -0.220] | -0.154 [-0.154, -0.154] | 0.52% | +0.07% [+0.06%, +0.08%] | +0.38% [+0.36%, +0.40%] | +0.18% [+0.17, +0.19] |
| TB-large | 1000 | preWarmed | 33.1% | 1.006 | +0.000 [+0.000, +0.000] | -0.002 [-0.002, -0.002] | +0.059 [+0.059, +0.059] | 0.45% | +0.00% [+0.00%, +0.00%] | +0.31% [+0.29%, +0.33%] | +0.14% [+0.12, +0.15] |
| TB-env (cap=9C/7) | 1000 | preWarmed | 33.0% | 1.008 | +0.002 [+0.002, +0.002] | +0.000 [+0.000, +0.000] | +0.061 [+0.061, +0.061] | 0.14% | -0.31% [-0.33%, -0.29%] | +0.00% [+0.00%, +0.00%] | -0.17% [-0.19, -0.16] |
| TB-small | 1000 | preWarmed | 54.9% | 0.678 | -0.328 [-0.328, -0.328] | -0.330 [-0.330, -0.330] | -0.269 [-0.269, -0.269] | 6.29% | +5.84% [+5.58%, +6.10%] | +6.15% [+5.91%, +6.39%] | +5.98% [+5.74, +6.21] |
| v1 burst-matched | 1000 | preWarmed | 37.1% | 0.947 | -0.059 [-0.059, -0.059] | -0.061 [-0.061, -0.061] | +0.000 [+0.000, +0.000] | 0.31% | -0.13% [-0.14%, -0.12%] | +0.17% [+0.16%, +0.18%] | +0.00% [+0.00, +0.00] |
| v2 base 1.5 full | 1000 | preWarmed | 37.3% | 0.942 | -0.063 [-0.063, -0.063] | -0.065 [-0.065, -0.065] | -0.005 [-0.005, -0.005] | 0.36% | -0.09% [-0.09%, -0.09%] | +0.22% [+0.21%, +0.23%] | +0.04% [+0.03, +0.05] |
| v2 sustained-cost off | 1000 | preWarmed | 37.3% | 0.942 | -0.063 [-0.063, -0.063] | -0.065 [-0.065, -0.065] | -0.005 [-0.005, -0.005] | 0.35% | -0.10% [-0.10%, -0.10%] | +0.21% [+0.20%, +0.22%] | +0.03% [+0.02, +0.04] |
| v2 decay scaling off | 1000 | preWarmed | 37.1% | 0.947 | -0.059 [-0.059, -0.059] | -0.061 [-0.061, -0.061] | +0.000 [+0.000, +0.000] | 0.33% | -0.12% [-0.13%, -0.11%] | +0.19% [+0.18%, +0.20%] | +0.01% [+0.01, +0.02] |
| v2 both off | 1000 | preWarmed | 37.1% | 0.947 | -0.059 [-0.059, -0.059] | -0.061 [-0.061, -0.061] | +0.000 [+0.000, +0.000] | 0.31% | -0.13% [-0.14%, -0.12%] | +0.17% [+0.16%, +0.18%] | +0.00% [+0.00, +0.00] |
| v2 selected-by-rule (abuseDecayRate=0.25) | 1000 | preWarmed | 47.6% | 0.789 | -0.217 [-0.217, -0.217] | -0.219 [-0.219, -0.219] | -0.158 [-0.158, -0.158] | 0.50% | +0.05% [+0.04%, +0.06%] | +0.36% [+0.34%, +0.38%] | +0.18% [+0.17, +0.19] |
| TB-large | 50000 | coldStart | 33.1% | 1.006 | +0.000 [+0.000, +0.000] | -0.002 [-0.002, -0.002] | +0.064 [+0.064, +0.064] | 0.45% | +0.00% [+0.00%, +0.00%] | +0.31% [+0.29%, +0.33%] | +0.11% [+0.10, +0.12] |
| TB-env (cap=9C/7) | 50000 | coldStart | 33.0% | 1.008 | +0.002 [+0.002, +0.002] | +0.000 [+0.000, +0.000] | +0.066 [+0.066, +0.066] | 0.14% | -0.31% [-0.33%, -0.29%] | +0.00% [+0.00%, +0.00%] | -0.20% [-0.22, -0.18] |
| TB-small | 50000 | coldStart | 54.9% | 0.679 | -0.327 [-0.327, -0.327] | -0.329 [-0.329, -0.329] | -0.263 [-0.263, -0.263] | 6.28% | +5.83% [+5.60%, +6.06%] | +6.14% [+5.91%, +6.37%] | +5.94% [+5.71, +6.17] |
| v1 burst-matched | 50000 | coldStart | 37.4% | 0.942 | -0.064 [-0.064, -0.064] | -0.066 [-0.066, -0.066] | +0.000 [+0.000, +0.000] | 0.34% | -0.11% [-0.12%, -0.10%] | +0.20% [+0.18%, +0.22%] | +0.00% [+0.00, +0.00] |
| v2 base 1.5 full | 50000 | coldStart | 37.9% | 0.935 | -0.071 [-0.071, -0.071] | -0.073 [-0.073, -0.073] | -0.007 [-0.007, -0.007] | 0.38% | -0.07% [-0.08%, -0.06%] | +0.24% [+0.23%, +0.25%] | +0.04% [+0.04, +0.04] |
| v2 sustained-cost off | 50000 | coldStart | 37.9% | 0.935 | -0.071 [-0.071, -0.071] | -0.073 [-0.073, -0.073] | -0.007 [-0.007, -0.007] | 0.37% | -0.08% [-0.09%, -0.07%] | +0.23% [+0.22%, +0.24%] | +0.03% [+0.03, +0.04] |
| v2 decay scaling off | 50000 | coldStart | 37.4% | 0.942 | -0.064 [-0.064, -0.064] | -0.066 [-0.066, -0.066] | +0.000 [+0.000, +0.000] | 0.35% | -0.10% [-0.11%, -0.09%] | +0.21% [+0.19%, +0.23%] | +0.01% [+0.01, +0.01] |
| v2 both off | 50000 | coldStart | 37.4% | 0.942 | -0.064 [-0.064, -0.064] | -0.066 [-0.066, -0.066] | +0.000 [+0.000, +0.000] | 0.34% | -0.11% [-0.12%, -0.10%] | +0.20% [+0.18%, +0.22%] | +0.00% [+0.00, +0.00] |
| v2 selected-by-rule (abuseDecayRate=0.25) | 50000 | coldStart | 47.6% | 0.788 | -0.218 [-0.218, -0.218] | -0.220 [-0.220, -0.220] | -0.154 [-0.154, -0.154] | 0.52% | +0.07% [+0.06%, +0.08%] | +0.38% [+0.36%, +0.40%] | +0.18% [+0.17, +0.19] |
| TB-large | 50000 | preWarmed | 33.1% | 1.006 | +0.000 [+0.000, +0.000] | -0.002 [-0.002, -0.002] | +0.059 [+0.059, +0.059] | 0.45% | +0.00% [+0.00%, +0.00%] | +0.31% [+0.29%, +0.33%] | +0.14% [+0.12, +0.15] |
| TB-env (cap=9C/7) | 50000 | preWarmed | 33.0% | 1.008 | +0.002 [+0.002, +0.002] | +0.000 [+0.000, +0.000] | +0.061 [+0.061, +0.061] | 0.14% | -0.31% [-0.33%, -0.29%] | +0.00% [+0.00%, +0.00%] | -0.17% [-0.19, -0.16] |
| TB-small | 50000 | preWarmed | 54.9% | 0.679 | -0.327 [-0.327, -0.327] | -0.329 [-0.329, -0.329] | -0.268 [-0.268, -0.268] | 6.28% | +5.83% [+5.59%, +6.07%] | +6.14% [+5.91%, +6.37%] | +5.97% [+5.73, +6.21] |
| v1 burst-matched | 50000 | preWarmed | 37.1% | 0.947 | -0.059 [-0.059, -0.059] | -0.061 [-0.061, -0.061] | +0.000 [+0.000, +0.000] | 0.31% | -0.13% [-0.14%, -0.12%] | +0.17% [+0.16%, +0.18%] | +0.00% [+0.00, +0.00] |
| v2 base 1.5 full | 50000 | preWarmed | 37.5% | 0.939 | -0.066 [-0.066, -0.066] | -0.068 [-0.068, -0.068] | -0.007 [-0.007, -0.007] | 0.36% | -0.09% [-0.09%, -0.09%] | +0.22% [+0.21%, +0.23%] | +0.04% [+0.03, +0.05] |
| v2 sustained-cost off | 50000 | preWarmed | 37.5% | 0.939 | -0.066 [-0.066, -0.066] | -0.068 [-0.068, -0.068] | -0.007 [-0.007, -0.007] | 0.35% | -0.10% [-0.10%, -0.10%] | +0.21% [+0.20%, +0.22%] | +0.03% [+0.02, +0.04] |
| v2 decay scaling off | 50000 | preWarmed | 37.1% | 0.947 | -0.059 [-0.059, -0.059] | -0.061 [-0.061, -0.061] | +0.000 [+0.000, +0.000] | 0.32% | -0.12% [-0.13%, -0.11%] | +0.19% [+0.18%, +0.20%] | +0.01% [+0.01, +0.02] |
| v2 both off | 50000 | preWarmed | 37.1% | 0.947 | -0.059 [-0.059, -0.059] | -0.061 [-0.061, -0.061] | +0.000 [+0.000, +0.000] | 0.31% | -0.13% [-0.14%, -0.12%] | +0.17% [+0.16%, +0.18%] | +0.00% [+0.00, +0.00] |
| v2 selected-by-rule (abuseDecayRate=0.25) | 50000 | preWarmed | 47.6% | 0.789 | -0.217 [-0.217, -0.217] | -0.219 [-0.219, -0.219] | -0.158 [-0.158, -0.158] | 0.50% | +0.05% [+0.04%, +0.06%] | +0.36% [+0.34%, +0.38%] | +0.18% [+0.17, +0.19] |

---

## 3. Generalization Across Offered Rates ($k$) and Abusive Fractions ($f$)
*Evaluates `v1 burst-matched`, `v2 base 1.5 full`, and `v2 selected-by-rule (abuseDecayRate=0.25)` across $k \in \{1.1, 1.25, 2.0, 4.0\}$ and $f \in \{0.10, 0.50\}$ on held-out seeds 2000–2029 ($C=1000, \text{coldStart}$). Bursty-legit FRR is reported separately.*

| Slice | Configuration | Norm Rate / R | Diff Rate vs v1 | Overall Legit FRR % | Diff FRR vs v1 (95% CI) | Bursty-Legit FRR % | Diff Bursty vs v1 (95% CI) |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| $f=0.10, k=1.10$ | v1 burst-matched | 0.806 | +0.000 [+0.000, +0.000] | 0.33% | +0.00% [+0.00%, +0.00%] | 1.81% | +0.00% [+0.00%, +0.00%] |
| $f=0.10, k=1.10$ | v2 base 1.5 full | 0.794 | -0.012 [-0.012, -0.012] | 0.38% | +0.04% [+0.04, +0.04%] | 2.04% | +0.23% [+0.22, +0.24%] |
| $f=0.10, k=1.10$ | v2 selected-by-rule | 0.610 | -0.197 [-0.197, -0.197] | 0.51% | +0.18% [+0.17, +0.18%] | 2.80% | +0.99% [+0.94, +1.03%] |
| $f=0.10, k=1.25$ | v1 burst-matched | 0.857 | +0.000 [+0.000, +0.000] | 0.33% | +0.00% [+0.00%, +0.00%] | 1.81% | +0.00% [+0.00%, +0.00%] |
| $f=0.10, k=1.25$ | v2 base 1.5 full | 0.848 | -0.009 [-0.009, -0.009] | 0.38% | +0.04% [+0.04, +0.04%] | 2.04% | +0.23% [+0.22, +0.24%] |
| $f=0.10, k=1.25$ | v2 selected-by-rule | 0.676 | -0.181 [-0.181, -0.181] | 0.51% | +0.18% [+0.17, +0.18%] | 2.80% | +0.99% [+0.94, +1.03%] |
| $f=0.10, k=2.00$ | v1 burst-matched | 1.008 | +0.000 [+0.000, +0.000] | 0.33% | +0.00% [+0.00%, +0.00%] | 1.81% | +0.00% [+0.00%, +0.00%] |
| $f=0.10, k=2.00$ | v2 base 1.5 full | 1.008 | -0.000 [-0.000, +0.000] | 0.38% | +0.04% [+0.04, +0.04%] | 2.04% | +0.23% [+0.22, +0.24%] |
| $f=0.10, k=2.00$ | v2 selected-by-rule | 1.001 | -0.007 [-0.007, -0.007] | 0.51% | +0.18% [+0.17, +0.18%] | 2.80% | +0.99% [+0.94, +1.03%] |
| $f=0.10, k=4.00$ | v1 burst-matched | 1.010 | +0.000 [+0.000, +0.000] | 0.33% | +0.00% [+0.00%, +0.00%] | 1.81% | +0.00% [+0.00%, +0.00%] |
| $f=0.10, k=4.00$ | v2 base 1.5 full | 1.010 | +0.000 [+0.000, +0.000] | 0.38% | +0.04% [+0.04, +0.04%] | 2.04% | +0.23% [+0.22, +0.24%] |
| $f=0.10, k=4.00$ | v2 selected-by-rule | 1.010 | +0.000 [+0.000, +0.000] | 0.51% | +0.18% [+0.17, +0.18%] | 2.80% | +0.99% [+0.94, +1.03%] |
| $f=0.50, k=1.10$ | v1 burst-matched | 0.806 | +0.000 [+0.000, +0.000] | 0.34% | +0.00% [+0.00%, +0.00%] | 1.83% | +0.00% [+0.00%, +0.00%] |
| $f=0.50, k=1.10$ | v2 base 1.5 full | 0.794 | -0.012 [-0.012, -0.012] | 0.38% | +0.04% [+0.04, +0.04%] | 2.07% | +0.24% [+0.23, +0.25%] |
| $f=0.50, k=1.10$ | v2 selected-by-rule | 0.610 | -0.197 [-0.197, -0.197] | 0.53% | +0.18% [+0.17, +0.19%] | 2.83% | +1.00% [+0.94, +1.04%] |
| $f=0.50, k=1.25$ | v1 burst-matched | 0.857 | +0.000 [+0.000, +0.000] | 0.34% | +0.00% [+0.00%, +0.00%] | 1.83% | +0.00% [+0.00%, +0.00%] |
| $f=0.50, k=1.25$ | v2 base 1.5 full | 0.848 | -0.009 [-0.009, -0.009] | 0.38% | +0.04% [+0.04, +0.04%] | 2.07% | +0.24% [+0.23, +0.25%] |
| $f=0.50, k=1.25$ | v2 selected-by-rule | 0.676 | -0.181 [-0.181, -0.181] | 0.53% | +0.18% [+0.17, +0.19%] | 2.83% | +1.00% [+0.94, +1.04%] |
| $f=0.50, k=2.00$ | v1 burst-matched | 1.008 | +0.000 [+0.000, +0.000] | 0.34% | +0.00% [+0.00%, +0.00%] | 1.83% | +0.00% [+0.00%, +0.00%] |
| $f=0.50, k=2.00$ | v2 base 1.5 full | 1.008 | -0.000 [-0.000, +0.000] | 0.38% | +0.04% [+0.04, +0.04%] | 2.07% | +0.24% [+0.23, +0.25%] |
| $f=0.50, k=2.00$ | v2 selected-by-rule | 1.001 | -0.007 [-0.007, -0.007] | 0.53% | +0.18% [+0.17, +0.19%] | 2.83% | +1.00% [+0.94, +1.04%] |
| $f=0.50, k=4.00$ | v1 burst-matched | 1.010 | +0.000 [+0.000, +0.000] | 0.34% | +0.00% [+0.00%, +0.00%] | 1.83% | +0.00% [+0.00%, +0.00%] |
| $f=0.50, k=4.00$ | v2 base 1.5 full | 1.010 | +0.000 [+0.000, +0.000] | 0.38% | +0.04% [+0.04, +0.04%] | 2.07% | +0.24% [+0.23, +0.25%] |
| $f=0.50, k=4.00$ | v2 selected-by-rule | 1.010 | +0.000 [+0.000, +0.000] | 0.53% | +0.18% [+0.17, +0.19%] | 2.83% | +1.00% [+0.94, +1.04%] |

> **Key Generalization Insight:** Because the tuning selection rule only constrained **overall** FRR ($\le \text{FRR}_{\text{TB-large}} + 0.1$ pp), the aggressive abuser penalty of `abuseDecayRate=0.25` causes bursty-legit clients to suffer a large collateral penalty: bursty-legit false rejection increases from **1.81%** under v1 to **2.80%** under the selected setting (+0.99 pp, an increase of >54% in false rejections).

---

## 4. Formal Hypotheses Evaluation (Strict Original Wording)

### Hypothesis H3a: "v2 admits less abuser traffic than v1 burst-matched at no higher legit FRR."
- **Verdict:** **CONTRADICTED**.
- **Evidence:**
  - **Abuser Admitted Rate Difference:** Full v2 base 1.5 admits less abuser traffic than v1 burst-matched: $0.938 R$ vs $0.942 R$ ($\Delta = -0.004 R$ with 95% bootstrap CI $[-0.004, -0.004]$).
  - **Legitimate False Rejection Rate Difference:** Full v2 has an overall legitimate FRR of $0.38\%$ compared to $0.34\%$ for v1 burst-matched, yielding a paired difference of $\Delta \text{FRR} = +0.04$ percentage points with paired 95% bootstrap CI **$[+0.04\%, +0.05\%] > 0$**.
  - Similarly, the tuning-selected configuration (`v2 selected-by-rule`) admits $0.788 R$ ($\Delta = -0.154 R$), but its legitimate FRR increases to $0.52\%$ ($\Delta \text{FRR} = +0.18$ pp with paired 95% CI $[+0.17\%, +0.19\%] > 0$).
  - Because the paired confidence interval of legitimate FRR is strictly positive, the strict condition *"at no higher legit FRR"* is violated.

### Hypothesis H3b: "both v2 adaptive terms (sustained-cost scaling, decay scaling) contribute."
- **Verdict:** **CONTRADICTED**.
- **Evidence:**
  - As shown in the ablation table, disabling sustained-cost scaling (`v2 sustained-cost off`) achieves an abusive rejection rate of **37.6%** and an abuser admitted rate of **0.938 R**, which is **identical** to full v2 (**37.6% ARR, 0.938 R**).
  - Conversely, disabling decay scaling (`v2 decay scaling off`) increases admitted rate to **0.942 R** and reduces ARR to **37.4%**, matching v1 burst-matched exactly.
  - The sustained-cost scaling term shows **no measurable effect** whatsoever. Direct mechanism tracing in Experiment E7 confirms that in DREB, rejections occur exclusively when the burst bucket and idle credit are exhausted, while the sustained bucket remains $\ge 53\%$ full at all times and never binds.
  - Consequently, only decay scaling contributes to abuse suppression; both terms do not contribute.

### Hypothesis H3c: "v2 with both adaptive terms off is identical to v1 burst-matched."
- **Verdict:** **SUPPORTED**.
- **Evidence:**
  - `v2 both off` and `v1 burst-matched` are bit-for-bit identical across all 30 held-out seeds for all metrics: abuser rejection (37.4% vs 37.4%), admitted rate (0.942 vs 0.942), and legitimate FRR (0.34% vs 0.34%), with maximum absolute difference $= 0.0000000$. Verified by unit test `V2BothOffSanityTest`.
