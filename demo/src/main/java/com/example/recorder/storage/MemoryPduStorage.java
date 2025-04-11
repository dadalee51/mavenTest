package com.example.recorder.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.recorder.model.RecordedPdu;

/**
 * In-memory implementation of PduStorage.
 * Stores PDUs in memory, organized by exercise ID.
 */
public class MemoryPduStorage implements PduStorage {
    private final Map<String, List<RecordedPdu>> storage = new HashMap<>();

    @Override
    public void storePdu(RecordedPdu recordedPdu) {
        String exerciseId = recordedPdu.getExerciseId();
        storage.computeIfAbsent(exerciseId, k -> new ArrayList<>()).add(recordedPdu);
    }

    @Override
    public List<RecordedPdu> getPdusForExercise(String exerciseId) {
        return storage.getOrDefault(exerciseId, new ArrayList<>()).stream()
                .sorted((pdu1, pdu2) -> Long.compare(pdu1.getTimestamp(), pdu2.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public void clearExercise(String exerciseId) {
        storage.remove(exerciseId);
    }

    @Override
    public List<String> getExerciseIds() {
        return new ArrayList<>(storage.keySet());
    }
}
