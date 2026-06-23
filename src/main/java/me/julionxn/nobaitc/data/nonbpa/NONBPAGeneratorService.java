package me.julionxn.nobaitc.data.nonbpa;

import me.julionxn.nobaitc.data.MatlabFunctions;
import me.julionxn.nobaitc.data.alias.AliasStructure;
import me.julionxn.nobaitc.data.alias.AliasStructureGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Servicio para generar fracciones NONBPA.
 *
 * Cada fracción generada incluye automáticamente la Estructura de Alias,
 * calculada con AliasStructureGenerator (igual que el módulo manual),
 * sin afectar el flujo de captura manual que sigue disponible en
 * AliasStructureController.
 */
public class NONBPAGeneratorService {

    private final BalancedGBMMatrix gbmCalculator;
    private final OrthogonalJ2Matrix j2Calculator;
    private final VIFSMatrix vifsCalculator;

    public NONBPAGeneratorService() {
        this.gbmCalculator   = new BalancedGBMMatrix();
        this.j2Calculator    = new OrthogonalJ2Matrix();
        this.vifsCalculator  = new VIFSMatrix();
    }

    /**
     * Valida si un diseño es válido para NONBPA
     */
    public boolean validateDesign(int[] design) {
        if (design == null || design.length == 0 || design.length > 15) {
            return false;
        }
        for (int levels : design) {
            if (levels < 2) return false;
        }
        long tr  = calculateProduct(design);
        long lcm = MatlabFunctions.calculateLCM(design);
        return tr == lcm;
    }

    /**
     * Calcula parámetros del diseño
     */
    public DesignParameters calculateParameters(int[] design) {
        long tr      = calculateProduct(design);
        int factors  = design.length;
        long lcm     = MatlabFunctions.calculateLCM(design);
        int gl       = factors + 2;
        int maxLevel = Arrays.stream(design).max().orElse(0);
        int sfMin    = Math.max(gl, maxLevel);
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
        DesignParameters params   = calculateParameters(design);
        long[] randomStarts       = MatlabFunctions.nonRepeatableRandomNumbers(1, params.tr(), numberOfFractions);
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
        return generateFractionsFromStarts(design, fractionSize, customArray);
    }

    // ── Validaciones privadas ─────────────────────────────────────────────────

    private void validateInputs(int[] design, int fractionSize, int numberOfFractions) {
        if (!validateDesign(design)) {
            throw new IllegalArgumentException("Diseño no válido para NONBPA");
        }
        if (!validateFractionSize(design, fractionSize)) {
            throw new IllegalArgumentException("Tamaño de fracción no válido");
        }
        DesignParameters params = calculateParameters(design);
        if (numberOfFractions <= 0 || numberOfFractions > params.tr()) {
            throw new IllegalArgumentException(
                "Número de fracciones no válido (1-" + params.tr() + ")"
            );
        }
    }

    private void validateCustomStarts(List<Integer> customStarts, long maxValue) {
        for (int start : customStarts) {
            if (start < 1 || start > maxValue) {
                throw new IllegalArgumentException(
                    "Fracción " + start + " fuera del rango válido (1-" + maxValue + ")"
                );
            }
        }
    }

    // ── Extracción on-the-fly ─────────────────────────────────────────────────

    private double[][] extractFractionOnTheFly(int[] design, long startRow, int fractionSize) {
        int factors      = design.length;
        long tr          = calculateProduct(design);
        double[][] fraction = new double[fractionSize][factors];

        for (int r = 0; r < fractionSize; r++) {
            long actualRow = (startRow + r) % tr;
            for (int c = 0; c < factors; c++) {
                int levels = design[c];
                fraction[r][c] = (actualRow % levels) + 1;
            }
        }
        return fraction;
    }

    // ── Generación principal ──────────────────────────────────────────────────

    /**
     * Genera las fracciones a partir de los índices de inicio y calcula
     * automáticamente GBM, J2, VIF y — nuevo — la Estructura de Alias
     * para cada fracción.
     *
     * Si AliasStructureGenerator lanza IllegalStateException (efectos
     * principales correlacionados) o cualquier otra excepción, el error
     * queda registrado en FractionResult sin interrumpir las demás fracciones.
     */
    private List<FractionResult> generateFractionsFromStarts(int[] design, int fractionSize, long[] starts) {
        List<FractionResult> results = new ArrayList<>(starts.length);

        for (int i = 0; i < starts.length; i++) {
            long startIndex = starts[i] - 1; // índice base-0 para extracción

            // ── Métricas existentes ────────────────────────────────────────
            double[][] fraction = extractFractionOnTheFly(design, startIndex, fractionSize);

            double[] gbmVector = gbmCalculator.calculateGBMVector(fraction, design);
            double   gbm       = 0;
            for (double v : gbmVector) gbm += v;

            double   j2   = j2Calculator.calculateJ2(fraction);
            double[] vifs = vifsCalculator.calculate(fraction);

            // ── Nuevo: Estructura de Alias automática ──────────────────────
            AliasStructure alias      = null;
            String         aliasError = null;
            try {
                AliasStructureGenerator generator = new AliasStructureGenerator(fraction);
                alias = generator.generate();
            } catch (IllegalStateException e) {
                // Efectos principales fuertemente correlacionados: no bloquea la fracción
                aliasError = "Efectos principales correlacionados";
            } catch (Exception e) {
                aliasError = "Error al calcular";
            }

            // starts[i] es base-1 (igual que los números que ve el usuario)
            results.add(new FractionResult(
                i + 1, starts[i],
                gbm, gbmVector,
                j2, vifs,
                fraction,
                alias, aliasError
            ));
        }

        return results;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

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