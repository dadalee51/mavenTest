package com.example;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.nps.moves.dis.EntityID;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.Vector3Double;



public class DisExample {
    private static final Logger logger = LoggerFactory.getLogger(DisExample.class);
    public static void main(String[] args) {
        try {
            // Create an Entity State PDU
            EntityStatePdu espdu = new EntityStatePdu();

            // Set the Entity ID
            EntityID entityId = new EntityID();
            entityId.setSite(1);
            entityId.setApplication(1);
            entityId.setEntity(100);
            espdu.setEntityID(entityId);

            // Set Entity Location (x, y, z coordinates)
            Vector3Double location = new Vector3Double();
            location.setX(100.0);
            location.setY(200.0);
            location.setZ(0.0);
            espdu.setEntityLocation(location);

            // Serialize the PDU to a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            espdu.marshal(dos);
            byte[] pduBytes = baos.toByteArray();

            // Print the serialized PDU length
            logger.info("PDU serialized successfully. Length: " + pduBytes.length + " bytes");

            // Print some PDU details
            logger.info("Entity ID: Site=" + espdu.getEntityID().getSite() +
                    ", App=" + espdu.getEntityID().getApplication() +
                    ", Entity=" + espdu.getEntityID().getEntity());
            logger.info("Location: X=" + espdu.getEntityLocation().getX() +
                    ", Y=" + espdu.getEntityLocation().getY() +
                    ", Z=" + espdu.getEntityLocation().getZ());

        } catch (Exception e) {
            System.err.println("Error creating or serializing PDU: " + e.getMessage());
            e.printStackTrace();
        }
    }
}