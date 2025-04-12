package com.techtest.recorder.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.techtest.recorder.interfaces.PduAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techtest.recorder.interfaces.PduRecorder;
import com.techtest.recorder.interfaces.PduReplayer;
import com.techtest.recorder.interfaces.PduStorage;

/**
 * Controller for managing PDU recording and replay operations.
 */
public class RecorderController {
    private static final Logger logger = LoggerFactory.getLogger(RecorderController.class);
    
    private final PduStorage storage;
    private final PduRecorder recorder;
    private final PduReplayer replayer;
    
    /**
     * Create a new RecorderController.
     * 
     * @param storage The storage to use
     * @param recorder The recorder to use
     * @param replayer The replayer to use
     */
    public RecorderController(PduStorage storage, PduRecorder recorder, PduReplayer replayer) {
        this.storage = storage;
        this.recorder = recorder;
        this.replayer = replayer;
    }
    
    /**
     * Start recording PDUs for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     * @return true if recording started, false otherwise
     */
    public boolean startRecording(String exerciseId) {
        if (recorder.isRecording()) {
            logger.warn("Already recording exercise: {}", recorder.getCurrentExerciseId());
            return false;
        }
        
        if (exerciseId == null || exerciseId.trim().isEmpty()) {
            logger.error("Exercise ID cannot be null or empty");
            return false;
        }
        
        try {
            recorder.startRecording(exerciseId);
            logger.info("Started recording exercise: {}", exerciseId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to start recording: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Stop recording PDUs.
     * 
     * @return true if recording stopped, false otherwise
     */
    public boolean stopRecording() {
        if (!recorder.isRecording()) {
            logger.warn("Not currently recording");
            return false;
        }
        
        try {
            String exerciseId = recorder.getCurrentExerciseId();
            recorder.stopRecording();
            logger.info("Stopped recording exercise: {}", exerciseId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to stop recording: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Start replaying PDUs for a specific exercise.
     *
     * @param exerciseId The exercise ID
     * @param speedFactor The speed factor (e.g., 1.0 for normal speed, 2.0 for double speed)
     * @param onComplete Consumer to be called when replay completes naturally
     * @return true if replay started, false otherwise
     */
    public boolean startReplay(String exerciseId, double speedFactor, Consumer<Void> onComplete) {
        if (replayer.isReplaying()) {
            logger.warn("Already replaying exercise: {}", replayer.getCurrentExerciseId());
            return false;
        }
        
        if (exerciseId == null || exerciseId.trim().isEmpty()) {
            logger.error("Exercise ID cannot be null or empty");
            return false;
        }
        
        if (speedFactor <= 0) {
            logger.error("Speed factor must be positive");
            return false;
        }
        
        try {
            CompletableFuture<Void> future = replayer.startReplay(exerciseId, speedFactor);
            
            // Set up callback for when replay completes naturally
            future.thenAccept(onComplete)
                  .exceptionally(ex -> {
                      logger.error("Error during replay: {}", ex.getMessage(), ex);
                      return null;
                  });
                  
            logger.info("Started replaying exercise: {} at {}x speed", exerciseId, speedFactor);
            return true;
        } catch (Exception e) {
            logger.error("Failed to start replay: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Start replaying PDUs for a specific exercise without completion callback.
     *
     * @param exerciseId The exercise ID
     * @param speedFactor The speed factor (e.g., 1.0 for normal speed, 2.0 for double speed)
     * @return true if replay started, false otherwise
     */
    public boolean startReplay(String exerciseId, double speedFactor) {
        return startReplay(exerciseId, speedFactor, null);
    }
    
    /**
     * Stop replaying PDUs.
     *
     * @param onStopped Consumer to be called when replay is fully stopped
     * @return true if stop request was initiated, false otherwise
     */
    public boolean stopReplay(Consumer<Void> onStopped) {
        if (!replayer.isReplaying()) {
            logger.warn("Not currently replaying");
            if (onStopped != null) {
                onStopped.accept(null);
            }
            return false;
        }
        
        try {
            String exerciseId = replayer.getCurrentExerciseId();
            logger.info("Initiating stop of replay for exercise: {}", exerciseId);
            
            // Get the future that represents the completion of the stop operation
            CompletableFuture<Void> future = replayer.stopReplay();
            
            // Set up callback for when stop completes
            // This ensures the GUI is only updated after replay is fully stopped
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Error stopping replay: {}", ex.getMessage(), ex);
                } else {
                    logger.info("Stopped replaying exercise: {}", exerciseId);
                }
                
                // In either case, invoke the callback to update the UI
                if (onStopped != null) {
                    onStopped.accept(null);
                }
            });
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to stop replay: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Stop replaying PDUs without completion callback.
     *
     * @return true if stop request was initiated, false otherwise
     */
    public boolean stopReplay() {
        return stopReplay(null);
    }
    
    /**
     * Clear all PDUs for a specific exercise.
     * 
     * @param exerciseId The exercise ID
     * @return true if exercise cleared, false otherwise
     */
    public boolean clearExercise(String exerciseId) {
        if (exerciseId == null || exerciseId.trim().isEmpty()) {
            logger.error("Exercise ID cannot be null or empty");
            return false;
        }
        
        try {
            storage.clearExercise(exerciseId);
            logger.info("Cleared exercise: {}", exerciseId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to clear exercise: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get all available exercise IDs.
     * 
     * @return List of exercise IDs
     */
    public List<String> getExerciseIds() {
        return storage.getExerciseIds();
    }
    
    /**
     * Check if recording is active.
     * 
     * @return true if recording, false otherwise
     */
    public boolean isRecording() {
        return recorder.isRecording();
    }
    
    /**
     * Get the current exercise ID being recorded.
     * 
     * @return The current exercise ID, or null if not recording
     */
    public String getCurrentRecordingExerciseId() {
        return recorder.getCurrentExerciseId();
    }
    
    /**
     * Check if replay is active.
     * 
     * @return true if replaying, false otherwise
     */
    public boolean isReplaying() {
        return replayer.isReplaying();
    }
    
    /**
     * Get the current exercise ID being replayed.
     * 
     * @return The current exercise ID, or null if not replaying
     */
    public String getCurrentReplayExerciseId() {
        return replayer.getCurrentExerciseId();
    }
    
    /**
     * Get the current replay speed factor.
     * 
     * @return The current speed factor, or 0 if not replaying
     */
    public double getCurrentReplaySpeedFactor() {
        return replayer.getCurrentSpeedFactor();
    }
    
    /**
     * Add an analyzer to the recorder.
     * 
     * @param analyzer The analyzer to add
     * @return true if analyzer added, false otherwise
     */
    public boolean addAnalyzer(PduAnalyzer analyzer) {
        if (analyzer == null) {
            logger.error("Analyzer cannot be null");
            return false;
        }
        
        try {
            recorder.addAnalyzer(analyzer);
            logger.info("Added analyzer: {}", analyzer.getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to add analyzer: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Remove an analyzer from the recorder.
     * 
     * @param analyzer The analyzer to remove
     * @return true if analyzer removed, false otherwise
     */
    public boolean removeAnalyzer(PduAnalyzer analyzer) {
        if (analyzer == null) {
            logger.error("Analyzer cannot be null");
            return false;
        }
        
        try {
            recorder.removeAnalyzer(analyzer);
            logger.info("Removed analyzer: {}", analyzer.getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove analyzer: {}", e.getMessage(), e);
            return false;
        }
    }
}
