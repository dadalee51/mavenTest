package com.example.recorder.model;

import edu.nps.moves.dis.Pdu;

/**
 * Represents a recorded PDU with its timestamp.
 */
public class RecordedPdu {
    private final Pdu pdu;
    private final long timestamp;
    private final String exerciseId;

    public RecordedPdu(Pdu pdu, long timestamp, String exerciseId) {
        this.pdu = pdu;
        this.timestamp = timestamp;
        this.exerciseId = exerciseId;
    }

    public Pdu getPdu() {
        return pdu;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getExerciseId() {
        return exerciseId;
    }
}
