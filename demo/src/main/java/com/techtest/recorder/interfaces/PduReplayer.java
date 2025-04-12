package com.techtest.recorder.interfaces;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for PDU replay operations.
 */
public interface PduReplayer {
    /**
     * Start replaying PDUs for a specific exercise.
     *
     * @param exerciseId The exercise ID
     * @param speedFactor The speed factor (e.g., 1.0 for normal speed, 2.0 for double speed)
     * @return A CompletableFuture that completes when replay finishes (either naturally or by stopping)
     */
    CompletableFuture<Void> startReplay(String exerciseId, double speedFactor);
    
    /**
     * Stop replaying PDUs.
     *
     * @return A CompletableFuture that completes when the replay has fully stopped
     */
    CompletableFuture<Void> stopReplay();
    
    /**
     * Check if replay is active.
     *
     * @return true if replaying, false otherwise
     */
    boolean isReplaying();
    
    /**
     * Get the current exercise ID being replayed.
     *
     * @return The current exercise ID, or null if not replaying
     */
    String getCurrentExerciseId();
    
    /**
     * Get the current replay speed factor.
     *
     * @return The current speed factor, or 0 if not replaying
     */
    double getCurrentSpeedFactor();
}
