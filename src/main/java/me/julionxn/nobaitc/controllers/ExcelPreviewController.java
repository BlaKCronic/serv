package me.julionxn.nobaitc.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import me.julionxn.nobaitc.MainApplication;
import me.julionxn.nobaitc.data.nonbpa.FractionResult;
import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType;
import org.controlsfx.control.spreadsheet.SpreadsheetView;

import java.util.List;

public class ExcelPreviewController {

    @FXML private AnchorPane spreadsheetContainer;
    @FXML private Button btnResumen, btnDetalles;

    private SpreadsheetView viewResumen;
    private SpreadsheetView viewDetalles;

    public void initData(List<FractionResult> data) {
        this.viewResumen = buildResumenView(data);
        this.viewDetalles = buildDetallesView(data);
        showResumen();
    }

    @FXML private void showResumen() { renderView(viewResumen); }
    @FXML private void showDetalles() { renderView(viewDetalles); }

    private void renderView(SpreadsheetView view) {
        spreadsheetContainer.getChildren().clear();
        AnchorPane.setTopAnchor(view, 0.0);
        AnchorPane.setBottomAnchor(view, 0.0);
        AnchorPane.setLeftAnchor(view, 0.0);
        AnchorPane.setRightAnchor(view, 0.0);
        spreadsheetContainer.getChildren().add(view);
    }

    private SpreadsheetView buildResumenView(List<FractionResult> data) {
        // 1. Detectar el ancho máximo de la matriz 'fraction'
        int maxColsInData = 0;
        for (FractionResult res : data) {
            double[][] matrix = res.getFraction();
            if (matrix != null && matrix.length > 0) {
                maxColsInData = Math.max(maxColsInData, matrix[0].length);
            }
        }
        // Mínimo 3 para mantener estética si vienen vacíos
        maxColsInData = Math.max(maxColsInData, 3);

        // numCols = Margen(1) + Datos(maxColsInData) + VIFS(1) + Margen(1)
        // Pero para simplificar: Margen(0) + Datos + VIFS + Margen(último)
        int colOffset = 1;
        int numCols = maxColsInData + colOffset + 2;

        GridBase grid = new GridBase(data.size() * 15, numCols);
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();

        int currRow = 0;

        for (FractionResult res : data) {
            // --- Fila 1: Título "Fracción X" ---
            ObservableList<SpreadsheetCell> titleRow = FXCollections.observableArrayList();
            titleRow.add(createEmptyCell(currRow, 0));

            // El span del título debe cubrir (maxColsInData + 1) para incluir la columna de VIFS
            int totalDataSpan = maxColsInData + 1;
            SpreadsheetCell titleCell = SpreadsheetCellType.STRING.createCell(currRow, colOffset, 1, totalDataSpan, "Fracción " + res.getFractionNumber());
            titleCell.getStyleClass().add("header-cell");
            titleCell.setStyle("-fx-alignment: center; -fx-font-weight: bold; -fx-background-color: #d1d1d1;");

            for (int i = 0; i < totalDataSpan; i++) titleRow.add(titleCell);
            titleRow.add(createEmptyCell(currRow, numCols - 1));
            rows.add(titleRow);
            currRow++;

            // --- Fila 2: J2 y GBM ---
            ObservableList<SpreadsheetCell> j2GbmRow = FXCollections.observableArrayList();
            j2GbmRow.add(createEmptyCell(currRow, 0));
            j2GbmRow.add(createStyledCell(currRow, colOffset, "J2:", "header-cell"));
            j2GbmRow.add(createStyledCell(currRow, colOffset + 1, res.getJ2(), "spreadsheet-cell"));
            j2GbmRow.add(createStyledCell(currRow, colOffset + 2, "GBM:", "header-cell"));
            j2GbmRow.add(createStyledCell(currRow, colOffset + 3, res.getGbm(), "spreadsheet-cell"));
            // Rellenar hasta el final
            for(int i = colOffset + 4; i < numCols; i++) j2GbmRow.add(createEmptyCell(currRow, i));
            rows.add(j2GbmRow);
            currRow++;

            rows.add(createEmptyRow(currRow++, numCols));

            // --- Fila 4: Encabezados DINÁMICOS ---
            ObservableList<SpreadsheetCell> headersRow = FXCollections.observableArrayList();
            headersRow.add(createEmptyCell(currRow, 0));

            // "Datos" ahora ocupa exactamente maxColsInData
            SpreadsheetCell datosHeader = SpreadsheetCellType.STRING.createCell(currRow, colOffset, 1, maxColsInData, "Datos");
            datosHeader.getStyleClass().add("header-cell");
            datosHeader.setStyle("-fx-alignment: center; -fx-font-weight: bold;");
            for(int i = 0; i < maxColsInData; i++) headersRow.add(datosHeader);

            // "VIFS" va justo después de todos los datos
            headersRow.add(createStyledCell(currRow, colOffset + maxColsInData, "VIFS", "header-cell"));
            headersRow.add(createEmptyCell(currRow, numCols - 1));
            rows.add(headersRow);
            currRow++;

            // --- Filas 5+: Matrices y VIFS ---
            double[][] matrix = res.getFraction();
            double[] vifs = res.getVifs();
            int maxRowsInRes = Math.max(matrix != null ? matrix.length : 0, vifs != null ? vifs.length : 0);

            for (int r = 0; r < maxRowsInRes; r++) {
                ObservableList<SpreadsheetCell> dataRow = FXCollections.observableArrayList();
                dataRow.add(createEmptyCell(currRow, 0));

                // Escribir todas las columnas de la matriz
                for (int c = 0; c < maxColsInData; c++) {
                    if (matrix != null && r < matrix.length && c < matrix[r].length) {
                        dataRow.add(createStyledCell(currRow, colOffset + c, matrix[r][c], "spreadsheet-cell"));
                    } else {
                        dataRow.add(createEmptyCell(currRow, colOffset + c));
                    }
                }

                // Columna VIFS (siempre en colOffset + maxColsInData)
                if (vifs != null && r < vifs.length) {
                    dataRow.add(createStyledCell(currRow, colOffset + maxColsInData, vifs[r], "spreadsheet-cell"));
                } else {
                    dataRow.add(createEmptyCell(currRow, colOffset + maxColsInData));
                }

                dataRow.add(createEmptyCell(currRow, numCols - 1));
                rows.add(dataRow);
                currRow++;
            }

            rows.add(createEmptyRow(currRow++, numCols));
            rows.add(createEmptyRow(currRow++, numCols));
        }

        grid.setRows(rows);
        SpreadsheetView sv = new SpreadsheetView(grid);
        sv.setShowRowHeader(false);

        for (int i = 1; i < numCols - 1; i++) {
            sv.getColumns().get(i).setPrefWidth(90.0); // O lo que tuvieras antes
        }
        return sv;
    }
    private SpreadsheetView buildDetallesView(List<FractionResult> data) {
        int maxCols = 0;
        int totalRows = 0;
        for (FractionResult res : data) {
            if (res.getFraction() != null) {
                totalRows += res.getFraction().length + 4;
                for (double[] r : res.getFraction()) if (r.length > maxCols) maxCols = r.length;
            }
        }

        int numCols = maxCols + 5;
        GridBase grid = new GridBase(totalRows + 20, numCols);
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();

        int currRow = 1;
        int colOffset = 1;

        for (FractionResult res : data) {
            double[][] matrix = res.getFraction();
            int matrixWidth = (matrix != null && matrix.length > 0) ? matrix[0].length : 3;

            ObservableList<SpreadsheetCell> titleRow = FXCollections.observableArrayList();
            titleRow.add(createEmptyCell(currRow, 0));

            SpreadsheetCell titleCell = SpreadsheetCellType.STRING.createCell(currRow, colOffset, 1, matrixWidth, "Fracción " + res.getFractionNumber());
            titleCell.getStyleClass().add("header-cell");
            titleCell.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

            for(int i = 0; i < matrixWidth; i++) titleRow.add(titleCell);
            for(int i = colOffset + matrixWidth; i < numCols; i++) titleRow.add(createEmptyCell(currRow, i));

            rows.add(titleRow);
            currRow++;

            // LA MATRIZ DE DATOS EMPIEZA INMEDIATAMENTE DESPUÉS DEL TÍTULO

            if (matrix != null) {
                for (double[] mRow : matrix) {
                    ObservableList<SpreadsheetCell> row = FXCollections.observableArrayList();
                    row.add(createEmptyCell(currRow, 0));
                    for (int c = 0; c < mRow.length; c++) {
                        row.add(createStyledCell(currRow, colOffset + c, mRow[c], "spreadsheet-cell"));
                    }
                    for(int c = colOffset + mRow.length; c < numCols; c++) row.add(createEmptyCell(currRow, c));
                    rows.add(row);
                    currRow++;
                }
            }
            rows.add(createEmptyRow(currRow++, numCols));
            rows.add(createEmptyRow(currRow++, numCols));
        }
        grid.setRows(rows);
        SpreadsheetView sv = new SpreadsheetView(grid);
        sv.setShowRowHeader(false);
        return sv;
    }

    // --- MÉTODOS AUXILIARES ---

    private ObservableList<SpreadsheetCell> createEmptyRow(int r, int numCols) {
        ObservableList<SpreadsheetCell> row = FXCollections.observableArrayList();
        for (int i = 0; i < numCols; i++) {
            row.add(createEmptyCell(r, i));
        }
        return row;
    }

    private SpreadsheetCell createEmptyCell(int r, int c) {
        SpreadsheetCell cell = SpreadsheetCellType.STRING.createCell(r, c, 1, 1, "");
        cell.setEditable(false);
        return cell;
    }

    private SpreadsheetCell createStyledCell(int r, int c, Object value, String styleClass) {
        SpreadsheetCell cell;
        if (value instanceof Number) {
            cell = SpreadsheetCellType.DOUBLE.createCell(r, c, 1, 1, ((Number) value).doubleValue());
        } else {
            cell = SpreadsheetCellType.STRING.createCell(r, c, 1, 1, value != null ? value.toString() : "");
        }
        cell.getStyleClass().add(styleClass);
        cell.setStyle("-fx-alignment: center;");
        return cell;
    }
}