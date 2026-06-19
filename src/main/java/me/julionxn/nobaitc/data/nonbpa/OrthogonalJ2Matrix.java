package me.julionxn.nobaitc.data.nonbpa;

/**
 * Calcula el parámetro J2.
 */
public class OrthogonalJ2Matrix {

    /**
     * Calcula el parámetro J2 para una fracción dada
     * @param fraction matriz que representa la fracción
     * @return J2
     */
    public double calculateJ2(double[][] fraction) {
        int rows = fraction.length;
        int factors = fraction[0].length;

        double j2Total = 0;
        for (int i = 0; i < rows - 1; i++) {
            double[] row1 = fraction[i];
            for (int j = i + 1; j < rows; j++) {
                double[] row2 = fraction[j];
                int matches = countMatches(row1, row2, factors);
                j2Total += matches * matches;
            }
        }

        return j2Total;
    }

    private int countMatches(double[] row1, double[] row2, int length) {
        int matches = 0;
        for (int i = 0; i < length; i++) {
            if (row1[i] == row2[i]) {
                matches++;
            }
        }
        return matches;
    }
}