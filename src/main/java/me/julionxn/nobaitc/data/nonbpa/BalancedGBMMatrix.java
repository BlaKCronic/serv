package me.julionxn.nobaitc.data.nonbpa;

import me.julionxn.nobaitc.data.MatlabFunctions;

/**
 * Calcula el parámetro GBM.
 * Ahora expone tanto el escalar total (suma) como el vector por factor.
 */
public class BalancedGBMMatrix {

    /**
     * Devuelve un vector con el GBM de cada factor.
     * El GBM total es la suma de todos los elementos.
     */
    public double[] calculateGBMVector(double[][] fraction, int[] design) {
        int rows    = fraction.length;
        int factors = fraction[0].length;
        double[] gbmPerFactor = new double[factors];

        for (int factor = 0; factor < factors; factor++) {
            int    levels        = design[factor];
            double expectedCount = (double) rows / levels;
            double[] column      = MatlabFunctions.extractColumn(fraction, factor);
            double factorGBM     = 0;
            for (int level = 1; level <= levels; level++) {
                int actualCount = MatlabFunctions.countOccurrences(column, level);
                factorGBM += MatlabFunctions.squaredDifference(actualCount, expectedCount);
            }
            gbmPerFactor[factor] = factorGBM;
        }
        return gbmPerFactor;
    }

    /**
     * GBM total = suma del vector (mantiene compatibilidad con código existente).
     */
    public double calculateGBM(double[][] fraction, int[] design) {
        double[] vec = calculateGBMVector(fraction, design);
        double sum   = 0;
        for (double v : vec) sum += v;
        return sum;
    }
}