package me.julionxn.nobaitc.data.nonbpa;

import javafx.beans.property.*;
import me.julionxn.nobaitc.util.FormatHelper;

public class FractionResult {
    private final IntegerProperty fractionNumber;   // número consecutivo (1, 2, 3...)
    private final LongProperty startIndex;          // número real de la fracción (ej: 47, 103...)
    private final StringProperty fractionData;
    private final StringProperty vifsData;
    private final DoubleProperty gbm;
    private final DoubleProperty j2;
    private final double[] vifs;
    private final double[] gbmVector;              // GBM por columna (factor)
    private final double[][] fraction;

    public FractionResult(int fractionNumber, long startIndex, double gbm, double[] gbmVector,
                          double j2, double[] vifs, double[][] fraction) {
        this.fractionNumber = new SimpleIntegerProperty(fractionNumber);
        this.startIndex = new SimpleLongProperty(startIndex);
        this.fractionData = new SimpleStringProperty(FormatHelper.formatMatrix(fraction));
        this.vifsData = new SimpleStringProperty(FormatHelper.formatVerticalList(vifs));
        this.gbm = new SimpleDoubleProperty(gbm);
        this.j2 = new SimpleDoubleProperty(j2);
        this.vifs = vifs;
        this.gbmVector = gbmVector;
        this.fraction = fraction;
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

    // ── GBM escalar (promedio, para compatibilidad) ───────────────────────────
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
}