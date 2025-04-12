package com.techtest.recorder.replay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techtest.recorder.model.RecordedPdu;
import com.techtest.recorder.storage.PduStorage;

import edu.nps.moves.dis.Pdu;

/**
 * Implementation of PduReplayer that uses a multicast socket to replay PDUs.
 */
public class MulticastPduReplayer implements PduReplayer {
    private static final Logger logger = LoggerFactory.getLogger(MulticastPduReplayer.class);
    private static final String DEFAULT_MULTICAST_GROUP = "239.1.2.3";
    private static final int DEFAULT_PORT = 3000;

    private final PduStorage storage;
    private final String multicastGroup;
    private final int port;
    private MulticastSocket socket;
    private final AtomicBoolean replaying = new AtomicBoolean(false);
    private String currentExerciseId;
    private double currentSpeedFactor;
    private ExecutorService executorService;
    private CompletableFuture<Void> replayFuture;

    /**
     * Create a new MulticastPduReplayer with default multicast group and port.
     * 
     * @param storage The storage to use for recorded PDUs
     */
    public MulticastPduReplayer(PduStorage storage) {
        this(storage, DEFAULT_MULTICAST_GROUP, DEFAULT_PORT);
    }

    /**
     * Create a new MulticastPduReplayer with custom multicast group and port.
     * 
     * @param storage The storage to use for recorded PDUs
     * @param multicastGroup The multicast group to join
     * @param port The port to send on
     */
    public MulticastPduReplayer(PduStorage storage, String multicastGroup, int port) {
        this.storage = storage;
        this.multicastGroup = multicastGroup;
        this.port = port;
    }

    @Override
    public CompletableFuture<Void> startReplay(String exerciseId, double speedFactor) {
        // Create a new future if none exists or if the previous one is completed
        if (replayFuture == null || replayFuture.isDone()) {
            replayFuture = new CompletableFuture<>();
        } else {
            // Return the existing future if replay is already in progress
            logger.warn("Already replaying exercise: {}", currentExerciseId);
            return replayFuture;
        }
        
        if (replaying.compareAndSet(false, true)) {
            this.currentExerciseId = exerciseId;
            this.currentSpeedFactor = speedFactor;
            
            try {
                socket = new MulticastSocket();
                
                executorService = Executors.newSingleThreadExecutor();
                executorService.submit(() -> replayExercise(exerciseId, speedFactor, replayFuture));
                
                logger.info("Started replaying exercise: {} at {}x speed on {}:{}",
                    exerciseId, speedFactor, multicastGroup, port);
            } catch (IOException e) {
                replaying.set(false);
                currentExerciseId = null;
                currentSpeedFactor = 0;
                replayFuture.completeExceptionally(e);
                logger.error("Failed to start replay: {}", e.getMessage(), e);
            }
        }
        
        return replayFuture;
    }

    @Override
    public CompletableFuture<Void> stopReplay() {
        // If we're not replaying, complete immediately
        if (!replaying.get()) {
            return CompletableFuture.completedFuture(null);
        }
        
        logger.info("Stopping replay of exercise: {}", currentExerciseId);
        
        // Set the replaying flag to false to signal the replay thread to stop
        replaying.set(false);
        
        // We'll use the existing replayFuture as the point of synchronization
        // This ensures that both natural completion and user-initiated stop
        // complete through the same future
        CompletableFuture<Void> stopFuture;
        
        // If we have an active replay future, use it to coordinate shutdown
        if (replayFuture != null && !replayFuture.isDone()) {
            // Return a new future that completes when the replay future completes
            stopFuture = replayFuture.thenApply(result -> null);
        } else {
            // If no active replay future, clean up and return completed future
            cleanupResources();
            stopFuture = CompletableFuture.completedFuture(null);
        }
        
        return stopFuture;
    }
    
    /**
     * Clean up resources after stopping replay.
     */
    private void cleanupResources() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        
        if (socket != null) {
            socket.close();
            socket = null;
        }
        
        logger.info("Stopped replaying exercise: {}", currentExerciseId);
        currentExerciseId = null;
        currentSpeedFactor = 0;
    }

    @Override
    public boolean isReplaying() {
        return replaying.get();
    }

    @Override
    public String getCurrentExerciseId() {
        return currentExerciseId;
    }

    @Override
    public double getCurrentSpeedFactor() {
        return currentSpeedFactor;
    }

    /**
     * Replay an exercise at the specified speed factor.
     *
     * @param exerciseId The exercise ID to replay
     * @param speedFactor The speed factor to replay at
     * @param future The CompletableFuture to complete when replay finishes
     */
    private void replayExercise(String exerciseId, double speedFactor, CompletableFuture<Void> future) {
        List<RecordedPdu> pdus = storage.getPdusForExercise(exerciseId);
        
        try {
            if (pdus.isEmpty()) {
                logger.warn("No PDUs found for exercise: {}", exerciseId);
                replaying.set(false);
                currentExerciseId = null;
                currentSpeedFactor = 0;
                future.complete(null);
                return;
            }
            
            InetAddress group = InetAddress.getByName(multicastGroup);
            long startTime = System.currentTimeMillis();
            long firstPduTime = pdus.get(0).getTimestamp();
            
            for (int i = 0; i < pdus.size() && replaying.get(); i++) {
                RecordedPdu recordedPdu = pdus.get(i);
                Pdu pdu = recordedPdu.getPdu();
                
                // Calculate delay based on timestamps and speed factor
                long pduTimestamp = recordedPdu.getTimestamp();
                long elapsedRecordTime = pduTimestamp - firstPduTime;
                long targetReplayTime = (long) (elapsedRecordTime / speedFactor);
                long currentReplayTime = System.currentTimeMillis() - startTime;
                
                // Sleep if needed to maintain timing
                if (targetReplayTime > currentReplayTime) {
                    try {
                        Thread.sleep(targetReplayTime - currentReplayTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.info("Replay interrupted for exercise: {}", exerciseId);
                        break;
                    }
                }
                
                // Check if we've been asked to stop
                if (!replaying.get()) {
                    logger.info("Replay stopped for exercise: {}", exerciseId);
                    break;
                }
                
                // Send the PDU
                byte[] pduBytes = pduToBytes(pdu);
                if (pduBytes != null) {
                    DatagramPacket packet = new DatagramPacket(pduBytes, pduBytes.length, group, port);
                    socket.send(packet);
                    logger.debug("Replayed PDU type {} for exercise {}",
                        pdu.getClass().getSimpleName(), exerciseId);
                }
            }
            
            // Successfully completed all PDUs or was manually stopped
            logger.info("Finished replaying exercise: {}", exerciseId);
            
            // Always clean up resources before completing the future
            // This ensures resources are released before callbacks run
            cleanupResources();
            
            // Complete the future to signal that replay is done
            // This will trigger any waiting CompletableFutures from stopReplay()
            future.complete(null);
            
        } catch (IOException e) {
            if (replaying.get()) {
                logger.error("Error during replay: {}", e.getMessage(), e);
                cleanupResources();
                future.completeExceptionally(e);
            } else {
                // If we're not replaying, this is just a cancellation
                cleanupResources();
                future.complete(null);
            }
        } finally {
            replaying.set(false);
        }
    }
    
    /**
     * Convert a PDU to a byte array.
     * 
     * @param pdu The PDU to convert
     * @return The byte array, or null if conversion failed
     */
    private byte[] pduToBytes(Pdu pdu) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            pdu.marshal(dos);
            return baos.toByteArray();
        } catch (Exception e) {
            logger.warn("Error converting PDU to bytes: {}", e.getMessage());
            return null;
        }
    }
}
