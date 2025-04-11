package com.example.recorder.factory;

import com.example.recorder.controller.RecorderController;
import com.example.recorder.recording.MulticastPduRecorder;
import com.example.recorder.recording.PduRecorder;
import com.example.recorder.replay.MulticastPduReplayer;
import com.example.recorder.replay.PduReplayer;
import com.example.recorder.storage.MemoryPduStorage;
import com.example.recorder.storage.PduStorage;

/**
 * Factory for creating recorder components.
 * This demonstrates how the design supports extensibility for different
 * storage, recorder, and replayer implementations.
 */
public class RecorderFactory {
    
    /**
     * Create a recorder controller with default components.
     * 
     * @return A new recorder controller
     */
    public static RecorderController createDefaultController() {
        PduStorage storage = new MemoryPduStorage();
        PduRecorder recorder = new MulticastPduRecorder(storage);
        PduReplayer replayer = new MulticastPduReplayer(storage);
        
        return new RecorderController(storage, recorder, replayer);
    }
    
    /**
     * Create a recorder controller with custom multicast group and port.
     * 
     * @param multicastGroup The multicast group to use
     * @param port The port to use
     * @return A new recorder controller
     */
    public static RecorderController createCustomNetworkController(String multicastGroup, int port) {
        PduStorage storage = new MemoryPduStorage();
        PduRecorder recorder = new MulticastPduRecorder(storage, multicastGroup, port);
        PduReplayer replayer = new MulticastPduReplayer(storage, multicastGroup, port);
        
        return new RecorderController(storage, recorder, replayer);
    }
    
    /**
     * Create a recorder controller with custom components.
     * 
     * @param storage The storage to use
     * @param recorder The recorder to use
     * @param replayer The replayer to use
     * @return A new recorder controller
     */
    public static RecorderController createCustomController(
            PduStorage storage, PduRecorder recorder, PduReplayer replayer) {
        return new RecorderController(storage, recorder, replayer);
    }
    
    /**
     * Example of how to extend the system with a custom storage implementation.
     * This method is not implemented but shows how the design supports extensibility.
     * 
     * @param storageType The type of storage to create
     * @return A new storage instance
     */
    public static PduStorage createStorage(String storageType) {
        // This is just an example of how the system could be extended
        // with different storage implementations
        switch (storageType.toLowerCase()) {
            case "memory":
                return new MemoryPduStorage();
            case "file":
                // Example of how a file-based storage could be created
                // return new FilePduStorage("/path/to/storage");
                throw new UnsupportedOperationException("File storage not implemented");
            case "database":
                // Example of how a database storage could be created
                // return new DatabasePduStorage("jdbc:mysql://localhost:3306/dis_recorder");
                throw new UnsupportedOperationException("Database storage not implemented");
            default:
                throw new IllegalArgumentException("Unknown storage type: " + storageType);
        }
    }
}
