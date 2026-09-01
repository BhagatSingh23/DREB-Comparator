package com.ratelimiter.metrics;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects and computes CPU load metrics.
 * <p>
 * Limitation: CPU sampling is inherently imprecise, depends on OS scheduler,
 * and the sampling interval affects accuracy.
 * </p>
 */
public class CpuMetrics {

    private final List<Double> samples = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean isSampling = new AtomicBoolean(false);
    private Thread samplingThread;
    
    private com.sun.management.OperatingSystemMXBean getOsBean() {
        return (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    /**
     * Starts periodic sampling of process CPU load.
     * @param intervalMs sampling interval in milliseconds
     */
    public synchronized void startSampling(long intervalMs) {
        if (isSampling.get()) {
            return;
        }
        isSampling.set(true);
        samples.clear();
        
        samplingThread = new Thread(() -> {
            com.sun.management.OperatingSystemMXBean osBean = getOsBean();
            while (isSampling.get()) {
                double load = osBean.getProcessCpuLoad();
                if (load >= 0.0) {
                    samples.add(load);
                }
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CpuMetricsSampler");
        
        samplingThread.setDaemon(true);
        samplingThread.start();
    }

    /**
     * Stops the CPU sampling.
     */
    public synchronized void stopSampling() {
        isSampling.set(false);
        if (samplingThread != null) {
            samplingThread.interrupt();
            try {
                samplingThread.join(2000); // Wait for thread to stop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            samplingThread = null;
        }
    }

    /**
     * Returns the average process CPU load.
     * @return average CPU load (0.0 to 1.0)
     */
    public double getAverageCpuLoad() {
        if (samples.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        synchronized (samples) {
            for (Double sample : samples) {
                sum += sample;
            }
            return sum / samples.size();
        }
    }

    /**
     * Returns the peak process CPU load.
     * @return peak CPU load (0.0 to 1.0)
     */
    public double getPeakCpuLoad() {
        if (samples.isEmpty()) {
            return 0.0;
        }
        double peak = 0.0;
        synchronized (samples) {
            for (Double sample : samples) {
                if (sample > peak) {
                    peak = sample;
                }
            }
        }
        return peak;
    }

    /**
     * Returns a copy of the collected samples.
     * @return list of CPU load samples
     */
    public List<Double> getSamples() {
        synchronized (samples) {
            return new ArrayList<>(samples);
        }
    }
}
