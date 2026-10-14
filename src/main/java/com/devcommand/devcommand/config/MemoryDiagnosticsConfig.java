package com.devcommand.devcommand.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class MemoryDiagnosticsConfig {

    private static final Logger log = LoggerFactory.getLogger(MemoryDiagnosticsConfig.class);

    @EventListener(ApplicationReadyEvent.class)
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        log.info("=== DevCommand Memory Diagnostics ===");
        log.info("Max Heap: {} MB | Allocated Heap: {} MB | Used Heap: {} MB | Available Cores: {}",
                maxMemory, totalMemory, usedMemory, runtime.availableProcessors());
        log.info("======================================");
    }
}
