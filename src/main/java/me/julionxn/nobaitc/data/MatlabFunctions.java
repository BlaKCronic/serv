package me.julionxn.nobaitc.data;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoublePredicate;

public class MatlabFunctions {

    private static final double EPSILON = 1e-10;
    private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(Random::new);

    // ==================== LCM y GCD ====================

    /**
     * Calcula el Mínimo Común Múltiplo (LCM) de un arreglo de números usando long.
     */
    public static long calculateLCM(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return 0L;
        }

        long lcm = numbers[0];
        for (int i = 1; i < numbers.length; i++) {
            lcm = lcm(lcm, numbers[i]);
        }

        return lcm;
    }

    private static long lcm(long a, long b) {
        if (a == 0 || b == 0) {
            return 0L;
        }
        // Se divide primero por el GCD para minimizar el riesgo de desbordamiento en la multiplicación
        return (a / gcd(a, b)) * b;
    }

    /**
     * Calcula el Máximo Común Divisor (GCD) usando el algoritmo de Euclides.
     */
    private static long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }

    // ==================== Random ====================

    public static long[] nonRepeatableRandomNumbers(long min, long max, int count) {
        long range = max - min + 1;
        if (range < count) {
            throw new IllegalArgumentException("Rango insuficiente para generar " + count + " números únicos");
        }

        long[] result = new long[count];

        // Si el rango cabe en un int Y pedimos más del 50% de los números posibles,
        // usamos la estrategia original (Fisher-Yates) para evitar lentitud por colisiones.
        if (range <= Integer.MAX_VALUE && count > range * 0.5) {
            int intRange = (int) range;
            int[] pool = new int[intRange];
            for (int i = 0; i < intRange; i++) {
                pool[i] = (int) min + i;
            }

            Random random = RANDOM.get();
            for (int i = 0; i < count; i++) {
                int j = i + random.nextInt(intRange - i);
                result[i] = pool[j];
                pool[j] = pool[i];
            }
        } else {
            // Para rangos gigantes (long) o selecciones pequeñas, un HashSet previene
            // errores de memoria y es extremadamente rápido para descartar repetidos.
            Set<Long> used = new HashSet<>(count);
            for (int i = 0; i < count; i++) {
                long num;
                do {
                    // Genera un long aleatorio entre min (inclusivo) y max (inclusivo)
                    num = ThreadLocalRandom.current().nextLong(min, max + 1);
                } while (!used.add(num)); // add() devuelve false si el número ya existía

                result[i] = num;
            }
        }
        return result;
    }
    // ==================== Operaciones de Vectores ====================

    public static double mean(double[] array) {
        if (array == null || array.length == 0) return 0;

        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum / array.length;
    }

    public static double sum(double[] array) {
        if (array == null || array.length == 0) return 0;

        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }

    public static int sum(int[] array) {
        if (array == null || array.length == 0) return 0;

        int sum = 0;
        for (int value : array) {
            sum += value;
        }
        return sum;
    }

    public static double max(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array vacío");
        }

        double max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public static int argmax(double[] array) {
        if (array == null || array.length == 0) return -1;

        int maxIdx = 0;
        double maxVal = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public static int[] find(double[] array, DoublePredicate condition) {
        if (array == null) return new int[0];

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            if (condition.test(array[i])) {
                indices.add(i);
            }
        }

        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Encuentra el valor máximo en un rango del array
     * @param array Array de entrada
     * @param start Índice de inicio (inclusivo)
     * @param end Índice de fin (exclusivo)
     * @return Valor máximo en el rango
     */
    public static double maxInRange(double[] array, int start, int end) {
        if (array == null || start >= end || start < 0) return 0;

        end = Math.min(end, array.length);
        double max = array[start];

        for (int i = start + 1; i < end; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    // ==================== Operaciones de Matrices ====================

    public static double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = matrix[i][j];
            }
        }

        return result;
    }

    /**
     * Extrae una columna de la matriz
     */
    public static double[] extractColumn(double[][] matrix, int col) {
        int rows = matrix.length;
        double[] column = new double[rows];

        for (int i = 0; i < rows; i++) {
            column[i] = matrix[i][col];
        }

        return column;
    }

    /**
     * Cuenta ocurrencias de un valor en un array
     */
    public static int countOccurrences(double[] array, double value) {
        int count = 0;
        for (double v : array) {
            if (v == value) count++;
        }
        return count;
    }

    /**
     * Calcula diferencias al cuadrado
     */
    public static double squaredDifference(double actual, double expected) {
        double diff = actual - expected;
        return diff * diff;
    }

    public static double maxCol(double[][] matrix, int col) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Matriz vacía");
        }

        double max = matrix[0][col];
        for (int i = 1; i < matrix.length; i++) {
            if (matrix[i][col] > max) {
                max = matrix[i][col];
            }
        }
        return max;
    }

    // ==================== Correlación ====================

    public static double[][] corrcoef(double[][] matrix) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Matriz vacía");
        }

        int cols = matrix[0].length;
        double[][] corrMatrix = new double[cols][cols];

        double[] means = new double[cols];
        double[] stdDevs = new double[cols];

        for (int j = 0; j < cols; j++) {
            double[] column = extractColumn(matrix, j);
            means[j] = mean(column);
            stdDevs[j] = standardDeviation(column, means[j]);
        }

        for (int i = 0; i < cols; i++) {
            corrMatrix[i][i] = 1.0;

            for (int j = i + 1; j < cols; j++) {
                double corr = pearsonCorrelationOptimized(matrix, i, j, means[i], means[j], stdDevs[i], stdDevs[j]);
                corrMatrix[i][j] = corr;
                corrMatrix[j][i] = corr;
            }
        }

        return corrMatrix;
    }

    private static double pearsonCorrelationOptimized(double[][] matrix, int col1, int col2,
                                                      double mean1, double mean2, double std1, double std2) {
        if (std1 == 0 || std2 == 0) return Double.NaN;

        int rows = matrix.length;
        double covariance = 0;

        for (int i = 0; i < rows; i++) {
            covariance += (matrix[i][col1] - mean1) * (matrix[i][col2] - mean2);
        }

        return covariance / (rows * std1 * std2);
    }

    public static double pearsonCorrelation(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) {
            throw new IllegalArgumentException("Arrays inválidos");
        }

        int n = x.length;
        double meanX = mean(x);
        double meanY = mean(y);

        double covariance = 0;
        double varX = 0;
        double varY = 0;

        for (int i = 0; i < n; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;

            covariance += diffX * diffY;
            varX += diffX * diffX;
            varY += diffY * diffY;
        }

        double denominator = Math.sqrt(varX * varY);
        return denominator == 0 ? Double.NaN : covariance / denominator;
    }

    private static double standardDeviation(double[] array, double mean) {
        double variance = 0;
        for (double value : array) {
            double diff = value - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / array.length);
    }

    // ==================== Algebra Lineal ====================

    public static double[][] inv(double[][] matrix) {
        int n = matrix.length;

        for (double[] row : matrix) {
            if (row.length != n) {
                throw new IllegalArgumentException("Matriz debe ser cuadrada");
            }
        }

        double[][] augmented = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n + i] = 1.0;
        }

        for (int i = 0; i < n; i++) {
            int maxRow = findPivotRow(augmented, i, n);
            if (maxRow != i) {
                swapRows(augmented, i, maxRow);
            }
            if (Math.abs(augmented[i][i]) < EPSILON) {
                return null;
            }
            double pivot = augmented[i][i];
            multiplyRow(augmented[i], 1.0 / pivot);
            eliminateColumn(augmented, i, n);
        }

        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }

        return inverse;
    }

    private static int findPivotRow(double[][] matrix, int col, int n) {
        int maxRow = col;
        double maxVal = Math.abs(matrix[col][col]);
        for (int k = col + 1; k < n; k++) {
            double val = Math.abs(matrix[k][col]);
            if (val > maxVal) {
                maxVal = val;
                maxRow = k;
            }
        }
        return maxRow;
    }

    private static void swapRows(double[][] matrix, int row1, int row2) {
        double[] temp = matrix[row1];
        matrix[row1] = matrix[row2];
        matrix[row2] = temp;
    }

    private static void multiplyRow(double[] row, double scalar) {
        for (int j = 0; j < row.length; j++) {
            row[j] *= scalar;
        }
    }

    private static void eliminateColumn(double[][] matrix, int pivotRow, int n) {
        for (int k = 0; k < n; k++) {
            if (k != pivotRow) {
                double factor = matrix[k][pivotRow];
                for (int j = 0; j < matrix[k].length; j++) {
                    matrix[k][j] -= factor * matrix[pivotRow][j];
                }
            }
        }
    }

    // ==================== Otras Utilidades ====================

    public static double[][] diag(double[] vector) {
        int n = vector.length;
        double[][] result = new double[n][n];

        for (int i = 0; i < n; i++) {
            result[i][i] = vector[i];
        }

        return result;
    }

    public static double[] diag(double[][] matrix) {
        int n = Math.min(matrix.length, matrix[0].length);
        double[] result = new double[n];

        for (int i = 0; i < n; i++) {
            result[i] = matrix[i][i];
        }

        return result;
    }

    public static double[][] tril(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i && j < cols; j++) {
                result[i][j] = matrix[i][j];
            }
        }

        return result;
    }

    public static int nchoosek(int n, int k) {
        if (k > n || k < 0) return 0;
        if (k == 0 || k == n) return 1;
        if (k > n - k) k = n - k;

        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }

        return (int) result;
    }

    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }
}