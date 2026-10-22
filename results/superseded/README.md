# Superseded Benchmark Results

The benchmark outputs contained in this directory (`20260901_144038` and `20260902_030333`) are archived and superseded.

## Rationale
These results were produced using the pre-fix prototype implementation of the DREB library prior to tag `v1.0.1-phase1`. Specifically, the following known defects affected the recorded metrics:

1. **Mid-gap idle-credit over-accrual (P1):** Idle credit was awarded across the entirety of $\Delta t$ whenever the burst bucket was full at the conclusion of the interval, rather than for the precise duration $t_{\text{full}}$ during which the bucket was full. This inflated idle credit on burst recovery.
2. **Large-request admission check (P4):** Admission evaluated `burstTokens + idleCredit >= cost` rather than `burstTokens + (idleCredit / decay) >= cost`, admitting single requests exceeding the configured burst ceiling in burst-matched configurations.
3. **Clock rollback timestamp regression (P7):** Clock rollbacks regressed `lastRefillTs` to `now`, enabling duplicate token refills once logical time advanced again.
4. **Unsynchronized client state updates (P2):** The standalone library lacked per-client synchronization, permitting lost updates under concurrent access.

All experiments are being rerun under the corrected library specification (`v1.0.1-phase1`). Do not cite or compare against the numbers in this directory.
