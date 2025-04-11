package com.example.recorder.replay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.recorder.model.RecordedPdu;
import com.example.recorder.storage.PduStorage;

import edu.nps.moves.dis.Pdu;

/**
 * Implementation of PduReplayer that uses a multicast socket to replay PDUs.
 */
public class MulticastPduReplayer implements PduReplayer {
    private static final Logger logger = LoggerFactory.getLogger(MulticastPduReplayer.class);
    private static final String DEFAULT_MULTICAST_GROUP = "239.1.2.30";
    private static final int DEFAULT_PORT = 3000;

    private final PduStorage storage;
    private final String multicastGroup;
    private final int port;
    private MulticastSocket socket;
    private final AtomicBoolean replaying = new AtomicBoolean(false);
    private String currentExerciseId;
    private double currentSpeedFactor;
    private ExecutorService executorService;

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
    public void startReplay(String exerciseId, double speedFactor) {
        if (replaying.compareAndSet(false, true)) {
            this.currentExerciseId = exerciseId;
            this.currentSpeedFactor = speedFactor;
            
            try {
                socket = new MulticastSocket();
                
                executorService = Executors.newSingleThreadExecutor();
                executorService.submit(() -> replayExercise(exerciseId, speedFactor));
                
                logger.info("Started replaying exercise: {} at {}x speed on {}:{}", 
                    exerciseId, speedFactor, multicastGroup, port);
            } catch (IOException e) {
                replaying.set(false);
                currentExerciseId = null;
                currentSpeedFactor = 0;
                logger.error("Failed to start replay: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start replay", e);
            }
        } else {
            logger.warn("Already replaying exercise: {}", currentExerciseId);
        }
    }

    @Override
    public void stopReplay() {
        if (replaying.compareAndSet(true, false)) {
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
     */
    private void replayExercise(String exerciseId, double speedFactor) {
        List<RecordedPdu> pdus = storage.getPdusForExercise(exerciseId);
        
        if (pdus.isEmpty()) {
            logger.warn("No PDUs found for exercise: {}", exerciseId);
            replaying.set(false);
            currentExerciseId = null;
            currentSpeedFactor = 0;
            return;
        }
        
        try {
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
                        break;
                    }
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
        } catch (IOException e) {
            if (replaying.get()) {
                logger.error("Error during replay: {}", e.getMessage(), e);
            }
        } finally {
            if (replaying.compareAndSet(true, false)) {
                logger.info("Finished replaying exercise: {}", exerciseId);
                currentExerciseId = null;
                currentSpeedFactor = 0;
                
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
            }
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
