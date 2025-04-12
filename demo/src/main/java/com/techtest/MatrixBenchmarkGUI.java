package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MatrixBenchmarkGUI extends JFrame {
    private JTextField sizeField;
    private JTextArea resultArea;
    private MatrixMultiplicationBenchmark benchmark; 

    public MatrixBenchmarkGUI() {
        benchmark = new MatrixMultiplicationBenchmark();
        setupUI();
    }

    private void setupUI() {
        setTitle("Matrix Multiplication Benchmark");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Matrix Size:"));
        sizeField = new JTextField(10);
        inputPanel.add(sizeField);

        JButton runButton = new JButton("Run Benchmark");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runBenchmark();
            }
        });
        inputPanel.add(runButton);

        add(inputPanel, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
    }

    private void runBenchmark() {
        try {
            int size = Integer.parseInt(sizeField.getText());
            long time = benchmark.benchmark(size);
            resultArea.append(String.format("Matrix size %dx%d: %d ms\n", size, size, time));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please   enter a valid number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MatrixBenchmarkGUI gui = new MatrixBenchmarkGUI();
            gui.setVisible(true);
        });
    }
}
