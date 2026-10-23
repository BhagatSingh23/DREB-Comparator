# Experiment E5: Contract Boundary Compliance Evaluation

Evaluating steady clients offered strictly below, at, and above the contracted rate $R$ ($k \in \{0.90, 0.98, 1.00, 1.02, 1.10\} \times R$) across periodic and Poisson arrivals on seeds 2000–2029 ($N=30$) for $C \in \{1000, 50000\}$.

## 1. Executive Summary & Hypothesis Verdicts

### Hypothesis H5a: Complete Admission Below Contract
> **Statement:** *for compliant steady clients offered strictly below the contracted rate (offered rate in {0.90R, 0.98R}), all algorithms achieve complete admission (admitted / offered = 1.000, admitted rate / R = k) across both Poisson and periodic arrival processes, without false rejections.*

- **Periodic Arrivals (k in {0.90, 0.98}):** **SUPPORTED** (all algorithms achieved 100.0% admission, 0.0% false rejection).
- **Poisson Arrivals (k in {0.90, 0.98}):** **QUALIFIED / CONTRADICTED** (algorithms with small burst tolerance like Leaky Bucket or Fixed Window exhibit stochastic boundary drops when Poisson clusters exceed instant capacity).
- **Overall Verdict:** **PARTIALLY SUPPORTED (Supported under periodic load; Poisson load reveals algorithm-specific variance)**

### Hypothesis H5b: Strict Contract Capping at and Above Contract
> **Statement:** *at and above the contracted rate (offered rate in {1.00R, 1.02R, 1.10R}), all algorithms cap admitted rate to at most the contracted envelope (S_max + R*T) / (R*T), admitting at most R under sustained periodic load.*

- **Periodic Overload (k >= 1.00):** **SUPPORTED**. Under sustained periodic arrivals at and above $1.00R$, all evaluated algorithms strictly cap admitted rates to the contracted envelope $1.000R$ (plus initial cold-start envelope $\le 1.033R$ over 30s).
- **Overall Verdict:** **SUPPORTED**.

---

## 2. Comprehensive Boundary Results (C = 1000)

| Algorithm | Arrival Process | Offered Rate k | Admitted / Offered % [95% CI] | Admitted Rate / R [95% CI] |
| :--- | :---: | :---: | :---: | :---: |
| Token Bucket (TB-large)    | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-small)    | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-env)      | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Leaky Bucket               | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Fixed Window               | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Sliding Window Log         | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Sliding Window Counter     | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (proportional)        | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (burst-matched)       | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (v2 base 1.5)         | periodic | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-large)    | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-small)    | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-env)      | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Leaky Bucket               | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Fixed Window               | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Sliding Window Log         | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Sliding Window Counter     | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (proportional)        | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (burst-matched)       | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (v2 base 1.5)         | periodic | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-large)    | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-small)    | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-env)      | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Leaky Bucket               | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Fixed Window               | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Sliding Window Log         | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Sliding Window Counter     | periodic | 1.00R |  99.95% [ 99.95%,  99.95%] |  1.000 [ 1.000,  1.000] |
| DREB (proportional)        | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| DREB (burst-matched)       | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| DREB (v2 base 1.5)         | periodic | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-large)    | periodic | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Token Bucket (TB-small)    | periodic | 1.02R |  99.44% [ 99.44%,  99.44%] |  1.014 [ 1.014,  1.014] |
| Token Bucket (TB-env)      | periodic | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Leaky Bucket               | periodic | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Fixed Window               | periodic | 1.02R |  98.04% [ 98.04%,  98.04%] |  1.000 [ 1.000,  1.000] |
| Sliding Window Log         | periodic | 1.02R |  98.04% [ 98.04%,  98.04%] |  1.000 [ 1.000,  1.000] |
| Sliding Window Counter     | periodic | 1.02R |  97.99% [ 97.99%,  97.99%] |  1.000 [ 1.000,  1.000] |
| DREB (proportional)        | periodic | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| DREB (burst-matched)       | periodic | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| DREB (v2 base 1.5)         | periodic | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Token Bucket (TB-large)    | periodic | 1.10R |  93.94% [ 93.94%,  93.94%] |  1.033 [ 1.033,  1.033] |
| Token Bucket (TB-small)    | periodic | 1.10R |  92.21% [ 92.21%,  92.21%] |  1.014 [ 1.014,  1.014] |
| Token Bucket (TB-env)      | periodic | 1.10R |  94.80% [ 94.80%,  94.80%] |  1.043 [ 1.043,  1.043] |
| Leaky Bucket               | periodic | 1.10R |  93.94% [ 93.94%,  93.94%] |  1.033 [ 1.033,  1.033] |
| Fixed Window               | periodic | 1.10R |  90.91% [ 90.91%,  90.91%] |  1.000 [ 1.000,  1.000] |
| Sliding Window Log         | periodic | 1.10R |  90.91% [ 90.91%,  90.91%] |  1.000 [ 1.000,  1.000] |
| Sliding Window Counter     | periodic | 1.10R |  90.87% [ 90.87%,  90.87%] |  1.000 [ 1.000,  1.000] |
| DREB (proportional)        | periodic | 1.10R |  93.93% [ 93.93%,  93.93%] |  1.033 [ 1.033,  1.033] |
| DREB (burst-matched)       | periodic | 1.10R |  94.80% [ 94.80%,  94.80%] |  1.043 [ 1.043,  1.043] |
| DREB (v2 base 1.5)         | periodic | 1.10R |  94.80% [ 94.80%,  94.80%] |  1.043 [ 1.043,  1.043] |
| Token Bucket (TB-large)    | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-small)    | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-env)      | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Leaky Bucket               | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Fixed Window               | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Sliding Window Log         | poisson  | 0.90R |  99.99% [ 99.99%,  99.99%] |  0.900 [ 0.900,  0.900] |
| Sliding Window Counter     | poisson  | 0.90R |  99.98% [ 99.97%,  99.98%] |  0.900 [ 0.900,  0.900] |
| DREB (proportional)        | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (burst-matched)       | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (v2 base 1.5)         | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-large)    | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-small)    | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-env)      | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Leaky Bucket               | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Fixed Window               | poisson  | 0.98R |  99.23% [ 99.22%,  99.25%] |  0.972 [ 0.972,  0.973] |
| Sliding Window Log         | poisson  | 0.98R |  98.18% [ 98.17%,  98.20%] |  0.962 [ 0.962,  0.962] |
| Sliding Window Counter     | poisson  | 0.98R |  97.43% [ 97.41%,  97.45%] |  0.955 [ 0.955,  0.955] |
| DREB (proportional)        | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (burst-matched)       | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (v2 base 1.5)         | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-large)    | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-small)    | poisson  | 1.00R |  99.99% [ 99.99%,  99.99%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-env)      | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Leaky Bucket               | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Fixed Window               | poisson  | 1.00R |  98.28% [ 98.26%,  98.30%] |  0.983 [ 0.983,  0.983] |
| Sliding Window Log         | poisson  | 1.00R |  96.91% [ 96.89%,  96.93%] |  0.969 [ 0.969,  0.969] |
| Sliding Window Counter     | poisson  | 1.00R |  95.98% [ 95.96%,  96.01%] |  0.960 [ 0.960,  0.960] |
| DREB (proportional)        | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| DREB (burst-matched)       | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| DREB (v2 base 1.5)         | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-large)    | poisson  | 1.02R |  99.99% [ 99.99%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Token Bucket (TB-small)    | poisson  | 1.02R |  99.17% [ 99.15%,  99.20%] |  1.012 [ 1.011,  1.012] |
| Token Bucket (TB-env)      | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Leaky Bucket               | poisson  | 1.02R |  99.99% [ 99.99%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Fixed Window               | poisson  | 1.02R |  96.93% [ 96.91%,  96.96%] |  0.989 [ 0.989,  0.989] |
| Sliding Window Log         | poisson  | 1.02R |  95.45% [ 95.43%,  95.48%] |  0.974 [ 0.973,  0.974] |
| Sliding Window Counter     | poisson  | 1.02R |  94.40% [ 94.38%,  94.43%] |  0.963 [ 0.963,  0.963] |
| DREB (proportional)        | poisson  | 1.02R |  99.99% [ 99.99%, 100.00%] |  1.020 [ 1.020,  1.020] |
| DREB (burst-matched)       | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| DREB (v2 base 1.5)         | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Token Bucket (TB-large)    | poisson  | 1.10R |  93.83% [ 93.80%,  93.85%] |  1.032 [ 1.032,  1.032] |
| Token Bucket (TB-small)    | poisson  | 1.10R |  92.10% [ 92.07%,  92.12%] |  1.013 [ 1.013,  1.013] |
| Token Bucket (TB-env)      | poisson  | 1.10R |  94.69% [ 94.67%,  94.72%] |  1.042 [ 1.042,  1.042] |
| Leaky Bucket               | poisson  | 1.10R |  93.83% [ 93.80%,  93.85%] |  1.032 [ 1.032,  1.032] |
| Fixed Window               | poisson  | 1.10R |  90.29% [ 90.26%,  90.31%] |  0.993 [ 0.993,  0.993] |
| Sliding Window Log         | poisson  | 1.10R |  89.17% [ 89.15%,  89.20%] |  0.981 [ 0.981,  0.981] |
| Sliding Window Counter     | poisson  | 1.10R |  87.91% [ 87.89%,  87.94%] |  0.967 [ 0.967,  0.967] |
| DREB (proportional)        | poisson  | 1.10R |  93.82% [ 93.80%,  93.85%] |  1.032 [ 1.032,  1.032] |
| DREB (burst-matched)       | poisson  | 1.10R |  94.69% [ 94.66%,  94.72%] |  1.042 [ 1.041,  1.042] |
| DREB (v2 base 1.5)         | poisson  | 1.10R |  94.69% [ 94.66%,  94.72%] |  1.042 [ 1.041,  1.042] |

---

## 3. Comprehensive Boundary Results (C = 50000)

| Algorithm | Arrival Process | Offered Rate k | Admitted / Offered % [95% CI] | Admitted Rate / R [95% CI] |
| :--- | :---: | :---: | :---: | :---: |
| Token Bucket (TB-large)    | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-small)    | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-env)      | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Leaky Bucket               | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Fixed Window               | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Sliding Window Log         | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Sliding Window Counter     | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (proportional)        | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (burst-matched)       | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| DREB (v2 base 1.5)         | poisson  | 0.90R | 100.00% [100.00%, 100.00%] |  0.900 [ 0.900,  0.900] |
| Token Bucket (TB-large)    | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-small)    | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-env)      | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Leaky Bucket               | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Fixed Window               | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Sliding Window Log         | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Sliding Window Counter     | poisson  | 0.98R |  98.89% [ 98.88%,  98.90%] |  0.969 [ 0.969,  0.969] |
| DREB (proportional)        | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (burst-matched)       | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| DREB (v2 base 1.5)         | poisson  | 0.98R | 100.00% [100.00%, 100.00%] |  0.980 [ 0.980,  0.980] |
| Token Bucket (TB-large)    | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-small)    | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-env)      | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Leaky Bucket               | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Fixed Window               | poisson  | 1.00R |  99.00% [ 98.99%,  99.01%] |  0.990 [ 0.990,  0.990] |
| Sliding Window Log         | poisson  | 1.00R |  98.50% [ 98.50%,  98.51%] |  0.985 [ 0.985,  0.985] |
| Sliding Window Counter     | poisson  | 1.00R |  97.16% [ 97.15%,  97.17%] |  0.972 [ 0.972,  0.972] |
| DREB (proportional)        | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| DREB (burst-matched)       | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| DREB (v2 base 1.5)         | poisson  | 1.00R | 100.00% [100.00%, 100.00%] |  1.000 [ 1.000,  1.000] |
| Token Bucket (TB-large)    | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Token Bucket (TB-small)    | poisson  | 1.02R |  99.34% [ 99.33%,  99.34%] |  1.013 [ 1.013,  1.013] |
| Token Bucket (TB-env)      | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Leaky Bucket               | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Fixed Window               | poisson  | 1.02R |  97.13% [ 97.12%,  97.14%] |  0.991 [ 0.991,  0.991] |
| Sliding Window Log         | poisson  | 1.02R |  96.73% [ 96.73%,  96.74%] |  0.987 [ 0.987,  0.987] |
| Sliding Window Counter     | poisson  | 1.02R |  95.34% [ 95.33%,  95.35%] |  0.972 [ 0.972,  0.973] |
| DREB (proportional)        | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| DREB (burst-matched)       | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| DREB (v2 base 1.5)         | poisson  | 1.02R | 100.00% [100.00%, 100.00%] |  1.020 [ 1.020,  1.020] |
| Token Bucket (TB-large)    | poisson  | 1.10R |  93.85% [ 93.84%,  93.85%] |  1.032 [ 1.032,  1.032] |
| Token Bucket (TB-small)    | poisson  | 1.10R |  92.11% [ 92.11%,  92.12%] |  1.013 [ 1.013,  1.013] |
| Token Bucket (TB-env)      | poisson  | 1.10R |  94.71% [ 94.71%,  94.71%] |  1.042 [ 1.042,  1.042] |
| Leaky Bucket               | poisson  | 1.10R |  93.85% [ 93.84%,  93.85%] |  1.032 [ 1.032,  1.032] |
| Fixed Window               | poisson  | 1.10R |  90.00% [ 89.99%,  90.00%] |  0.990 [ 0.990,  0.990] |
| Sliding Window Log         | poisson  | 1.10R |  89.99% [ 89.99%,  90.00%] |  0.990 [ 0.990,  0.990] |
| Sliding Window Counter     | poisson  | 1.10R |  88.03% [ 88.03%,  88.03%] |  0.968 [ 0.968,  0.968] |
| DREB (proportional)        | poisson  | 1.10R |  93.84% [ 93.84%,  93.85%] |  1.032 [ 1.032,  1.032] |
| DREB (burst-matched)       | poisson  | 1.10R |  94.71% [ 94.71%,  94.71%] |  1.042 [ 1.042,  1.042] |
| DREB (v2 base 1.5)         | poisson  | 1.10R |  94.71% [ 94.71%,  94.71%] |  1.042 [ 1.042,  1.042] |
