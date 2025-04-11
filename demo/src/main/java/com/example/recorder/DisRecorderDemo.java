package com.example.recorder;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.recorder.analysis.PduAnalyzer;
import com.example.recorder.analysis.StatisticsAnalyzer;
import com.example.recorder.controller.RecorderController;
import com.example.recorder.factory.RecorderFactory;

/**
 * Demo application for DIS PDU recording and replay.
 */
public class DisRecorderDemo {
    private static final Logger logger = LoggerFactory.getLogger(DisRecorderDemo.class);
    
    private final RecorderController controller;
    private final Scanner scanner;
    private com.example.recorder.sender.PduSender sender;
    
    /**
     * Create a new DisRecorderDemo with default components.
     */
    public DisRecorderDemo() {
        // Use the factory to create the controller with default components
        controller = RecorderFactory.createDefaultController();
        scanner = new Scanner(System.in);
    }
    
    /**
     * Create a new DisRecorderDemo with custom multicast group and port.
     * 
     * @param multicastGroup The multicast group to use
     * @param port The port to use
     */
    public DisRecorderDemo(String multicastGroup, int port) {
        // Use the factory to create the controller with custom network settings
        controller = RecorderFactory.createCustomNetworkController(multicastGroup, port);
        scanner = new Scanner(System.in);
    }
    
    /**
     * Run the demo application.
     */
    public void run() {
        printHelp();
        
        boolean running = true;
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();
            
            if (command.isEmpty()) {
                continue;
            }
            
            String[] parts = command.split("\\s+");
            String action = parts[0].toLowerCase();
            
            try {
                switch (action) {
                    case "record":
                        handleRecord(parts);
                        break;
                    case "stop-record":
                        handleStopRecord();
                        break;
                    case "replay":
                        handleReplay(parts);
                        break;
                    case "stop-replay":
                        handleStopReplay();
                        break;
                    case "list":
                        handleList();
                        break;
                    case "clear":
                        handleClear(parts);
                        break;
                    case "status":
                        handleStatus();
                        break;
                    case "add-analyzer":
                        handleAddAnalyzer(parts);
                        break;
                    case "remove-analyzer":
                        handleRemoveAnalyzer(parts);
                        break;
                    case "start-sender":
                        handleStartSender(parts);
                        break;
                    case "stop-sender":
                        handleStopSender();
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown command: " + action);
                        printHelp();
                        break;
                }
            } catch (Exception e) {
                logger.error("Error executing command: {}", e.getMessage(), e);
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        // Clean up
        if (controller.isRecording()) {
            controller.stopRecording();
        }
        
        if (controller.isReplaying()) {
            controller.stopReplay();
        }
        
        if (sender != null && sender.isRunning()) {
            sender.stop();
        }
        
        scanner.close();
        System.out.println("Exiting...");
    }
    
    /**
     * Print help information.
     */
    private void printHelp() {
        System.out.println("DIS Recorder Demo");
        System.out.println("----------------");
        System.out.println("Commands:");
        System.out.println("  record <exercise-id>       - Start recording PDUs for the specified exercise");
        System.out.println("  stop-record                - Stop recording PDUs");
        System.out.println("  replay <exercise-id> [<speed-factor>] - Replay PDUs for the specified exercise");
        System.out.println("  stop-replay                - Stop replaying PDUs");
        System.out.println("  list                       - List all available exercises");
        System.out.println("  clear <exercise-id>        - Clear all PDUs for the specified exercise");
        System.out.println("  status                     - Show current recording/replay status");
        System.out.println("  add-analyzer <type>        - Add an analyzer (types: statistics)");
        System.out.println("  remove-analyzer <type>     - Remove an analyzer (types: statistics)");
        System.out.println("  start-sender [<rate>]      - Start sending test PDUs (default rate: 1 PDU/sec)");
        System.out.println("  stop-sender                - Stop sending test PDUs");
        System.out.println("  help                       - Show this help information");
        System.out.println("  exit                       - Exit the application");
        System.out.println();
    }
    
    /**
     * Handle the 'record' command.
     * 
     * @param parts Command parts
     */
    private void handleRecord(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: Missing exercise ID");
            System.out.println("Usage: record <exercise-id>");
            return;
        }
        
        String exerciseId = parts[1];
        boolean success = controller.startRecording(exerciseId);
        
        if (success) {
            System.out.println("Started recording exercise: " + exerciseId);
        } else {
            System.out.println("Failed to start recording");
        }
    }
    
    /**
     * Handle the 'stop-record' command.
     */
    private void handleStopRecord() {
        boolean success = controller.stopRecording();
        
        if (success) {
            System.out.println("Stopped recording");
        } else {
            System.out.println("Not currently recording");
        }
    }
    
    /**
     * Handle the 'replay' command.
     * 
     * @param parts Command parts
     */
    private void handleReplay(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: Missing exercise ID");
            System.out.println("Usage: replay <exercise-id> [<speed-factor>]");
            return;
        }
        
        String exerciseId = parts[1];
        double speedFactor = 1.0;
        
        if (parts.length >= 3) {
            try {
                speedFactor = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid speed factor");
                System.out.println("Usage: replay <exercise-id> [<speed-factor>]");
                return;
            }
        }
        
        boolean success = controller.startReplay(exerciseId, speedFactor);
        
        if (success) {
            System.out.println("Started replaying exercise: " + exerciseId + " at " + speedFactor + "x speed");
        } else {
            System.out.println("Failed to start replay");
        }
    }
    
    /**
     * Handle the 'stop-replay' command.
     */
    private void handleStopReplay() {
        boolean success = controller.stopReplay();
        
        if (success) {
            System.out.println("Stopped replaying");
        } else {
            System.out.println("Not currently replaying");
        }
    }
    
    /**
     * Handle the 'list' command.
     */
    private void handleList() {
        List<String> exerciseIds = controller.getExerciseIds();
        
        if (exerciseIds.isEmpty()) {
            System.out.println("No exercises available");
        } else {
            System.out.println("Available exercises:");
            for (String exerciseId : exerciseIds) {
                System.out.println("  " + exerciseId);
            }
        }
    }
    
    /**
     * Handle the 'clear' command.
     * 
     * @param parts Command parts
     */
    private void handleClear(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: Missing exercise ID");
            System.out.println("Usage: clear <exercise-id>");
            return;
        }
        
        String exerciseId = parts[1];
        boolean success = controller.clearExercise(exerciseId);
        
        if (success) {
            System.out.println("Cleared exercise: " + exerciseId);
        } else {
            System.out.println("Failed to clear exercise");
        }
    }
    
    /**
     * Handle the 'status' command.
     */
    private void handleStatus() {
        System.out.println("Status:");
        
        if (controller.isRecording()) {
            System.out.println("  Recording: Yes");
            System.out.println("  Exercise: " + controller.getCurrentRecordingExerciseId());
        } else {
            System.out.println("  Recording: No");
        }
        
        if (controller.isReplaying()) {
            System.out.println("  Replaying: Yes");
            System.out.println("  Exercise: " + controller.getCurrentReplayExerciseId());
            System.out.println("  Speed: " + controller.getCurrentReplaySpeedFactor() + "x");
        } else {
            System.out.println("  Replaying: No");
        }
        
        if (sender != null && sender.isRunning()) {
            System.out.println("  PDU Sender: Running");
        } else {
            System.out.println("  PDU Sender: Stopped");
        }
    }
    
    /**
     * Handle the 'add-analyzer' command.
     * 
     * @param parts Command parts
     */
    private void handleAddAnalyzer(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: Missing analyzer type");
            System.out.println("Usage: add-analyzer <analyzer-type>");
            System.out.println("Available analyzer types: statistics");
            return;
        }
        
        String analyzerType = parts[1].toLowerCase();
        PduAnalyzer analyzer = null;
        
        switch (analyzerType) {
            case "statistics":
                analyzer = new StatisticsAnalyzer();
                break;
            default:
                System.out.println("Error: Unknown analyzer type: " + analyzerType);
                System.out.println("Available analyzer types: statistics");
                return;
        }
        
        boolean success = controller.addAnalyzer(analyzer);
        
        if (success) {
            System.out.println("Added analyzer: " + analyzer.getName());
        } else {
            System.out.println("Failed to add analyzer");
        }
    }
    
    /**
     * Handle the 'remove-analyzer' command.
     * 
     * @param parts Command parts
     */
    private void handleRemoveAnalyzer(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: Missing analyzer type");
            System.out.println("Usage: remove-analyzer <analyzer-type>");
            System.out.println("Available analyzer types: statistics");
            return;
        }
        
        String analyzerType = parts[1].toLowerCase();
        PduAnalyzer analyzer = null;
        
        switch (analyzerType) {
            case "statistics":
                analyzer = new StatisticsAnalyzer();
                break;
            default:
                System.out.println("Error: Unknown analyzer type: " + analyzerType);
                System.out.println("Available analyzer types: statistics");
                return;
        }
        
        boolean success = controller.removeAnalyzer(analyzer);
        
        if (success) {
            System.out.println("Removed analyzer: " + analyzer.getName());
        } else {
            System.out.println("Failed to remove analyzer");
        }
    }
    
    /**
     * Handle the 'start-sender' command.
     * 
     * @param parts Command parts
     */
    private void handleStartSender(String[] parts) {
        int rate = 1; // Default rate: 1 PDU per second
        
        if (parts.length >= 2) {
            try {
                rate = Integer.parseInt(parts[1]);
                if (rate <= 0) {
                    System.out.println("Error: Rate must be positive");
                    System.out.println("Usage: start-sender [<rate>]");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid rate");
                System.out.println("Usage: start-sender [<rate>]");
                return;
            }
        }
        
        if (sender != null && sender.isRunning()) {
            System.out.println("Sender is already running");
            return;
        }
        
        try {
            sender = new com.example.recorder.sender.PduSender("239.1.2.30", 3000, rate);
            sender.start();
            System.out.println("Started PDU sender at " + rate + " PDUs/second");
        } catch (Exception e) {
            System.out.println("Failed to start PDU sender: " + e.getMessage());
        }
    }
    
    /**
     * Handle the 'stop-sender' command.
     */
    private void handleStopSender() {
        if (sender == null || !sender.isRunning()) {
            System.out.println("Sender is not running");
            return;
        }
        
        try {
            sender.stop();
            System.out.println("Stopped PDU sender");
        } catch (Exception e) {
            System.out.println("Failed to stop PDU sender: " + e.getMessage());
        }
    }
    
    /**
     * Main entry point.
     * 
     * @param args Command line arguments
     *             args[0] (optional): Multicast group (e.g., "239.1.2.30")
     *             args[1] (optional): Port (e.g., "3000")
     */
    public static void main(String[] args) {
        DisRecorderDemo demo;
        
        if (args.length >= 2) {
            try {
                String multicastGroup = args[0];
                int port = Integer.parseInt(args[1]);
                System.out.println("Using custom network settings: " + multicastGroup + ":" + port);
                demo = new DisRecorderDemo(multicastGroup, port);
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid port number. Using default settings.");
                demo = new DisRecorderDemo();
            }
        } else {
            System.out.println("Using default network settings (239.1.2.30:3000)");
            demo = new DisRecorderDemo();
        }
        
        demo.run();
    }
}
