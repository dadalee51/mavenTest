package com.example.recorder.recording;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.recorder.analysis.PduAnalyzer;
import com.example.recorder.model.RecordedPdu;
import com.example.recorder.storage.PduStorage;

import edu.nps.moves.dis.Pdu;
import edu.nps.moves.disenum.PduType;

/**
 * Implementation of PduRecorder that uses a multicast socket to record PDUs.
 */
public class MulticastPduRecorder implements PduRecorder {
    private static final Logger logger = LoggerFactory.getLogger(MulticastPduRecorder.class);
    private static final String DEFAULT_MULTICAST_GROUP = "239.1.2.3";
    private static final int DEFAULT_PORT = 3000;
    private static final int BUFFER_SIZE = 8192;

    private final PduStorage storage;
    private final String multicastGroup;
    private final int port;
    private MulticastSocket socket;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private String currentExerciseId;
    private ExecutorService executorService;
    private final List<PduAnalyzer> analyzers = new CopyOnWriteArrayList<>();

    /**
     * Create a new MulticastPduRecorder with default multicast group and port.
     * 
     * @param storage The storage to use for recorded PDUs
     */
    public MulticastPduRecorder(PduStorage storage) {
        this(storage, DEFAULT_MULTICAST_GROUP, DEFAULT_PORT);
    }

    /**
     * Create a new MulticastPduRecorder with custom multicast group and port.
     * 
     * @param storage The storage to use for recorded PDUs
     * @param multicastGroup The multicast group to join
     * @param port The port to listen on
     */
    public MulticastPduRecorder(PduStorage storage, String multicastGroup, int port) {
        this.storage = storage;
        this.multicastGroup = multicastGroup;
        this.port = port;
    }

    @Override
    public void startRecording(String exerciseId) {
        if (recording.compareAndSet(false, true)) {
            this.currentExerciseId = exerciseId;
            try {
                socket = new MulticastSocket(port);
                InetAddress group = InetAddress.getByName(multicastGroup);
                socket.joinGroup(group);
                
                executorService = Executors.newSingleThreadExecutor();
                executorService.submit(this::receiveLoop);
                
                logger.info("Started recording exercise: {} on {}:{}", 
                    exerciseId, multicastGroup, port);
            } catch (IOException e) {
                recording.set(false);
                currentExerciseId = null;
                logger.error("Failed to start recording: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start recording", e);
            }
        } else {
            logger.warn("Already recording exercise: {}", currentExerciseId);
        }
    }

    @Override
    public void stopRecording() {
        if (recording.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = null;
            }
            
            if (socket != null) {
                try {
                    InetAddress group = InetAddress.getByName(multicastGroup);
                    socket.leaveGroup(group);
                } catch (IOException e) {
                    logger.warn("Error leaving multicast group: {}", e.getMessage());
                }
                socket.close();
                socket = null;
            }
            
            logger.info("Stopped recording exercise: {}", currentExerciseId);
            currentExerciseId = null;
        }
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }

    @Override
    public String getCurrentExerciseId() {
        return currentExerciseId;
    }
    
    @Override
    public void addAnalyzer(PduAnalyzer analyzer) {
        if (analyzer != null) {
            analyzers.add(analyzer);
            logger.info("Added analyzer: {}", analyzer.getName());
        }
    }
    
    @Override
    public void removeAnalyzer(PduAnalyzer analyzer) {
        if (analyzer != null && analyzers.remove(analyzer)) {
            logger.info("Removed analyzer: {}", analyzer.getName());
        }
    }

    /**
     * Main receive loop that listens for PDUs on the multicast socket.
     */
    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (recording.get() && !Thread.currentThread().isInterrupted()) {
            try {
                socket.receive(packet);
                
                // Process the received packet
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                
                try {
                    Pdu pdu = createPduFromData(data);
                    if (pdu != null) {
                        // Store PDU with current system time as timestamp
                        RecordedPdu recordedPdu = new RecordedPdu(pdu, System.currentTimeMillis(), currentExerciseId);
                        storage.storePdu(recordedPdu);
                        
                        // Run analyzers on the PDU
                        for (PduAnalyzer analyzer : analyzers) {
                            try {
                                analyzer.analyzePdu(recordedPdu);
                            } catch (Exception e) {
                                logger.warn("Error in analyzer {}: {}", analyzer.getName(), e.getMessage());
                            }
                        }
                        
                        logger.debug("Recorded PDU type {} for exercise {}", 
                            pdu.getClass().getSimpleName(), currentExerciseId);
                    }
                } catch (Exception e) {
                    logger.warn("Error processing PDU: {}", e.getMessage());
                }
                
                // Reset the packet for the next receive
                packet.setLength(buffer.length);
            } catch (IOException e) {
                if (!socket.isClosed() && recording.get()) {
                    logger.error("Error receiving PDU: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Create a PDU from raw byte data.
     * 
     * @param data The raw PDU data
     * @return The created PDU, or null if the data could not be parsed
     */
    private Pdu createPduFromData(byte[] data) {
        if (data == null || data.length < 12) { // PDUs have at least a 12-byte header
            return null;
        }
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            
            // Read PDU header to determine type
            dis.skipBytes(2); // Skip protocol version and exercise ID
            int pduTypeValue = dis.readUnsignedByte();
            
            // Create appropriate PDU based on type
            Pdu pdu = null;
            PduType pduType = PduType.lookup[pduTypeValue];
            
            if (pduType != null) {
                switch (pduType) {
                    case ENTITY_STATE:
                        pdu = new edu.nps.moves.dis.EntityStatePdu();
                        break;
                    case FIRE:
                        pdu = new edu.nps.moves.dis.FirePdu();
                        break;
                    case DETONATION:
                        pdu = new edu.nps.moves.dis.DetonationPdu();
                        break;
                    // Add more PDU types as needed
                    default:
                        logger.debug("Unsupported PDU type: {}", pduType);
                        return null;
                }
                
                // Reset stream and unmarshal the PDU
                bais.reset();
                pdu.unmarshal(dis);
                return pdu;
            }
        } catch (Exception e) {
            logger.warn("Error parsing PDU data: {}", e.getMessage());
        }
        
        return null;
    }
}
