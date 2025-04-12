package com.example.recorder.storage;

import java.util.List;

import com.example.recorder.model.RecordedPdu;

/**
 * Interface for PDU storage operations.
 * This allows for different storage implementations (memory, file, database, etc.)
 */
public interface PduStorage {
    /**
     * Store a recorded PDU.
     * 
     * @param recordedPdu The PDU to store
     */
    void storePdu(RecordedPdu recordedPdu);
    
    /**
     * Retrieve all PDUs for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     * @return List of recorded PDUs for the exercise
     */
    List<RecordedPdu> getPdusForExercise(String exerciseId);
    
    /**
     * Clear all PDUs for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     */
    void clearExercise(String exerciseId);
    
    /**
     * Get all available exercise IDs.
     * 
     * @return List of exercise IDs
     */
    List<String> getExerciseIds();
}
