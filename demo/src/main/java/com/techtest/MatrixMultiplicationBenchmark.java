package com.example;

public class MatrixMultiplicationBenchmark {

    public long benchmark(int size) {
        int[][] A = generateMatrix(size);
        int[][] B = generateMatrix(size);
        int[][] result = new int[size][size];

        long startTime = System.nanoTime();
        multiplyMatrices(A, B, result);
        long endTime = System.nanoTime();

        return (endTime - startTime) / 1_000_000;
    }

    public static void main(String[] args) {
        int size = 1000; // Increase for heavier loads (e.g., 1000)
        int[][] A = generateMatrix(size);
        int[][] B = generateMatrix(size);
        int[][] result = new int[size][size];

        System.out.println("Starting matrix multiplication benchmark...");

        long startTime = System.nanoTime();
        multiplyMatrices(A, B, result);
        long endTime = System.nanoTime();

        double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.printf("Execution Time: %.4f seconds%n", elapsedSeconds);
    }

    static int[][] generateMatrix(int size) {
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                matrix[i][j] = (int) (Math.random() * 10);
        return matrix;
    }

    static void multiplyMatrices(int[][] A, int[][] B, int[][] result) {
        int size = A.length;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                for (int k = 0; k < size; k++)
                    result[i][j] += A[i][k] * B[k][j];
    }
}
