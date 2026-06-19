package me.julionxn.nobaitc.data.alias;

import me.julionxn.nobaitc.data.MatlabFunctions;
import me.julionxn.nobaitc.util.FormatHelper;

import java.util.*;

public class AliasStructureGenerator {

    private final double[][] array;
    private final int m; // filas
    private final int n; // columnas (factores)
    private double ponderacion = 0.5;

    // Variables del proceso
    private double[][] matrizCorrelaciones;
    private double[][] T;  // Triangular inferior
    private double VL;     // Valor límite
    private double[][] W;  // Matriz de correlaciones absolutas
    private int A;         // Número de filas de W
    private int L;         // Número de columnas de W (total de efectos)
    private int me;        // Número de efectos principales
    private int doble;     // Número de interacciones de 2 factores
    private int triple;    // Número de interacciones de 3 factores

    private String[] renglonLetras;
    private String[][] matrixLetras;

    // Resultado final
    private double[][] MSZ;

    /**
     * Constructor principal
     * @param fraction Matriz del diseño factorial fraccionado
     */
    public AliasStructureGenerator(double[][] fraction) {
        if (fraction == null || fraction.length == 0) {
            throw new IllegalArgumentException("La fracción no puede estar vacía");
        }
        this.array = fraction;
        this.m = fraction.length;
        this.n = fraction[0].length;
    }

    /**
     * Genera la estructura de alias.
     * @return Estructura de alias calculada.
     * @throws IllegalStateException si los efectos principales están fuertemente correlacionados.
     */
    public AliasStructure generate() {
        // PASO 1-3: Calcular correlaciones
        matrizCorrelaciones = calcularCorrelaciones();

        // PASO 4: Procesar matriz de correlaciones
        paso4();

        // Verificar correlaciones fuertes entre efectos principales
        if (verificarCorrelacionesFuertes()) {
            throw new IllegalStateException(
                "La fracción contiene efectos principales que están fuertemente " +
                "correlacionados (r>0.5). No se puede calcular la estructura de alias."
            );
        }

        // PASO 5: Calcular alias
        paso5();

        return new AliasStructure(MSZ, renglonLetras, matrixLetras, me);
    }

    // =========================================================================
    // PASO 1-3: Calcular correlaciones
    // =========================================================================

    private double[][] calcularCorrelaciones() {
        generarCombinacionesLetras();
        double[][] matrizModelo = construirMatrizModelo();
        return MatlabFunctions.corrcoef(matrizModelo);
    }

    /**
     * Construye la matriz del modelo con efectos principales, dobles y triples.
     * Equivalente a PASO1A3CALCULARCORRELACIONES.m
     *
     * Normalización: 1 - (2*(Nj - x_ij) / (Nj - 1))
     */
    private double[][] construirMatrizModelo() {
        // Calcular máximo por columna
        int[] maximos = new int[n];
        for (int j = 0; j < n; j++) {
            int max = (int) array[0][j];
            for (int i = 1; i < m; i++) {
                if ((int) array[i][j] > max) max = (int) array[i][j];
            }
            maximos[j] = max;
        }

        // Normalizar columnas: columna_j[i] = 1 - (2*(Nj - array[i][j]) / (Nj - 1))
        double[][] cols = new double[n][m];
        for (int j = 0; j < n; j++) {
            double nj = maximos[j];
            if (nj == 1) {
                // Factor con un solo nivel: columna constante cero (evita div/0)
                Arrays.fill(cols[j], 0.0);
            } else {
                for (int i = 0; i < m; i++) {
                    cols[j][i] = 1.0 - (2.0 * (nj - array[i][j]) / (nj - 1.0));
                }
            }
        }

        List<double[]> columnas = new ArrayList<>();

        // Efectos principales
        for (int i = 0; i < n; i++) {
            columnas.add(cols[i]);
        }

        // Interacciones dobles
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                columnas.add(multiplicarColumnas(cols[i], cols[j]));
            }
        }

        // Interacciones triples (solo si n > 2)
        if (n > 2) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    for (int k = j + 1; k < n; k++) {
                        double[] dobleIJ = multiplicarColumnas(cols[i], cols[j]);
                        columnas.add(multiplicarColumnas(dobleIJ, cols[k]));
                    }
                }
            }
        }

        // Convertir lista de columnas a matriz [m x totalColumnas]
        double[][] resultado = new double[m][columnas.size()];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < columnas.size(); j++) {
                resultado[i][j] = columnas.get(j)[i];
            }
        }
        return resultado;
    }

    private double[] multiplicarColumnas(double[] col1, double[] col2) {
        double[] res = new double[col1.length];
        for (int i = 0; i < col1.length; i++) res[i] = col1[i] * col2[i];
        return res;
    }

    /**
     * Genera etiquetas de efectos y la matrizLetras.
     * NOTA: se usa "ABCDEFGHI" (I, no J) para ser fiel al MATLAB original.
     */
    private void generarCombinacionesLetras() {
        // Generar letras al estilo Excel
        String[] variables = new String[n];
        for (int i = 0; i < n; i++) {
            variables[i] = obtenerLetraExcel(i);
        }
        List<String> combinaciones = new ArrayList<>();

        // Efectos principales
        for (int i = 0; i < n; i++) {
            combinaciones.add(variables[i]);
        }

        // Dobles
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                combinaciones.add(variables[i] + "-" + variables[j]);
            }
        }

        // Triples
        if (n > 2) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    for (int k = j + 1; k < n; k++) {
                        combinaciones.add(variables[i] + "-" + variables[j] + "-" + variables[k]);
                    }
                }
            }
        }

        // === CORRECCIÓN AQUÍ ===
        // Quitamos "String[]" y "String[][]" para usar las variables de la clase
        this.renglonLetras = combinaciones.toArray(new String[0]);
        int localL = this.renglonLetras.length; // Usa un nombre local para que no choque con tu variable global 'L' si ya existe
        this.matrixLetras = new String[localL][localL];
        for (int i = 0; i < localL; i++) {
            Arrays.fill(this.matrixLetras[i], this.renglonLetras[i]);
        }
    }
    // Convierte un número (0, 1, 26) en letras (A, B, AA)
    private String obtenerLetraExcel(int index) {
        StringBuilder nombre = new StringBuilder();
        while (index >= 0) {
            nombre.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return nombre.toString();
    }
    // =========================================================================
    // PASO 4
    // Equivalente a PASO4.m
    // =========================================================================

    private void paso4() {
        T = MatlabFunctions.tril(matrizCorrelaciones);

        // Q = max(abs(T)) excluyendo diagonal (donde T=1)
        // MATLAB: element = find(abs(T<1)); Q = max(abs(T(element)))
        // Equivalente: max de |T[i][j]| donde |T[i][j]| < 1 (excluye diagonal=1)
        double Q = 0;
        for (int i = 0; i < T.length; i++) {
            for (int j = 0; j < T[i].length; j++) {
                double absVal = Math.abs(T[i][j]);
                if (absVal < 1.0 - 1e-10 && absVal > Q) {
                    Q = absVal;
                }
            }
        }
        VL = Q * ponderacion;

        // W = abs(T), redondear a cero valores muy pequeños
        W = new double[T.length][T[0].length];
        for (int i = 0; i < T.length; i++) {
            for (int j = 0; j < T[i].length; j++) {
                double absVal = Math.abs(T[i][j]);
                W[i][j] = (absVal < 0.0001) ? 0.0 : absVal;
            }
        }

        A = W.length;
        L = W[0].length;

        me = n;
        doble = MatlabFunctions.nchoosek(n, 2);
        triple = (n > 2) ? MatlabFunctions.nchoosek(n, 3) : 0;
    }

    /**
     * Verifica si hay correlaciones fuertes (>=1.5) entre efectos principales.
     * En correlaciones normalizadas [−1,1], >=1.5 no ocurre para r real;
     * este umbral detecta columnas constantes u otros artefactos numéricos.
     */
    private boolean verificarCorrelacionesFuertes() {
        for (int col = 0; col < me - 1; col++) {
            for (int fila = col + 1; fila < me; fila++) {
                if (Math.abs(W[fila][col]) >= 1.5) {
                    return true;
                }
            }
        }
        return false;
    }

    // =========================================================================
    // PASO 5
    // Equivalente a PASO5.m
    // =========================================================================

    private void paso5() {
        // BUSCACORRELACIONESSUPERIORESALVL
        double[][] revW = buscarCorrelacionesSuperioresAlVL();

        // vectoralias: 1 si la fila tt de revW tiene algún valor > 0
        int sumaVectorAlias = contarFilasConAlias(revW);

        if (sumaVectorAlias == 0) {
            // Diseño ortogonal: MSZ = revW con diagonal = 1
            for (int ttt = 0; ttt < L; ttt++) {
                revW[ttt][ttt] = 1.0;
            }
            MSZ = revW;
            System.out.println("ALIAS CALCULADOS CORRECTAMENTE, DISEÑO ORTOGONAL");

        } else {
            // LOCALIZACORRELACIONESSUPERIORESALVL
            double[][] D = localizarCorrelacionesSuperioresAlVL(revW);

            // vectoralias2: 1 si la fila ttf de D tiene algún valor > 0
            int sumaVectorAlias2 = contarFilasConAlias(D);

            double[][] CH;
            if (sumaVectorAlias2 == L) {
                // TODOS los efectos tienen alias → CH = D directamente
                // (rama que faltaba en el Java original)
                System.out.println("ALIAS CALCULADOS CORRECTAMENTE a");
                CH = D;
            } else {
                // Quedan efectos sin asignar → ASIGNACORRELACIONESINFERIORESALVL
                CH = asignarCorrelacionesInferioresAlVL(D);
            }

            MSZ = cambioDeSignos(CH);
        }
    }

    /**
     * BUSCACORRELACIONESSUPERIORESALVL
     * Retorna revW: copia de W con ceros en diagonal y en valores < VL
     */
    private double[][] buscarCorrelacionesSuperioresAlVL() {
        double[][] revW = new double[A][L];
        for (int v = 0; v < A; v++) {
            for (int i = 0; i < L; i++) {
                double val = W[v][i];
                // Eliminar diagonal (≈1) y valores menores al VL
                if (Math.abs(val - 1.0) < 0.0001 || val < VL) {
                    revW[v][i] = 0.0;
                } else {
                    revW[v][i] = val;
                }
            }
        }
        return revW;
    }

    /**
     * Cuenta filas que tienen al menos un valor > 0.
     * Equivalente al vectoralias de MATLAB (sum del vector).
     */
    private int contarFilasConAlias(double[][] matriz) {
        int suma = 0;
        for (double[] fila : matriz) {
            for (double val : fila) {
                if (val > 0) { suma++; break; }
            }
        }
        return suma;
    }

    /**
     * LOCALIZACORRELACIONESSUPERIORESALVL
     * Asigna a cada efecto el alias más fuerte (mayor correlación),
     * separando por rango: efectos principales → dobles → triples.
     */
    private double[][] localizarCorrelacionesSuperioresAlVL(double[][] eM) {
        System.out.println("________________EM_0_______________");
        FormatHelper.printMatrix(eM);
        System.out.println("ME: " + me);

        // vecceros: indica qué tipo de alias tiene cada efecto
        // 0=ninguno, 1=aliado con principal, 2=aliado con doble, 3=aliado con triple
        int[] vecceros = new int[A];

        for (int vv = 0; vv < A; vv++) {
            double fencuentra   = MatlabFunctions.maxInRange(eM[vv], 0, me);
            double ffencuentra  = MatlabFunctions.maxInRange(eM[vv], me, me + doble);
            double fffencuentra = MatlabFunctions.max(eM[vv]);

            if (fffencuentra == 0) {
                vecceros[vv] = 0;
            } else if (fencuentra != 0) {
                vecceros[vv] = 1;
            } else if (ffencuentra != 0) {
                vecceros[vv] = 2;
            }
        }

        // Efectos aliados con un efecto principal (tipo 1)
        for (int fx = me; fx < A; fx++) {
            if (vecceros[fx] == 1) {
                double[] vctr = Arrays.copyOfRange(eM[fx], 0, me);
                int h = MatlabFunctions.argmax(vctr);
                double r = vctr[h];

                if (r != 0) {
                    // Dejar solo el máximo en vctr
                    limpiarDejaMaximo(vctr, h);

                    System.arraycopy(vctr, 0, eM[fx], 0, me);
                    // Ceros en la parte de dobles y triples de esta fila
                    for (int i = me; i < L; i++) eM[fx][i] = 0.0;
                    // Ceros en toda la columna fx (ya fue asignado)
                    for (int i = 0; i < A; i++) eM[i][fx] = 0.0;
                }
            }
        }

        // Efectos aliados con un doble (tipo 2)
        for (int vx = me; vx < A; vx++) {
            if (vecceros[vx] == 2) {
                double[] rangoDobles = Arrays.copyOfRange(eM[vx], me, me + doble);
                double vc = MatlabFunctions.max(rangoDobles);

                if (vc != 0) {
                    int hg = MatlabFunctions.argmax(rangoDobles);
                    double rg = rangoDobles[hg];

                    if (rg != 0) {
                        limpiarDejaMaximo(rangoDobles, hg);

                        // Limpiar toda la fila y poner solo el doble ganador
                        Arrays.fill(eM[vx], 0.0);
                        System.arraycopy(rangoDobles, 0, eM[vx], me, doble);
                        // Ceros en toda la columna vx
                        for (int i = 0; i < A; i++) eM[i][vx] = 0.0;
                    }
                }
            }
        }

        // Efectos aliados con un triple (tipo 3), solo si hay triples
        if (triple > 0) {
            for (int gk = me + doble; gk < A; gk++) {
                double ffffencuentra = MatlabFunctions.maxInRange(eM[gk], 0, me + doble);
                double fff3encuentra = MatlabFunctions.maxInRange(eM[gk], me + doble, L);
                if (ffffencuentra == 0 && fff3encuentra != 0) {
                    vecceros[gk] = 3;
                }
            }

            for (int vxx = me + doble; vxx < A; vxx++) {
                if (vecceros[vxx] == 3) {
                    double ir = MatlabFunctions.max(eM[vxx]);

                    if (ir != 0) {
                        double[] vctrrr = Arrays.copyOfRange(eM[vxx], me + doble, L);
                        int hgg = MatlabFunctions.argmax(vctrrr);
                        double rgg = vctrrr[hgg];

                        if (rgg != 0) {
                            limpiarDejaMaximo(vctrrr, hgg);
                            System.arraycopy(vctrrr, 0, eM[vxx], me + doble, triple);
                            for (int i = 0; i < A; i++) eM[i][vxx] = 0.0;
                        }
                    }
                }
            }
        }

        return eM;
    }

    /**
     * Deja solo el elemento de índice `maxIdx` en el arreglo; el resto = 0.
     * Si hay empates con el máximo (dentro de tolerancia), también se ponen a 0.
     */
    private void limpiarDejaMaximo(double[] arr, int maxIdx) {
        double maxVal = arr[maxIdx];
        for (int c = 0; c < arr.length; c++) {
            if (c == maxIdx) continue;
            if (maxVal > arr[c] || Math.abs(maxVal - arr[c]) < 0.0001) {
                arr[c] = 0.0;
            }
        }
    }

    /**
     * ASIGNACORRELACIONESINFERIORESALVL
     * Asigna los efectos que no quedaron aliados en la localización.
     */
    private double[][] asignarCorrelacionesInferioresAlVL(double[][] D) {

        // UD = W sin la diagonal
        double[][] UD = new double[A][L];
        for (int dd = 0; dd < A; dd++) {
            for (int i = 0; i < L; i++) {
                if (i == dd && Math.abs(W[dd][i] - 1.0) < 0.0001) {
                    UD[dd][i] = 0.0;
                } else {
                    UD[dd][i] = W[dd][i];
                }
            }
        }

        // MFL: para filas con alias en D, usa D; para el resto, usa UD
        double[][] MFL = new double[A][L];
        for (int f = 0; f < A; f++) {
            double maxRenglon = MatlabFunctions.max(D[f]);
            if (f < me || maxRenglon == 0) {
                System.arraycopy(UD[f], 0, MFL[f], 0, L);
            } else {
                System.arraycopy(D[f], 0, MFL[f], 0, L);
            }
        }

        // ML: si la columna ff>=me tiene algún alias en D, limpiar esa fila en ML
        double[][] ML = new double[A][L];
        for (int ff = 0; ff < A; ff++) {
            System.arraycopy(MFL[ff], 0, ML[ff], 0, L);
            if (ff >= me) {
                double maxCol = MatlabFunctions.maxCol(D, ff);
                if (maxCol != 0) {
                    Arrays.fill(ML[ff], 0.0);
                }
            }
        }

        // MZ + vecAyuda: marcar qué efectos ya tienen alias en ME
        double[][] MZ = new double[A][L];
        int[] vecAyuda = new int[L];

        for (int ll = 0; ll < A; ll++) {
            System.arraycopy(ML[ll], 0, MZ[ll], 0, L);
            boolean tieneValor = false;
            for (int i = 0; i < me; i++) {
                if (D[ll][i] > 0) { tieneValor = true; break; }
            }
            vecAyuda[ll] = tieneValor ? 1 : 0;
        }

        for (int ee = me; ee < A; ee++) {
            if (vecAyuda[ee] == 0) {
                for (int uu = me; uu < L; uu++) {
                    if (MZ[ee][uu] != 0) {
                        if (vecAyuda[uu] == 1) {
                            MZ[ee][uu] = 0.0;
                        } else {
                            vecAyuda[ee] = 1;
                        }
                    }
                }
            }
        }

        // MX: para cada fila dejar solo el alias con mayor correlación
        double[][] MX = new double[A][L];
        for (int fff = 0; fff < A; fff++) {
            double[] renglon = MZ[fff].clone();
            double maxVal = MatlabFunctions.max(renglon);

            if (maxVal != 0) {
                int maxIdx = MatlabFunctions.argmax(renglon);
                limpiarDejaMaximo(renglon, maxIdx);
            }
            System.arraycopy(renglon, 0, MX[fff], 0, L);
        }

        // CH: matriz final — si la columna eee>=me ya tiene alias en MX, limpiar su fila
        double[][] CH = new double[L][L];
        for (int eee = 0; eee < L; eee++) {
            System.arraycopy(MX[eee], 0, CH[eee], 0, L);
            if (eee >= me) {
                double maxCol = MatlabFunctions.maxCol(MX, eee);
                if (maxCol != 0) {
                    Arrays.fill(CH[eee], 0.0);
                }
            }
        }

        // Diagonal = 1 para efectos principales
        for (int ss = 0; ss < me; ss++) {
            CH[ss][ss] = 1.0;
        }

        // Diagonal = 1 para interacciones que son cabeza de alias
        for (int sss = me; sss < L; sss++) {
            double maxCol = MatlabFunctions.maxCol(CH, sss);
            if (maxCol != 0) {
                CH[sss][sss] = 1.0;
            }
        }

        // Si alguna fila >=me quedó vacía, ponerle diagonal=1
        int numeroAlias = contarFilasConAlias(CH);
        if (numeroAlias < L) {
            for (int yy = me; yy < L; yy++) {
                double maxRen = MatlabFunctions.max(CH[yy]);
                if (maxRen == 0) {
                    CH[yy][yy] = 1.0;
                }
            }
        }

        System.out.println("ALIAS CALCULADOS CORRECTAMENTE");
        return CH;
    }

    /**
     * CAMBIODESIGNOS
     * Restaura los signos originales de T en MSZ.
     * Si el valor absoluto coincide, toma el signo de T; si no, pone 0.
     */
    private double[][] cambioDeSignos(double[][] CH) {
        double[][] MSZresult = new double[A][L];
        for (int ppp = 0; ppp < A; ppp++) {
            for (int i = 0; i < L; i++) {
                double absT  = Math.abs(T[ppp][i]);
                double absCH = Math.abs(CH[ppp][i]);
                MSZresult[ppp][i] = (Math.abs(absT - absCH) < 0.0001) ? T[ppp][i] : 0.0;
            }
        }
        return MSZresult;
    }
}