package com.ratelimiter.util;

import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility for collecting system hardware information.
 */
public final class HardwareInfo {

    private HardwareInfo() {
        // Utility class
    }

    /**
     * Collects hardware and system metadata.
     *
     * @return a map containing the collected metadata
     */
    public static Map<String, Object> collect() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        
        metadata.put("timestamp", Instant.now().toString());
        
        String gitCommit = "unknown";
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse --short HEAD");
            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    gitCommit = reader.readLine();
                }
            }
        } catch (Exception ignored) {
        }
        metadata.put("gitCommit", gitCommit);
        
        metadata.put("javaVersion", System.getProperty("java.version"));
        metadata.put("jvmVendor", System.getProperty("java.vm.vendor"));
        metadata.put("jvmName", System.getProperty("java.vm.name"));
        metadata.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        
        String cpuModel = "unknown";
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                Process process = Runtime.getRuntime().exec("sysctl -n machdep.cpu.brand_string");
                if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        cpuModel = reader.readLine();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        metadata.put("cpuModel", cpuModel);
        metadata.put("cpuCores", Runtime.getRuntime().availableProcessors());
        
        long totalMemoryMb;
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            totalMemoryMb = osBean.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) {
            totalMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        }
        metadata.put("totalMemoryMb", totalMemoryMb);
        
        metadata.put("jvmMaxMemoryMb", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        
        return metadata;
    }
}
