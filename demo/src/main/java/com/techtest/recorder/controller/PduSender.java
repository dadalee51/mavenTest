package com.techtest.recorder.controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.Orientation;
import edu.nps.moves.dis.Vector3Double;

/**
 * A class for sending PDUs to a multicast group for testing purposes.
 */
public class PduSender {
    private static final Logger logger = LoggerFactory.getLogger(PduSender.class);
    private static final String DEFAULT_MULTICAST_GROUP = "239.1.2.3";
    private static final int DEFAULT_PORT = 3000;
    private static final int DEFAULT_RATE = 1; // PDUs per second
    
    private final String multicastGroup;
    private final int port;
    private final int rate;
    private MulticastSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executorService;
    private final Random random = new Random();
    private final AtomicInteger entityCounter = new AtomicInteger(1);
    
    /**
     * Create a new PduSender with default settings.
     */
    public PduSender() {
        this(DEFAULT_MULTICAST_GROUP, DEFAULT_PORT, DEFAULT_RATE);
    }
    
    /**
     * Create a new PduSender with custom settings.
     * 
     * @param multicastGroup The multicast group to send to
     * @param port The port to send on
     * @param rate The rate to send PDUs (PDUs per second)
     */
    public PduSender(String multicastGroup, int port, int rate) {
        this.multicastGroup = multicastGroup;
        this.port = port;
        this.rate = rate;
    }
    
    /**
     * Start sending PDUs.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                socket = new MulticastSocket();
                executorService = Executors.newSingleThreadScheduledExecutor();
                
                // Schedule PDU sending at the specified rate
                executorService.scheduleAtFixedRate(
                    this::sendPdu, 
                    0, 
                    1000 / rate, 
                    TimeUnit.MILLISECONDS
                );
                
                logger.info("Started sending PDUs to {}:{} at {} PDUs/second", 
                    multicastGroup, port, rate);
            } catch (IOException e) {
                running.set(false);
                logger.error("Failed to start PDU sender: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start PDU sender", e);
            }
        } else {
            logger.warn("PDU sender already running");
        }
    }
    
    /**
     * Stop sending PDUs.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = null;
            }
            
            if (socket != null) {
                socket.close();
                socket = null;
            }
            
            logger.info("Stopped sending PDUs");
        }
    }
    
    /**
     * Check if the sender is running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Send a PDU.
     */
    private void sendPdu() {
        try {
            // Create an Entity State PDU
            EntityStatePdu pdu = createEntityStatePdu();
            
            // Convert PDU to bytes
            byte[] pduBytes = pduToBytes(pdu);
            
            // Send PDU
            InetAddress group = InetAddress.getByName(multicastGroup);
            DatagramPacket packet = new DatagramPacket(pduBytes, pduBytes.length, group, port);
            socket.send(packet);
            
            logger.debug("Sent PDU: Entity ID {}", pdu.getEntityID().getEntity());
        } catch (Exception e) {
            if (running.get()) {
                logger.error("Error sending PDU: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Create a random Entity State PDU.
     * 
     * @return The created PDU
     */
    private EntityStatePdu createEntityStatePdu() {
        EntityStatePdu pdu = new EntityStatePdu();
        
        // Set the Entity ID
        EntityID entityId = new EntityID();
        entityId.setSite(1);
        entityId.setApplication(1);
        entityId.setEntity(entityCounter.getAndIncrement());
        pdu.setEntityID(entityId);
        
        // Set Entity Location (random x, y, z coordinates)
        Vector3Double location = new Vector3Double();
        location.setX(random.nextDouble() * 1000);
        location.setY(random.nextDouble() * 1000);
        location.setZ(random.nextDouble() * 100);
        pdu.setEntityLocation(location);
        
        // Set Entity Orientation (random orientation)
        Orientation orientation = new Orientation();
        orientation.setPsi((float)(random.nextDouble() * Math.PI * 2));
        orientation.setTheta((float)(random.nextDouble() * Math.PI * 2));
        orientation.setPhi((float)(random.nextDouble() * Math.PI * 2));
        pdu.setEntityOrientation(orientation);
        
        return pdu;
    }
    
    /**
     * Convert a PDU to a byte array.
     * 
     * @param pdu The PDU to convert
     * @return The byte array
     * @throws IOException If an error occurs during conversion
     */
    private byte[] pduToBytes(EntityStatePdu pdu) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
        pdu.marshal(dos);
        return baos.toByteArray();
    }
}
