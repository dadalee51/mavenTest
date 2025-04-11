package com.example.recorder.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.recorder.analysis.StatisticsAnalyzer;
import com.example.recorder.controller.RecorderController;
import com.example.recorder.factory.RecorderFactory;
import com.example.recorder.sender.PduSender;

/**
 * A Swing GUI for the DIS PDU recorder and replayer.
 */
public class RecorderGui extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RecorderGui.class);
    
    private final RecorderController controller;
    private PduSender sender;
    
    // UI Components
    private JTextField exerciseIdField;
    private JSpinner speedFactorSpinner;
    private JSpinner senderRateSpinner;
    private JButton recordButton;
    private JButton replayButton;
    private JButton senderButton;
    private JButton clearButton;
    private JButton refreshButton;
    private JButton addAnalyzerButton;
    private JButton removeAnalyzerButton;
    private JComboBox<String> analyzerComboBox;
    private JList<String> exerciseList;
    private DefaultListModel<String> exerciseListModel;
    private JTextArea statusArea;
    
    /**
     * Create a new RecorderGui with default components.
     */
    public RecorderGui() {
        super("DIS PDU Recorder");
        
        // Create controller
        controller = RecorderFactory.createDefaultController();
        
        // Set up the UI
        setupUi();
        
        // Add window close handler
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
        
        // Set default close operation
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Set size and make visible
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Refresh the exercise list
        refreshExerciseList();
        
        logger.info("RecorderGui initialized");
    }
    
    /**
     * Set up the UI components.
     */
    private void setupUi() {
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Control panel (left side)
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        
        // Recording panel
        JPanel recordingPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        recordingPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Recording", TitledBorder.LEFT, TitledBorder.TOP));
        
        // Exercise ID field
        JPanel exerciseIdPanel = new JPanel(new BorderLayout());
        exerciseIdPanel.add(new JLabel("Exercise ID:"), BorderLayout.WEST);
        exerciseIdField = new JTextField("exercise1");
        exerciseIdPanel.add(exerciseIdField, BorderLayout.CENTER);
        recordingPanel.add(exerciseIdPanel);
        
        // Record button
        recordButton = new JButton("Start Recording");
        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRecord();
            }
        });
        recordingPanel.add(recordButton);
        
        // Replay panel
        JPanel replayPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        replayPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Replay", TitledBorder.LEFT, TitledBorder.TOP));
        
        // Speed factor spinner
        JPanel speedFactorPanel = new JPanel(new BorderLayout());
        speedFactorPanel.add(new JLabel("Speed Factor:"), BorderLayout.WEST);
        speedFactorSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 100.0, 0.1));
        speedFactorPanel.add(speedFactorSpinner, BorderLayout.CENTER);
        replayPanel.add(speedFactorPanel);
        
        // Replay button
        replayButton = new JButton("Start Replay");
        replayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleReplay();
            }
        });
        replayPanel.add(replayButton);
        
        // Sender panel
        JPanel senderPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        senderPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "PDU Sender", TitledBorder.LEFT, TitledBorder.TOP));
        
        // Sender rate spinner
        JPanel senderRatePanel = new JPanel(new BorderLayout());
        senderRatePanel.add(new JLabel("PDUs per second:"), BorderLayout.WEST);
        senderRateSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        senderRatePanel.add(senderRateSpinner, BorderLayout.CENTER);
        senderPanel.add(senderRatePanel);
        
        // Sender button
        senderButton = new JButton("Start Sender");
        senderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSender();
            }
        });
        senderPanel.add(senderButton);
        
        // Analyzer panel
        JPanel analyzerPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        analyzerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Analyzers", TitledBorder.LEFT, TitledBorder.TOP));
        
        // Analyzer combo box
        JPanel analyzerComboPanel = new JPanel(new BorderLayout());
        analyzerComboPanel.add(new JLabel("Analyzer:"), BorderLayout.WEST);
        analyzerComboBox = new JComboBox<>(new String[]{"statistics"});
        analyzerComboPanel.add(analyzerComboBox, BorderLayout.CENTER);
        analyzerPanel.add(analyzerComboPanel);
        
        // Analyzer buttons
        JPanel analyzerButtonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        addAnalyzerButton = new JButton("Add");
        addAnalyzerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddAnalyzer();
            }
        });
        removeAnalyzerButton = new JButton("Remove");
        removeAnalyzerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRemoveAnalyzer();
            }
        });
        analyzerButtonPanel.add(addAnalyzerButton);
        analyzerButtonPanel.add(removeAnalyzerButton);
        analyzerPanel.add(analyzerButtonPanel);
        
        // Add panels to control panel
        controlPanel.add(recordingPanel, BorderLayout.NORTH);
        JPanel centerControlPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        centerControlPanel.add(replayPanel);
        centerControlPanel.add(senderPanel);
        centerControlPanel.add(analyzerPanel);
        controlPanel.add(centerControlPanel, BorderLayout.CENTER);
        
        // Exercise list panel (right side)
        JPanel exerciseListPanel = new JPanel(new BorderLayout());
        exerciseListPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Exercises", TitledBorder.LEFT, TitledBorder.TOP));
        
        // Exercise list
        exerciseListModel = new DefaultListModel<>();
        exerciseList = new JList<>(exerciseListModel);
        JScrollPane exerciseScrollPane = new JScrollPane(exerciseList);
        exerciseListPanel.add(exerciseScrollPane, BorderLayout.CENTER);
        
        // Exercise list buttons
        JPanel exerciseButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleClear();
            }
        });
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshExerciseList();
            }
        });
        exerciseButtonPanel.add(clearButton);
        exerciseButtonPanel.add(refreshButton);
        exerciseListPanel.add(exerciseButtonPanel, BorderLayout.SOUTH);
        
        // Status panel (bottom)
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Status", TitledBorder.LEFT, TitledBorder.TOP));
        
        // Status area
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusPanel.add(statusScrollPane, BorderLayout.CENTER);
        
        // Add panels to main panel
        mainPanel.add(controlPanel, BorderLayout.WEST);
        mainPanel.add(exerciseListPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        // Set content pane
        setContentPane(mainPanel);
    }
    
    /**
     * Handle the record button click.
     */
    private void handleRecord() {
        String exerciseId = exerciseIdField.getText().trim();
        
        if (exerciseId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Exercise ID cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (recordButton.getText().equals("Start Recording")) {
            boolean success = controller.startRecording(exerciseId);
            
            if (success) {
                recordButton.setText("Stop Recording");
                updateStatus("Started recording exercise: " + exerciseId);
                logger.info("Started recording exercise: {}", exerciseId);
            } else {
                updateStatus("Failed to start recording");
                logger.error("Failed to start recording exercise: {}", exerciseId);
            }
        } else {
            boolean success = controller.stopRecording();
            
            if (success) {
                recordButton.setText("Start Recording");
                updateStatus("Stopped recording");
                logger.info("Stopped recording");
                refreshExerciseList();
            } else {
                updateStatus("Failed to stop recording");
                logger.error("Failed to stop recording");
            }
        }
    }
    
    /**
     * Handle the replay button click.
     */
    private void handleReplay() {
        if (replayButton.getText().equals("Start Replay")) {
            String exerciseId = getSelectedExercise();
            
            if (exerciseId == null) {
                JOptionPane.showMessageDialog(this, "Please select an exercise to replay", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            double speedFactor = (Double) speedFactorSpinner.getValue();
            boolean success = controller.startReplay(exerciseId, speedFactor);
            
            if (success) {
                replayButton.setText("Stop Replay");
                updateStatus("Started replaying exercise: " + exerciseId + " at " + speedFactor + "x speed");
                logger.info("Started replaying exercise: {} at {}x speed", exerciseId, speedFactor);
            } else {
                updateStatus("Failed to start replay");
                logger.error("Failed to start replay for exercise: {}", exerciseId);
            }
        } else {
            boolean success = controller.stopReplay();
            
            if (success) {
                replayButton.setText("Start Replay");
                updateStatus("Stopped replaying");
                logger.info("Stopped replaying");
            } else {
                updateStatus("Failed to stop replay");
                logger.error("Failed to stop replay");
            }
        }
    }
    
    /**
     * Handle the sender button click.
     */
    private void handleSender() {
        if (senderButton.getText().equals("Start Sender")) {
            int rate = (Integer) senderRateSpinner.getValue();
            
            try {
                sender = new PduSender("239.1.2.30", 3000, rate);
                sender.start();
                senderButton.setText("Stop Sender");
                updateStatus("Started PDU sender at " + rate + " PDUs/second");
                logger.info("Started PDU sender at {} PDUs/second", rate);
            } catch (Exception e) {
                updateStatus("Failed to start PDU sender: " + e.getMessage());
                logger.error("Failed to start PDU sender: {}", e.getMessage(), e);
            }
        } else {
            if (sender != null && sender.isRunning()) {
                try {
                    sender.stop();
                    senderButton.setText("Start Sender");
                    updateStatus("Stopped PDU sender");
                    logger.info("Stopped PDU sender");
                } catch (Exception e) {
                    updateStatus("Failed to stop PDU sender: " + e.getMessage());
                    logger.error("Failed to stop PDU sender: {}", e.getMessage(), e);
                }
            } else {
                updateStatus("Sender is not running");
                logger.warn("Attempted to stop sender, but it was not running");
            }
        }
    }
    
    /**
     * Handle the clear button click.
     */
    private void handleClear() {
        String exerciseId = getSelectedExercise();
        
        if (exerciseId == null) {
            JOptionPane.showMessageDialog(this, "Please select an exercise to clear", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to clear exercise: " + exerciseId + "?", 
                "Confirm Clear", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            boolean success = controller.clearExercise(exerciseId);
            
            if (success) {
                updateStatus("Cleared exercise: " + exerciseId);
                logger.info("Cleared exercise: {}", exerciseId);
                refreshExerciseList();
            } else {
                updateStatus("Failed to clear exercise");
                logger.error("Failed to clear exercise: {}", exerciseId);
            }
        }
    }
    
    /**
     * Handle the add analyzer button click.
     */
    private void handleAddAnalyzer() {
        String analyzerType = (String) analyzerComboBox.getSelectedItem();
        
        if (analyzerType == null) {
            return;
        }
        
        switch (analyzerType) {
            case "statistics":
                boolean success = controller.addAnalyzer(new StatisticsAnalyzer());
                
                if (success) {
                    updateStatus("Added analyzer: statistics");
                    logger.info("Added analyzer: statistics");
                } else {
                    updateStatus("Failed to add analyzer");
                    logger.error("Failed to add analyzer: statistics");
                }
                break;
            default:
                updateStatus("Unknown analyzer type: " + analyzerType);
                logger.error("Unknown analyzer type: {}", analyzerType);
                break;
        }
    }
    
    /**
     * Handle the remove analyzer button click.
     */
    private void handleRemoveAnalyzer() {
        String analyzerType = (String) analyzerComboBox.getSelectedItem();
        
        if (analyzerType == null) {
            return;
        }
        
        switch (analyzerType) {
            case "statistics":
                boolean success = controller.removeAnalyzer(new StatisticsAnalyzer());
                
                if (success) {
                    updateStatus("Removed analyzer: statistics");
                    logger.info("Removed analyzer: statistics");
                } else {
                    updateStatus("Failed to remove analyzer");
                    logger.error("Failed to remove analyzer: statistics");
                }
                break;
            default:
                updateStatus("Unknown analyzer type: " + analyzerType);
                logger.error("Unknown analyzer type: {}", analyzerType);
                break;
        }
    }
    
    /**
     * Refresh the exercise list.
     */
    private void refreshExerciseList() {
        List<String> exerciseIds = controller.getExerciseIds();
        
        exerciseListModel.clear();
        for (String exerciseId : exerciseIds) {
            exerciseListModel.addElement(exerciseId);
        }
        
        updateStatus("Refreshed exercise list");
        logger.info("Refreshed exercise list, found {} exercises", exerciseIds.size());
    }
    
    /**
     * Get the selected exercise from the list.
     * 
     * @return The selected exercise ID, or null if none selected
     */
    private String getSelectedExercise() {
        return exerciseList.getSelectedValue();
    }
    
    /**
     * Update the status area.
     * 
     * @param status The status message to add
     */
    private void updateStatus(String status) {
        statusArea.append(status + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }
    
    /**
     * Clean up resources when closing.
     */
    private void cleanup() {
        if (controller.isRecording()) {
            controller.stopRecording();
            logger.info("Stopped recording on exit");
        }
        
        if (controller.isReplaying()) {
            controller.stopReplay();
            logger.info("Stopped replay on exit");
        }
        
        if (sender != null && sender.isRunning()) {
            sender.stop();
            logger.info("Stopped PDU sender on exit");
        }
        
        logger.info("RecorderGui closed");
    }
    
    /**
     * Main entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Could not set system look and feel: {}", e.getMessage());
        }
        
        // Create and show GUI on the event dispatch thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new RecorderGui().setVisible(true);
            }
        });
    }
}
