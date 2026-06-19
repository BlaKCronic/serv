package me.julionxn.nobaitc.util;

import java.util.Arrays;

public class FormatHelper {

    private static final String[] letters = new String[]{
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
    };

    public static String getLetter(int index){
        return letters[index % letters.length];
    }

    /**
     * Formatea un vector (double[])
     */
    public static String formatVector(double[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        // Recorremos TODO el vector, sin límites
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(", ");

            double val = vector[i];
            if (val == (long) val) {
                sb.append(String.format("%d", (long) val));
            } else {
                sb.append(String.format("%.4f", val));
            }
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Formatea una matriz (double[][]) de forma recursiva
     */
    public static String formatMatrix(double[][] matrix) {
        StringBuilder sb = new StringBuilder();

        // Recorremos TODAS las filas
        for (int i = 0; i < matrix.length; i++) {
            sb.append("  ").append(formatVector(matrix[i])); // Espacios para indentar

            if (i < matrix.length - 1) {
                sb.append(",\n"); // Coma y salto de línea si no es la última fila
            } else {
                sb.append("\n"); // Solo salto de línea si es la última
            }
        }
        return sb.toString();
    }

    public static void printMatrix(double[][] matrix) {
        StringBuilder sb = new StringBuilder("[");
        for (double[] row : matrix) {
            sb.append("[");
            for (double value : row) {
//                sb.append(String.format("%.4f", value)).append(", ");
                //sb.append(value).append(", ");
            }
            sb.append("],").append("\n");
        }
        sb.append("]");
//        System.out.println(sb);
    }

    public static void printMatrix(double[] matrix){
        StringBuilder sb = new StringBuilder();
        sb.append("[").append("\n");
        for (double value : matrix) {
            sb.append(value).append(", ");
        }
        sb.append("]");
        System.out.println(sb);
    }

    public static void printMatrix(int[] matrix){
        StringBuilder sb = new StringBuilder();
        sb.append("[").append("\n");
        for (double value : matrix) {
            sb.append(value).append(", \n");
        }
        sb.append("]");
        System.out.println(sb);
    }

    /**
     * Formatea un vector de forma vertical, sin corchetes y con saltos de línea
     */
    public static String formatVerticalList(double[] vector) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < vector.length; i++) {
            double val = vector[i];

            // Reutilizamos la lógica limpia que ya armamos para los decimales/enteros
            if (val == (long) val) {
                sb.append(String.format("%d", (long) val));
            } else {
                sb.append(String.format("%.4f", val));
            }

            // Agrega la coma y el salto de línea, excepto en el último elemento
            if (i < vector.length - 1) {
                sb.append(",\n");
            }
        }

        return sb.toString();
    }
}
