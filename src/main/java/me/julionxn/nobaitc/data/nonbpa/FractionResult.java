package me.julionxn.nobaitc.data.nonbpa;

import javafx.beans.property.*;
import me.julionxn.nobaitc.data.alias.AliasStructure;
import me.julionxn.nobaitc.util.FormatHelper;

import java.util.ArrayList;
import java.util.List;

public class FractionResult {
    private final IntegerProperty fractionNumber;   // número consecutivo (1, 2, 3...)
    private final LongProperty startIndex;          // número real de la fracción (ej: 47, 103...)
    private final StringProperty fractionData;
    private final StringProperty vifsData;
    private final StringProperty aliasData;         // texto formateado de la estructura de alias
    private final DoubleProperty gbm;
    private final DoubleProperty j2;
    private final double[] vifs;
    private final double[] gbmVector;              // GBM por columna (factor)
    private final double[][] fraction;
    private final AliasStructure aliasStructure;   // objeto alias (puede ser null si hubo error)

    /**
     * @param aliasStructure  resultado del AliasStructureGenerator (nullable si hubo error)
     * @param aliasError      mensaje de error si no se pudo calcular (nullable si fue exitoso)
     */
    public FractionResult(int fractionNumber, long startIndex, double gbm, double[] gbmVector,
                          double j2, double[] vifs, double[][] fraction,
                          AliasStructure aliasStructure, String aliasError) {
        this.fractionNumber  = new SimpleIntegerProperty(fractionNumber);
        this.startIndex      = new SimpleLongProperty(startIndex);
        this.fractionData    = new SimpleStringProperty(FormatHelper.formatMatrix(fraction));
        this.vifsData        = new SimpleStringProperty(FormatHelper.formatVerticalList(vifs));
        this.gbm             = new SimpleDoubleProperty(gbm);
        this.j2              = new SimpleDoubleProperty(j2);
        this.vifs            = vifs;
        this.gbmVector       = gbmVector;
        this.fraction        = fraction;
        this.aliasStructure  = aliasStructure;
        this.aliasData       = new SimpleStringProperty(formatAliasData(aliasStructure, aliasError));
    }

    // ── Número consecutivo (columna "Fracción") ──────────────────────────────
    public int getFractionNumber() { return fractionNumber.get(); }
    public IntegerProperty fractionNumberProperty() { return fractionNumber; }
    public void setFractionNumber(int v) { fractionNumber.set(v); }

    // ── Número real de inicio (nueva columna "Inicio") ───────────────────────
    public long getStartIndex() { return startIndex.get(); }
    public LongProperty startIndexProperty() { return startIndex; }

    // ── Datos de la fracción (texto para columna "Datos") ────────────────────
    public String getFractionData() { return fractionData.get(); }
    public StringProperty fractionDataProperty() { return fractionData; }
    public void setFractionData(String v) { fractionData.set(v); }

    // ── VIFs ─────────────────────────────────────────────────────────────────
    public String getVifsData() { return vifsData.get(); }
    public StringProperty vifsDataProperty() { return vifsData; }
    public double[] getVifs() { return vifs; }

    // ── GBM escalar (suma, para compatibilidad) ────────────────────────────────
    public double getGbm() { return gbm.get(); }
    public DoubleProperty gbmProperty() { return gbm; }
    public void setGbm(double v) { gbm.set(v); }

    // ── GBM vector (un valor por factor) ─────────────────────────────────────
    public double[] getGbmVector() { return gbmVector; }

    /** Texto vertical del vector GBM, igual que vifsData. */
    public String getGbmVectorData() {
        return FormatHelper.formatVerticalList(gbmVector);
    }

    // ── J2 ───────────────────────────────────────────────────────────────────
    public double getJ2() { return j2.get(); }
    public DoubleProperty j2Property() { return j2; }
    public void setJ2(double v) { j2.set(v); }

    // ── Matriz completa ───────────────────────────────────────────────────────
    public double[][] getFraction() { return fraction; }

    // ── Alias (automático, calculado junto con GBM/J2/VIF) ───────────────────
    public String getAliasData() { return aliasData.get(); }
    public StringProperty aliasDataProperty() { return aliasData; }
    public AliasStructure getAliasStructure() { return aliasStructure; }

    // =========================================================================
    // Formateo del alias para mostrar en la tabla
    // =========================================================================

    /**
     * Genera el texto compacto de la estructura de alias para mostrar en la columna
     * de la tabla, con el mismo estilo que VIFs (una relación por línea).
     *
     * Casos:
     *   - Error al calcular  → "Error: <mensaje>"
     *   - No calculado       → "N/A"
     *   - Diseño ortogonal   → "Ortogonal"
     *   - Con alias          → "A      = B  +0.5000·A-B\nC      = D-E ..."
     */
    private static String formatAliasData(AliasStructure structure, String error) {
        if (error != null) return "Error: " + error;
        if (structure == null) return "N/A";
        if (structure.isOrthogonal()) return "Ortogonal";

        double[][] msz    = structure.getMatrizAlias();
        String[]   efectos = structure.getEfectos();
        String[][] letras  = structure.getMatrizLetras();
        int A = msz.length;
        int L = efectos.length;

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (int x = 0; x < L; x++) {
            List<String> terminos = new ArrayList<>();

            for (int xx = 0; xx < A; xx++) {
                double val = msz[xx][x];
                if (val == 0) continue;

                String signo  = val >= 0 ? "+" : "-";
                double absVal = Math.abs(val);
                String etiq   = letras[xx][x];

                String coefStr = (Math.abs(absVal - 1.0) < 0.0001)
                    ? signo + etiq
                    : String.format("%s%.4f·%s", signo, absVal, etiq);

                terminos.add(coefStr);
            }

            // Solo mostrar efectos que tienen alias con otros (size > 1 = más que sí mismo)
            if (terminos.size() <= 1) continue;

            if (!first) sb.append("\n");

            sb.append(String.format("%-6s= ", efectos[x]));
            for (int t = 0; t < terminos.size(); t++) {
                String term = terminos.get(t);
                if (t == 0) {
                    // Primer término: quitar '+' inicial
                    sb.append(term.startsWith("+") ? term.substring(1) : term);
                } else {
                    sb.append("  ").append(term);
                }
            }
            first = false;
        }

        // Si todos los efectos eran solo self-alias, es ortogonal
        return (sb.length() == 0) ? "Ortogonal" : sb.toString();
    }
}