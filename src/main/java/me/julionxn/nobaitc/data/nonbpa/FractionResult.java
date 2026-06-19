package me.julionxn.nobaitc.data.nonbpa;

import javafx.beans.property.*;
import me.julionxn.nobaitc.util.FormatHelper;

public class FractionResult {
    private final IntegerProperty fractionNumber;
    private final StringProperty fractionData;
    private final StringProperty vifsData;
    private final DoubleProperty gbm;
    private final DoubleProperty j2;
    private final double[] vifs;
    private final double[][] fraction; // Datos completos de la fracción

    public FractionResult(int fractionNumber, double gbm, double j2, double[] vifs, double[][] fraction) {
        this.fractionNumber = new SimpleIntegerProperty(fractionNumber);
        this.fractionData = new SimpleStringProperty(FormatHelper.formatMatrix(fraction));
        this.vifsData = new SimpleStringProperty(FormatHelper.formatVerticalList(vifs));
        this.gbm = new SimpleDoubleProperty(gbm);
        this.j2 = new SimpleDoubleProperty(j2);
        this.vifs = vifs;
        this.fraction = fraction;
    }

    // Getters para las propiedades (requerido por TableView)
    public int getFractionNumber() {
        return fractionNumber.get();
    }

    public IntegerProperty fractionNumberProperty() {
        return fractionNumber;
    }

    public String getFractionData() {
        return fractionData.get();
    }

    public String getVifsData() {
        return vifsData.get();
    }

    public StringProperty fractionDataProperty() {
        return fractionData;
    }

    public StringProperty vifsDataProperty() {
        return vifsData;
    }

    public double getGbm() {
        return gbm.get();
    }

    public DoubleProperty gbmProperty() {
        return gbm;
    }

    public double getJ2() {
        return j2.get();
    }

    public DoubleProperty j2Property() {
        return j2;
    }

    public double[][] getFraction() {
        return fraction;
    }

    public double[] getVifs() { return vifs; }

    // Setters
    public void setFractionNumber(int fractionNumber) {
        this.fractionNumber.set(fractionNumber);
    }

    public void setFractionData(String fractionData) {
        this.fractionData.set(fractionData);
    }

    public void setGbm(double gbm) {
        this.gbm.set(gbm);
    }

    public void setJ2(double j2) {
        this.j2.set(j2);
    }

}