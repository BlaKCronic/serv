package me.julionxn.nobaitc.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TablePosition;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import me.julionxn.nobaitc.util.ClipboardHelper;
import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType;
import org.controlsfx.control.spreadsheet.SpreadsheetView;
import me.julionxn.nobaitc.data.nonbpa.FractionResult;
import me.julionxn.nobaitc.util.FormatHelper;

import java.util.ArrayList;
import java.util.List;

public class FractionResultDetailsController {

    @FXML private Label titleLabel;
    @FXML private Label j2Label;
    @FXML private Label gbmLabel;
    @FXML private AnchorPane spreadsheetContainer;
    private SpreadsheetView spreadsheet;

    private FractionResult data;

    public void setData(FractionResult data) {
        this.data = data;
        displayData();
    }

    private void displayData(){
        titleLabel.setText("Fracción " + data.getFractionNumber());
        j2Label.setText("J2: " + data.getJ2());
        gbmLabel.setText("GBM: " + data.getGbm());
        buildSpreadsheet();
    }

    private void buildSpreadsheet() {
        double[][] fraction = data.getFraction();
        int rows = fraction.length;
        int columns = fraction[0].length;

        GridBase grid = new GridBase(rows, columns);
        ObservableList<ObservableList<SpreadsheetCell>> rowList = FXCollections.observableArrayList();

        for (int row = 0; row < rows; row++) {
            ObservableList<SpreadsheetCell> cellRow = FXCollections.observableArrayList();
            for (int col = 0; col < columns; col++) {
                double value = fraction[row][col];
                SpreadsheetCell cell = SpreadsheetCellType.DOUBLE.createCell(row, col, 1, 1, value);
                cellRow.add(cell);
            }
            rowList.add(cellRow);
        }

        grid.setRows(rowList);

        spreadsheet = new SpreadsheetView(grid);
        spreadsheet.prefWidthProperty().bind(spreadsheetContainer.widthProperty());
        spreadsheet.prefHeightProperty().bind(spreadsheetContainer.heightProperty());

        spreadsheetContainer.getChildren().add(spreadsheet);

        List<String> colHeaders = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            colHeaders.add(FormatHelper.getLetter(i));
        }
        grid.getColumnHeaders().setAll(colHeaders);

        spreadsheet.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.C && e.isControlDown()) {
                List<TablePosition> selectedCells = spreadsheet.getSelectionModel().getSelectedCells();
                if (selectedCells == null || selectedCells.isEmpty()) {
                    return;
                }
                int minColumn = Integer.MAX_VALUE;
                int maxColumn = Integer.MIN_VALUE;
                int minRow = Integer.MAX_VALUE;
                int maxRow = Integer.MIN_VALUE;

                for (TablePosition cell : selectedCells) {
                    int row = cell.getRow();
                    int column = cell.getColumn();
                    if (column < minColumn) minColumn = column;
                    if (column > maxColumn) maxColumn = column;
                    if (row < minRow) minRow = row;
                    if (row > maxRow) maxRow = row;
                }
                StringBuilder sb = new StringBuilder();
                int numColumns = maxColumn - minColumn + 1;
                for (int row = minRow; row <= maxRow; row++) {
                    for (int col = minColumn; col <= maxColumn; col++) {
                        try {
                            Object item = spreadsheet.getGrid().getRows().get(row).get(col).getItem();
                            String value = item != null ? item.toString() : "";
                            sb.append(value);
                            if (col < maxColumn) {
                                sb.append("\t");
                            }
                        } catch (Exception ex) {
                            sb.append("");
                            if (col < maxColumn) {
                                sb.append("\t");
                            }
                        }
                    }
                    if (row < maxRow) {
                        sb.append("\n");
                    }
                }
                System.out.println(sb.toString());
                ClipboardHelper.copyToClipboard(sb.toString());
            }
        });
    }
}
