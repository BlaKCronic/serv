package me.julionxn.nobaitc.data.nonbpa;

import me.julionxn.nobaitc.data.MatlabFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Servicio para generar fracciones NONBPA
 */
public class NONBPAGeneratorService {

    private final BalancedGBMMatrix gbmCalculator;
    private final OrthogonalJ2Matrix j2Calculator;
    private final VIFSMatrix vifsCalculator;

    public NONBPAGeneratorService() {
        this.gbmCalculator = new BalancedGBMMatrix();
        this.j2Calculator = new OrthogonalJ2Matrix();
        this.vifsCalculator = new VIFSMatrix();
    }

    /**
     * Valida si un diseño es válido para NONBPA
     */
    public boolean validateDesign(int[] design) {
        if (design == null || design.length == 0 || design.length > 15) {
            return false;
        }

        long tr = calculateProduct(design);
        long lcm = MatlabFunctions.calculateLCM(design); // Asegúrate de cambiar esto en MatlabFunctions

        return tr == lcm;
    }

    /**
     * Calcula parámetros del diseño
     */
    public DesignParameters calculateParameters(int[] design) {
        long tr = calculateProduct(design);
        int factors = design.length;
        long lcm = MatlabFunctions.calculateLCM(design);
        int gl = factors + 2;
        int maxLevel = Arrays.stream(design).max().orElse(0);
        int sfMin = Math.max(gl, maxLevel);

        return new DesignParameters(tr, factors, lcm, gl, sfMin);
    }

    /**
     * Valida el tamaño de fracción
     */
    public boolean validateFractionSize(int[] design, int fractionSize) {
        DesignParameters params = calculateParameters(design);
        return fractionSize >= params.sfMin() && fractionSize < params.tr();
    }

    /**
     * Genera fracciones aleatorias
     */
    public List<FractionResult> generateRandomFractions(int[] design, int fractionSize, int numberOfFractions) {
        validateInputs(design, fractionSize, numberOfFractions);
        DesignParameters params = calculateParameters(design);
        long[] randomStarts = MatlabFunctions.nonRepeatableRandomNumbers(1, params.tr(), numberOfFractions);

        // Ya no llamamos a buildReflexMatrix
        return generateFractionsFromStarts(design, fractionSize, randomStarts);
    }

    public List<FractionResult> generateCustomFractions(int[] design, int fractionSize, List<Integer> customStarts) {
        if (!validateDesign(design)) {
            throw new IllegalArgumentException("Diseño no válido para NONBPA");
        }
        if (!validateFractionSize(design, fractionSize)) {
            throw new IllegalArgumentException("Tamaño de fracción no válido");
        }

        DesignParameters params = calculateParameters(design);
        validateCustomStarts(customStarts, params.tr());

        long[] customArray = customStarts.stream().mapToLong(Integer::longValue).toArray();

        // Ya no llamamos a buildReflexMatrix
        return generateFractionsFromStarts(design, fractionSize, customArray);
    }

    private void validateInputs(int[] design, int fractionSize, int numberOfFractions) {
        if (!validateDesign(design)) {
            throw new IllegalArgumentException("Diseño no válido para NONBPA");
        }

        if (!validateFractionSize(design, fractionSize)) {
            throw new IllegalArgumentException("Tamaño de fracción no válido");
        }

        DesignParameters params = calculateParameters(design);
        if (numberOfFractions <= 0 || numberOfFractions > params.tr()) {
            throw new IllegalArgumentException("Número de fracciones no válido (1-" + params.tr() + ")");
        }
    }

    // Cambiar el tipo de maxValue a long
    private void validateCustomStarts(List<Integer> customStarts, long maxValue) {
        for (int start : customStarts) {
            if (start < 1 || start > maxValue) {
                throw new IllegalArgumentException("Fracción " + start + " fuera del rango válido (1-" + maxValue + ")");
            }
        }
    }

    private double[][] buildReflexMatrix(int[] design, int fractionSize) {
        double[][] mainMatrix = generateMainEffectsMatrix(design);
        return createReflexMatrix(mainMatrix, fractionSize);
    }

    /**
     * Genera la matriz de efectos principales
     */
    private double[][] generateMainEffectsMatrix(int[] design) {
        long trLong = calculateProduct(design);
        int factors = design.length;

        // Seguro de memoria y límites de Java
        if (trLong > Integer.MAX_VALUE) {
            throw new IllegalStateException("El número de corridas (TR: " + trLong + ") es demasiado grande para ser procesado en la memoria RAM.");
        }

        int tr = (int) trLong; // Casteo seguro porque ya validamos el tamaño arriba
        double[][] matrix = new double[tr][factors];

        for (int f = 0; f < factors; f++) {
            fillFactorColumn(matrix, f, design[f], tr);
        }

        return matrix;
    }

    private void fillFactorColumn(double[][] matrix, int col, int levels, int totalRows) {
        int blockSize = levels;
        int repeats = totalRows / levels;

        for (int repeat = 0; repeat < repeats; repeat++) {
            int startRow = repeat * blockSize;
            for (int level = 0; level < levels; level++) {
                matrix[startRow + level][col] = level + 1;
            }
        }
    }

    private double[][] createReflexMatrix(double[][] original, int fractionSize) {
        int originalRows = original.length;
        int factors = original[0].length;
        int reflexRows = fractionSize - 1;
        int totalRows = originalRows + reflexRows;

        double[][] reflexMatrix = new double[totalRows][factors];

        // Copiar matriz original
        for (int i = 0; i < originalRows; i++) {
            System.arraycopy(original[i], 0, reflexMatrix[i], 0, factors);
        }

        // Agregar reflejo
        for (int i = 0; i < reflexRows; i++) {
            System.arraycopy(original[i], 0, reflexMatrix[originalRows + i], 0, factors);
        }

        return reflexMatrix;
    }

    private double[][] extractFractionOnTheFly(int[] design, long startRow, int fractionSize) {
        int factors = design.length;
        long tr = calculateProduct(design);
        double[][] fraction = new double[fractionSize][factors];

        for (int r = 0; r < fractionSize; r++) {
            // El módulo (%) hace el trabajo de la "Matriz Refleja" volviendo al inicio
            long actualRow = (startRow + r) % tr;

            for (int c = 0; c < factors; c++) {
                int levels = design[c];
                // Replica exactamente tu lógica original de fillFactorColumn pero sin usar RAM extra
                fraction[r][c] = (actualRow % levels) + 1;
            }
        }

        return fraction;
    }

// ── Solo el fragmento que cambia ──────────────────────────────────────────
    // Reemplaza generateFractionsFromStarts en NONBPAGeneratorService.java

    private List<FractionResult> generateFractionsFromStarts(int[] design, int fractionSize, long[] starts) {
        List<FractionResult> results = new ArrayList<>(starts.length);

        for (int i = 0; i < starts.length; i++) {
            long startIndex = starts[i] - 1; // índice base 0 para la extracción

            double[][] fraction = extractFractionOnTheFly(design, startIndex, fractionSize);

            double[]  gbmVector = gbmCalculator.calculateGBMVector(fraction, design);
            double    gbm       = 0;
            for (double v : gbmVector) gbm += v;

            double   j2   = j2Calculator.calculateJ2(fraction);
            double[] vifs = vifsCalculator.calculate(fraction);

            // starts[i] es base-1 (igual que los números que ve el usuario)
            results.add(new FractionResult(i + 1, starts[i], gbm, gbmVector, j2, vifs, fraction));
        }

        return results;
    }

    private double[][] extractFraction(double[][] source, int startRow, int rows, int cols) {
        double[][] fraction = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            System.arraycopy(source[startRow + i], 0, fraction[i], 0, cols);
        }

        return fraction;
    }

    private long calculateProduct(int[] array) {
        long product = 1L;
        for (int value : array) {
            product *= value;
        }
        return product;
    }

    public record DesignParameters(long tr, int factors, long lcm, int gl, int sfMin) {}
}