package com.example.recorder.recording;

import com.example.recorder.analysis.PduAnalyzer;

/**
 * Interface for PDU recording operations.
 */
public interface PduRecorder {
    /**
     * Start recording PDUs for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     */
    void startRecording(String exerciseId);
    
    /**
     * Stop recording PDUs.
     */
    void stopRecording();
    
    /**
     * Check if recording is active.
     * 
     * @return true if recording, false otherwise
     */
    boolean isRecording();
    
    /**
     * Get the current exercise ID being recorded.
     * 
     * @return The current exercise ID, or null if not recording
     */
    String getCurrentExerciseId();
    
    /**
     * Add an analyzer to the recorder.
     * 
     * @param analyzer The analyzer to add
     */
    void addAnalyzer(PduAnalyzer analyzer);
    
    /**
     * Remove an analyzer from the recorder.
     * 
     * @param analyzer The analyzer to remove
     */
    void removeAnalyzer(PduAnalyzer analyzer);
}
