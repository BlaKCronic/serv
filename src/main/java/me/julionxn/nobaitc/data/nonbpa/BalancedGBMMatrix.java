package me.julionxn.nobaitc.data.nonbpa;

import me.julionxn.nobaitc.data.MatlabFunctions;

/**
 * Calcula el parámetro GBM.
 */
public class BalancedGBMMatrix {

    /**
     * Calcula el parámetro GBM para una fracción dada
     * @param fraction matriz que representa la fracción
     * @param design arreglo con los niveles de cada factor
     * @return GBM
     */
    public double calculateGBM(double[][] fraction, int[] design) {
        int rows = fraction.length;
        int factors = fraction[0].length;

        double gbmTotal = 0;

        for (int factor = 0; factor < factors; factor++) {
            int levels = design[factor];
            double expectedCount = (double) rows / levels;
            double[] column = MatlabFunctions.extractColumn(fraction, factor);
            double factorGBM = 0;
            for (int level = 1; level <= levels; level++) {
                int actualCount = MatlabFunctions.countOccurrences(column, level);
                factorGBM += MatlabFunctions.squaredDifference(actualCount, expectedCount);
            }

            gbmTotal += factorGBM;
        }

        return gbmTotal;
    }
}