package com.example.recorder.analysis;

import com.example.recorder.model.RecordedPdu;

/**
 * Interface for PDU analysis components.
 * This allows for injecting analysis components into the recording process.
 */
public interface PduAnalyzer {
    /**
     * Analyze a recorded PDU.
     * 
     * @param recordedPdu The PDU to analyze
     */
    void analyzePdu(RecordedPdu recordedPdu);
    
    /**
     * Get the name of the analyzer.
     * 
     * @return The analyzer name
     */
    String getName();
}
