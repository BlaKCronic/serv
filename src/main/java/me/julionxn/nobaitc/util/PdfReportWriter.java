package me.julionxn.nobaitc.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.julionxn.nobaitc.data.nonbpa.FractionResult;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class PdfReportWriter {

    public static void generateReport(List<FractionResult> tableData, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        // Archivo en inglés
        fileChooser.setInitialFileName("Fractions_Report.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("REPORTE DE FRACCIONES")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(18));

            for (FractionResult res : tableData) {
                document.add(new Paragraph("\n"));

                document.add(new Paragraph("Fracción " + res.getFractionNumber())
                        .setBold()
                        .setFontSize(14)
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setPaddingLeft(5));

                Table summaryTable = new Table(4);
                summaryTable.setWidth(UnitValue.createPercentValue(100));
                summaryTable.addCell(new Cell().add(new Paragraph("J2:")).setBold());
                summaryTable.addCell(new Cell().add(new Paragraph(String.valueOf(res.getJ2()))));
                summaryTable.addCell(new Cell().add(new Paragraph("GBM:")).setBold());
                summaryTable.addCell(new Cell().add(new Paragraph(String.valueOf(res.getGbm()))));
                document.add(summaryTable);

                document.add(new Paragraph("Matrices y VIFS:").setItalic().setFontSize(10));

                double[][] matrix = res.getFraction();
                double[] vifs = res.getVifs();

                int matrixCols = (matrix != null && matrix.length > 0) ? matrix[0].length : 0;
                Table dataTable = new Table(UnitValue.createPercentArray(matrixCols + 1));
                dataTable.setWidth(UnitValue.createPercentValue(100));

                for (int i = 0; i < matrixCols; i++) {
                    dataTable.addHeaderCell(new Cell().add(new Paragraph("Dato " + (i + 1))).setBold().setBackgroundColor(ColorConstants.GRAY));
                }
                dataTable.addHeaderCell(new Cell().add(new Paragraph("VIFS")).setBold().setBackgroundColor(ColorConstants.GRAY));

                int rows = Math.max(matrix != null ? matrix.length : 0, vifs != null ? vifs.length : 0);

                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < matrixCols; j++) {
                        if (matrix != null && i < matrix.length && j < matrix[i].length) {
                            dataTable.addCell(new Cell().add(new Paragraph(String.valueOf(matrix[i][j]))));
                        } else {
                            dataTable.addCell(new Cell().add(new Paragraph("-")));
                        }
                    }
                    if (vifs != null && i < vifs.length) {
                        dataTable.addCell(new Cell().add(new Paragraph(String.valueOf(vifs[i]))));
                    } else {
                        dataTable.addCell(new Cell().add(new Paragraph("-")));
                    }
                }

                document.add(dataTable);
            }

            document.close();
            askToOpenFile(file);

            // ¡IMPORTANTE! Capturamos Throwable para ver si falta alguna clase de iText
        } catch (Throwable e) {
            showErrorAlert("Error de Escritura", "No se pudo generar el PDF: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void askToOpenFile(File file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Report Saved");
        alert.setHeaderText("PDF generated successfully!");
        alert.setContentText("Do you want to open the PDF file now?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (java.awt.Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception ex) {
                    showErrorAlert("Error", "Could not open the file automatically.");
                }
            }
        }
    }

    private static void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}