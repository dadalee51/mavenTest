package com.techtest.recorder.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techtest.recorder.model.RecordedPdu;

import edu.nps.moves.dis.Pdu;

/**
 * Sample implementation of PduAnalyzer that collects statistics about PDUs.
 */
public class StatisticsAnalyzer implements PduAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsAnalyzer.class);
    
    private final Map<Integer, AtomicInteger> pduTypeCount = new HashMap<>();
    private final AtomicInteger totalPdus = new AtomicInteger(0);
    private long startTime = 0;
    private long lastPduTime = 0;
    
    /**
     * Create a new StatisticsAnalyzer.
     */
    public StatisticsAnalyzer() {
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public void analyzePdu(RecordedPdu recordedPdu) {
        Pdu pdu = recordedPdu.getPdu();
        int pduType = pdu.getPduType();
        
        // Update PDU type count
        pduTypeCount.computeIfAbsent(pduType, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Update total PDU count
        int total = totalPdus.incrementAndGet();
        
        // Update last PDU time
        lastPduTime = recordedPdu.getTimestamp();
        
        // Log statistics periodically
        if (total % 100 == 0) {
            logStatistics();
        }
    }
    
    @Override
    public String getName() {
        return "Statistics Analyzer";
    }
    
    /**
     * Log current statistics.
     */
    public void logStatistics() {
        long duration = System.currentTimeMillis() - startTime;
        double pdusPerSecond = (double) totalPdus.get() / (duration / 1000.0);
        
        logger.info("PDU Statistics:");
        logger.info("  Total PDUs: {}", totalPdus.get());
        logger.info("  PDUs per second: {}", String.format("%.2f", pdusPerSecond));
        logger.info("  PDU types:");
        
        pduTypeCount.forEach((type, count) -> {
            logger.info("    Type {}: {} PDUs", type, count.get());
        });
    }
    
    /**
     * Get the total number of PDUs analyzed.
     * 
     * @return The total number of PDUs
     */
    public int getTotalPdus() {
        return totalPdus.get();
    }
    
    /**
     * Get the count of PDUs by type.
     * 
     * @return Map of PDU type to count
     */
    public Map<Integer, Integer> getPduTypeCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        pduTypeCount.forEach((type, count) -> counts.put(type, count.get()));
        return counts;
    }
    
    /**
     * Get the duration of analysis in milliseconds.
     * 
     * @return The duration in milliseconds
     */
    public long getDuration() {
        return lastPduTime - startTime;
    }
    
    /**
     * Reset the analyzer.
     */
    public void reset() {
        pduTypeCount.clear();
        totalPdus.set(0);
        startTime = System.currentTimeMillis();
        lastPduTime = 0;
    }
}
