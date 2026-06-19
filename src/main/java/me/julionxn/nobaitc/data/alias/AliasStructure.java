package me.julionxn.nobaitc.data.alias;

import lombok.Getter;

import java.util.*;

/**
 * Representa la estructura de alias resultante del análisis.
 * Contiene la matriz de alias y métodos de utilidad para consultar información.
 */
@Getter
public class AliasStructure {

    private final double[][] matrizAlias;
    private final String[] efectos;
    private final String[][] matrizLetras;
    private final int numEfectosPrincipales;
    private final Map<String, List<AliasPair>> aliasMap;

    /**
     * Constructor
     *
     * @param MSZ            Matriz de alias (A x L)
     * @param renglonLetras  Nombres de los efectos (longitud L)
     * @param matrixLetras   Matriz de etiquetas (A x L)
     * @param me             Número de efectos principales
     */
    public AliasStructure(double[][] MSZ, String[] renglonLetras,
                          String[][] matrixLetras, int me) {
        this.matrizAlias = MSZ;
        this.efectos = renglonLetras;
        this.matrizLetras = matrixLetras;
        this.numEfectosPrincipales = me;
        this.aliasMap = new LinkedHashMap<>();
        construirMapaAlias();
    }

    /**
     * Construye el mapa de alias para fácil acceso.
     *
     * MATLAB (AGREGALETRAS.m):
     *   for x = 1:L
     *       examinar = find(MSZ(:, x))   → busca en la COLUMNA x
     *       for xx = 1:A
     *           if MSZ(xx, x) ~= 0
     *               P = MATRIXLETRAS(xx, x)
     *
     * En Java (row-major), MSZ[xx][x] es fila xx, columna x — idéntico a MATLAB.
     */
    private void construirMapaAlias() {
        int L = efectos.length;
        int A = matrizAlias.length;

        for (int x = 0; x < L; x++) {
            // Verificar si la columna x tiene algún valor ≠ 0 (find(MSZ(:,x)))
            boolean tieneAlias = false;
            for (int xx = 0; xx < A; xx++) {
                if (matrizAlias[xx][x] != 0) { tieneAlias = true; break; }
            }
            if (!tieneAlias) continue;

            List<AliasPair> pares = new ArrayList<>();
            for (int xx = 0; xx < A; xx++) {
                double val = matrizAlias[xx][x];
                if (val != 0) {
                    pares.add(new AliasPair(val, matrizLetras[xx][x]));
                }
            }

            if (!pares.isEmpty()) {
                aliasMap.put(efectos[x], pares);
            }
        }
    }

    /**
     * Imprime la estructura de alias en consola.
     * Equivalente a la salida de AGREGALETRAS.m
     */
    public void print() {
        System.out.println("\n============ ESTRUCTURA DE ALIAS ============");

        int L = efectos.length;
        int A = matrizAlias.length;

        for (int x = 0; x < L; x++) {
            // Verificar si columna x tiene alias (find(MSZ(:,x)))
            boolean tieneAlias = false;
            for (int xx = 0; xx < A; xx++) {
                if (matrizAlias[xx][x] != 0) { tieneAlias = true; break; }
            }

            if (!tieneAlias) continue;

            System.out.println("============");
            System.out.println("  EFECTO    ");
            System.out.println(efectos[x]);
            System.out.println("     =   ");

            for (int xx = 0; xx < A; xx++) {
                double val = matrizAlias[xx][x];
                if (val != 0) {
                    System.out.printf(" %+f %n", val);
                    System.out.println(matrizLetras[xx][x]);
                }
            }
        }

        System.out.println("\n============================================");
    }

    /**
     * Verifica si el diseño es ortogonal
     * (todos los efectos solo están aliados consigo mismos, es decir la
     * diagonal es 1 y el resto 0).
     */
    public boolean isOrthogonal() {
        for (List<AliasPair> pairs : aliasMap.values()) {
            if (pairs.size() > 1) return false;
        }
        return true;
    }

    /**
     * Cuenta el número de efectos que tienen alias con otros efectos.
     */
    public int getAliasCount() {
        int count = 0;
        for (List<AliasPair> pairs : aliasMap.values()) {
            if (pairs.size() > 1) count++;
        }
        return count;
    }

    // =========================================================================
    // Inner class
    // =========================================================================

    /**
     * Representa un par de alias (coeficiente + nombre del efecto).
     */
    @Getter
    public static class AliasPair {
        public final double coeficiente;
        public final String efecto;

        public AliasPair(double coeficiente, String efecto) {
            this.coeficiente = coeficiente;
            this.efecto = efecto;
        }

        @Override
        public String toString() {
            return String.format("%+.4f %s", coeficiente, efecto);
        }
    }
}