package me.julionxn.nobaitc.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.julionxn.nobaitc.data.nonbpa.FractionResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
public class ExcelWriter {

    public static void generateReport(List<FractionResult> tableData, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Excel");
        fileChooser.setInitialFileName("Reporte_Fracciones.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de Excel", "*.xlsx"));

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        // 1. Detectar dinámicamente el ancho máximo de la matriz
        int maxColsInData = 0;
        for (FractionResult res : tableData) {
            double[][] matrix = res.getFraction();
            if (matrix != null && matrix.length > 0) {
                maxColsInData = Math.max(maxColsInData, matrix[0].length);
            }
        }
        maxColsInData = Math.max(maxColsInData, 3); // Mínimo 3 por estética

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle boxStyle = createBoxStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // --- HOJA 1: RESUMEN ---
            Sheet sheet1 = workbook.createSheet("Resumen de Fracciones");
            setupColumnWidths(sheet1, maxColsInData + 5, 15);

            int currentRowS1 = 1;
            int colOffset = 1;

            for (FractionResult res : tableData) {

                // --- Título "Fracción X" ---
                Row titleRow = getOrCreateRow(sheet1, currentRowS1);
                Cell titleCell = titleRow.createCell(colOffset);
                titleCell.setCellValue("Fracción " + res.getFractionNumber());
                titleCell.setCellStyle(titleStyle);

                // El título abarca "Datos" + "VIFS" (maxColsInData incluye la de VIFS)
                sheet1.addMergedRegion(new CellRangeAddress(currentRowS1, currentRowS1, colOffset, colOffset + maxColsInData));
                for(int i = 1; i <= maxColsInData; i++) {
                    titleRow.createCell(colOffset + i).setCellStyle(titleStyle);
                }

                currentRowS1++;

                // --- Fila Inmediata: J2 y GBM ---
                Row j2GbmRow = getOrCreateRow(sheet1, currentRowS1);
                setupCell(j2GbmRow, colOffset, "J2:", headerStyle);
                setupCell(j2GbmRow, colOffset + 1, res.getJ2(), boxStyle);
                setupCell(j2GbmRow, colOffset + 2, "GBM:", headerStyle);
                setupCell(j2GbmRow, colOffset + 3, res.getGbm(), boxStyle);

                currentRowS1 += 2;

                // --- Encabezados "Datos" y "VIFS" ---
                Row datosVifsRow = getOrCreateRow(sheet1, currentRowS1);
                Cell datosCell = datosVifsRow.createCell(colOffset);
                datosCell.setCellValue("Datos");
                datosCell.setCellStyle(headerStyle);

                // Combinar solo sobre el ancho de la matriz
                if (maxColsInData > 1) {
                    sheet1.addMergedRegion(new CellRangeAddress(currentRowS1, currentRowS1, colOffset, colOffset + maxColsInData - 1));
                }
                for(int i = 1; i < maxColsInData; i++) {
                    datosVifsRow.createCell(colOffset + i).setCellStyle(headerStyle);
                }

                // VIFS siempre va en la última columna calculada
                setupCell(datosVifsRow, colOffset + maxColsInData, "VIFS", headerStyle);

                currentRowS1++;

                // --- Matrices y VIFS ---
                double[][] matrix = res.getFraction();
                double[] vifs = res.getVifs();

                int dataRowsCount = (matrix != null) ? matrix.length : 0;
                int vifRowsCount = (vifs != null) ? vifs.length : 0;
                int maxRows = Math.max(dataRowsCount, vifRowsCount);

                for (int r = 0; r < maxRows; r++) {
                    Row dataRow = getOrCreateRow(sheet1, currentRowS1 + r);

                    if (matrix != null && r < matrix.length) {
                        double[] mRow = matrix[r];
                        for (int c = 0; c < maxColsInData; c++) {
                            if (c < mRow.length) {
                                setupCell(dataRow, colOffset + c, mRow[c], boxStyle);
                            } else {
                                setupCell(dataRow, colOffset + c, "", boxStyle);
                            }
                        }
                    } else {
                        for (int c = 0; c < maxColsInData; c++) setupCell(dataRow, colOffset + c, "", boxStyle);
                    }

                    if (vifs != null && r < vifs.length) {
                        setupCell(dataRow, colOffset + maxColsInData, vifs[r], boxStyle);
                    } else {
                        setupCell(dataRow, colOffset + maxColsInData, "", boxStyle);
                    }
                }

                currentRowS1 += maxRows + 2;
            }

            // --- HOJA 2: DETALLES ---
            Sheet sheet2 = workbook.createSheet("Matrices Detalladas");
            setupColumnWidths(sheet2, maxColsInData + 5, 10);
            int currentRowS2 = 1;

            for (FractionResult res : tableData) {
                double[][] matrix = res.getFraction();
                // Si la matriz es vacía, aseguramos que al menos tome valor de 1 para evitar error de merge de POI
                int matrixWidth = (matrix != null && matrix.length > 0) ? Math.max(matrix[0].length, 1) : 3;

                Row titleRow = sheet2.createRow(currentRowS2);
                Cell titleCell = titleRow.createCell(colOffset);
                titleCell.setCellValue("Fracción " + res.getFractionNumber());
                titleCell.setCellStyle(titleStyle);

                if (matrixWidth > 1) {
                    sheet2.addMergedRegion(new CellRangeAddress(currentRowS2, currentRowS2, colOffset, colOffset + matrixWidth - 1));
                }
                for(int i = 1; i < matrixWidth; i++) {
                    titleRow.createCell(colOffset + i).setCellStyle(titleStyle);
                }

                currentRowS2++;

                if (matrix != null) {
                    for (double[] mRow : matrix) {
                        Row r = sheet2.createRow(currentRowS2++);
                        for (int c = 0; c < mRow.length; c++) {
                            setupCell(r, colOffset + c, mRow[c], boxStyle);
                        }
                    }
                }
                currentRowS2 += 2;
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            askToOpenFile(file);

        } catch (IOException e) {
            showErrorAlert("Error de Escritura", "No se pudo guardar el archivo: " + e.getMessage());
        }
    }
    // --- MÉTODOS DE SOPORTE ---

    private static CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private static void askToOpenFile(File file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reporte Guardado");
        alert.setHeaderText("¡Archivo generado con éxito!");
        alert.setContentText("¿Deseas abrir el archivo de Excel ahora?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (java.awt.Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (IOException ex) {
                    showErrorAlert("Error al abrir", "No se pudo abrir el archivo automáticamente.");
                }
            }
        }
    }

    private static void setupColumnWidths(Sheet sheet, int numCols, int width) {
        for (int i = 0; i < numCols; i++) {
            sheet.setColumnWidth(i, width * 256);
        }
    }

    private static Row getOrCreateRow(Sheet s, int i) {
        Row r = s.getRow(i);
        return (r == null) ? s.createRow(i) : r;
    }

    private static void setupCell(Row r, int c, Object val, CellStyle s) {
        Cell cell = r.createCell(c);
        if (val instanceof Number) {
            cell.setCellValue(((Number) val).doubleValue());
        } else {
            cell.setCellValue(val != null ? val.toString() : "");
        }
        cell.setCellStyle(s);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    private static CellStyle createBoxStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}