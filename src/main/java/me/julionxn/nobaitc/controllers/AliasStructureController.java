package me.julionxn.nobaitc.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import me.julionxn.nobaitc.data.alias.AliasStructure;
import me.julionxn.nobaitc.data.alias.AliasStructureGenerator;
import me.julionxn.nobaitc.util.AppExecutor;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador para la vista de Estructura de Alias.
 * Acepta el formato de matriz MATLAB:
 *   [1 2 3 4 5 1 2 3 4 5; 1 2 3 4 5 6 7 1 2 3; 1 2 3 1 2 3 1 2 3 1]
 * y la transpone automáticamente (equivalente al ' de MATLAB).
 */
public class AliasStructureController implements Initializable {
    @FXML
    private Button clearGridPane;

    @FXML
    private GridPane gridPaneMatrix;

    @FXML
    private VBox gridpaneVBox;

    @FXML
    private TextArea inputMatrixArea;

    @FXML
    private TextField mInputTextField;

    @FXML
    private TextField nInputTextField;

    @FXML
    private VBox plainTextVBox;

    @FXML
    private TextArea resultArea;

    @FXML
    private Button showGridpaneBtn;

    @FXML
    private Button showPlainTextBtn;

    @FXML
    private Label statusLabel;

    private static final int MAX_SIZE = 120;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        mInputTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateMatrix();
        });
        nInputTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateMatrix();
        });
        mInputTextField.setText("3");
        nInputTextField.setText("2");
    }

    private void setupUI() {
        resultArea.setEditable(false);
        resultArea.setWrapText(false);

        inputMatrixArea.setPromptText(
                "Ingrese la fracción en formato MATLAB:\n" +
                        "[1 2 3 4 5 1 2 3 4 5; 1 2 3 4 5 6 7 1 2 3; 1 2 3 1 2 3 1 2 3 1]\n\n" +
                        "Cada FILA separada por  ;  (punto y coma).\n" +
                        "Los valores dentro de cada fila separados por COMA o espacio.\n\n" +
                        "⚠ Si omite el  ;  entre filas, todos los valores se tratan\n" +
                        "   como una sola fila y se suman como si fueran una sola fracción.\n\n" +
                        "La matriz se transpone automáticamente (equivalente a ' en MATLAB)."
        );
        showGridpane();
        statusLabel.setText("Ingrese la fracción y presione Calcular.");
    }

    // =========================================================================
    // Acciones
    // =========================================================================

    @FXML
    private void calculate() {
        String text = inputMatrixArea.getText().trim();
        if (text.isEmpty()) {
            showStatus("Error: ingrese una fracción.", true);
            return;
        }

        // 1. Parsear formato MATLAB → matriz double[][]
        double[][] matrizMATLAB = parseMatlabMatrix(text);
        if (matrizMATLAB == null) return;

        // 2. Transponer
        double[][] fraction = transpose(matrizMATLAB);

        // 3. Mostrar info de dimensiones
        int corridas = fraction.length;
        int factores = fraction[0].length;
        showStatus(
                "Fracción: " + corridas + " corridas × " + factores + " factores. Calculando...",
                false
        );

        // === INICIO DEL CRONÓMETRO ===
        long startTime = System.currentTimeMillis();

        // 4. Calcular estructura de alias
        AppExecutor.execute(
                () -> {
                    AliasStructureGenerator generator = new AliasStructureGenerator(fraction);
                    return generator.generate();
                },
                (structure) -> {
                    // === FIN DEL CRONÓMETRO (ÉXITO) ===
                    long endTime = System.currentTimeMillis();
                    long executionTime = endTime - startTime;

                    displayResult(structure, corridas, factores);
                    showStatus(
                            "Estructura calculada en " + executionTime + " ms. (" + corridas + " × " + factores + ")",
                            false
                    );
                },
                (error) -> {
                    // === FIN DEL CRONÓMETRO (ERROR) ===
                    long endTime = System.currentTimeMillis();
                    long executionTime = endTime - startTime;

                    showStatus("Error tras " + executionTime + " ms: " + error.getMessage(), true);
                    resultArea.setText("Error:\n\n" + error.getMessage());

                    if (!(error instanceof IllegalStateException)) {
                        error.printStackTrace();
                    }
                }
        );
    }

    @FXML
    private void calculateGridPane() {
        String mText = mInputTextField.getText();
        String nText = nInputTextField.getText();

        if (mText == null || mText.trim().isEmpty() || nText == null || nText.trim().isEmpty()) {
            showStatus("Error: Defina las dimensiones de la matriz primero.", true);
            return;
        }

        int m, n;
        try {
            m = Integer.parseInt(mText);
            n = Integer.parseInt(nText);
        } catch (NumberFormatException e) {
            showStatus("Error: Las dimensiones deben ser enteras.", true);
            return;
        }

        double[][] matrizExtraida = new double[m][n];

        try {
            for (Node node : gridPaneMatrix.getChildren()) {
                if (node instanceof TextField) {
                    TextField cell = (TextField) node;

                    Integer rowIndex = GridPane.getRowIndex(node);
                    Integer colIndex = GridPane.getColumnIndex(node);

                    int row = (rowIndex == null) ? 0 : rowIndex;
                    int col = (colIndex == null) ? 0 : colIndex;

                    if (row < m && col < n) {
                        String cellValue = cell.getText().trim();
                        double value = cellValue.isEmpty() ? 0.0 : Double.parseDouble(cellValue);
                        matrizExtraida[row][col] = value;
                    }
                }
            }
        } catch (NumberFormatException e) {
            showStatus("Error: Asegúrese de que todas las celdas contengan números válidos.", true);
            return;
        }

        // 2. Transponer
        double[][] fraction = transpose(matrizExtraida);

        // 3. Mostrar info de dimensiones
        int corridas = fraction.length;
        int factores = fraction[0].length;
        showStatus(
                "Fracción: " + corridas + " corridas × " + factores + " factores. Calculando...",
                false
        );

        // === INICIO DEL CRONÓMETRO ===
        long startTime = System.currentTimeMillis();

        // 4. Calcular estructura de alias
        AppExecutor.execute(
                () -> {
                    AliasStructureGenerator generator = new AliasStructureGenerator(fraction);
                    return generator.generate();
                },
                (structure) -> {
                    // === FIN DEL CRONÓMETRO (ÉXITO) ===
                    long endTime = System.currentTimeMillis();
                    long executionTime = endTime - startTime;

                    displayResult(structure, corridas, factores);
                    showStatus(
                            "Estructura calculada en " + executionTime + " ms. (" + corridas + " × " + factores + ")",
                            false
                    );
                },
                (error) -> {
                    // === FIN DEL CRONÓMETRO (ERROR) ===
                    long endTime = System.currentTimeMillis();
                    long executionTime = endTime - startTime;

                    showStatus("Error tras " + executionTime + " ms: " + error.getMessage(), true);
                    resultArea.setText("Error:\n\n" + error.getMessage());

                    if (!(error instanceof IllegalStateException)) {
                        error.printStackTrace();
                    }
                }
        );
    }
    @FXML
    private void clear() {
        inputMatrixArea.clear();
        resultArea.clear();
        showStatus("Ingrese la fracción y presione Calcular.", false);
    }
    @FXML
    private void clearGridPane() {
        for (Node node : gridPaneMatrix.getChildren()) {
            if (node instanceof TextField) {
                TextField cell = (TextField) node;
                cell.clear(); // o cell.setText("");
            }
        }
        if (resultArea != null) {
            resultArea.clear();
        }
        showStatus("Matriz limpiada. Ingrese los valores y presione Calcular.", false);
        mInputTextField.setText("4");
        nInputTextField.setText("2");
    }

    // =========================================================================
    // Parser — formato MATLAB
    // =========================================================================

    /**
     * Parsea una cadena en formato MATLAB a double[][].
     *
     * Formatos aceptados:
     *   [1 2 3; 4 5 6; 7 8 9]     → con corchetes
     *   1 2 3; 4 5 6; 7 8 9       → sin corchetes
     *   1 2 3\n4 5 6\n7 8 9       → saltos de línea como separador de filas
     *   1,2,3;4,5,6;7,8,9         → comas como separador de valores
     *
     * La matriz resultante NO está transpuesta todavía.
     */
    private double[][] parseMatlabMatrix(String text) {
        // Quitar corchetes [ ]
        text = text.replaceAll("[\\[\\]]", "").trim();

        // Normalizar: saltos de línea también actúan como separador de fila (;)
        text = text.replaceAll("\\r?\\n", ";");

        // Eliminar múltiples ; consecutivos y ; al inicio/fin
        text = text.replaceAll(";+", ";").trim();
        text = text.replaceAll("^;|;$", "");

        // Separar filas por ;
        String[] filas = text.split(";");

        List<double[]> rows = new ArrayList<>();
        int cols = -1;

        for (int i = 0; i < filas.length; i++) {
            String fila = filas[i].trim();
            if (fila.isEmpty()) continue;

            // Separar valores por espacios o comas
            String[] partes = fila.trim().split("[\\s,]+");

            if (cols == -1) {
                cols = partes.length;
            } else if (partes.length != cols) {
                showStatus(
                    "Error: la fila " + (i + 1) + " tiene " + partes.length +
                    " valores pero se esperaban " + cols + ".",
                    true
                );
                return null;
            }

            double[] row = new double[cols];
            for (int j = 0; j < cols; j++) {
                try {
                    row[j] = Double.parseDouble(partes[j]);
                } catch (NumberFormatException e) {
                    showStatus(
                        "Error: '" + partes[j] + "' no es un número válido (fila " + (i + 1) +
                        ", columna " + (j + 1) + ").",
                        true
                    );
                    return null;
                }
            }
            rows.add(row);
        }

        if (rows.isEmpty()) {
            showStatus("Error: no se encontraron datos en la entrada.", true);
            return null;
        }

        return rows.toArray(new double[0][]);
    }

    /**
     * Transpone una matriz double[][].
     * Equivalente al operador ' de MATLAB.
     */
    private double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    // =========================================================================
    // Display — formato horizontal
    // Ejemplo: A = +1.0000·AB + 0.5000·ABC
    // =========================================================================

    private void displayResult(AliasStructure structure, int corridas, int factores) {
        StringBuilder sb = new StringBuilder();

        double[][] msz     = structure.getMatrizAlias();
        String[]   efectos = structure.getEfectos();
        String[][] letras  = structure.getMatrizLetras();
        int A = msz.length;
        int L = efectos.length;

        sb.append("==============================================\n");
        sb.append(" ESTRUCTURA DE ALIAS\n");
        sb.append(" Diseño: ").append(corridas).append(" corridas × ")
          .append(factores).append(" factores\n");
        sb.append(" Efectos totales (L): ").append(L).append("\n");
        sb.append("==============================================\n\n");

        if (structure.isOrthogonal()) {
            sb.append("El diseño es ORTOGONAL — no existen alias entre efectos.\n\n");
        } else {
            sb.append("Efectos con alias: ").append(structure.getAliasCount()).append("\n\n");
        }

        // Una línea por efecto:  EFECTO  =  ±coef·ALIAS1  ±coef·ALIAS2  ...
        for (int x = 0; x < L; x++) {
            // Recolectar todos los términos de la columna x
            List<String> terminos = new ArrayList<>();
            for (int xx = 0; xx < A; xx++) {
                double val = msz[xx][x];
                if (val == 0) continue;

                String signo  = val >= 0 ? "+" : "-";
                double absVal = Math.abs(val);
                String etiq   = letras[xx][x];

                // Si el coeficiente es exactamente 1, no mostrarlo
                String coefStr = (Math.abs(absVal - 1.0) < 0.0001)
                    ? signo + etiq
                    : String.format("%s%.4f·%s", signo, absVal, etiq);

                terminos.add(coefStr);
            }

            if (terminos.isEmpty()) continue;

            // Construir la línea:  EFECTO  =  término1  término2 ...
            sb.append(String.format("%-6s = ", efectos[x]));
            for (int t = 0; t < terminos.size(); t++) {
                if (t == 0) {
                    // Primer término: quitar el '+' inicial si es positivo
                    String term = terminos.get(t);
                    sb.append(term.startsWith("+") ? term.substring(1) : term);
                } else {
                    // Resto: separar con espacio (el signo ya está incluido)
                    sb.append("  ").append(terminos.get(t));
                }
            }
            sb.append("\n");
        }

        sb.append("\n==============================================\n");

        resultArea.setText(sb.toString());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #cc0000; -fx-font-weight: bold;"
            : "-fx-text-fill: #006600;");
    }

    public void setMatrixData(String data) {
        if (data == null || data.trim().isEmpty()) return;

        // 1. Llenar el TextArea (Texto plano)
        if (inputMatrixArea != null) {
            inputMatrixArea.setText(data);
        }

        // 2. Parsear el String para obtener la matriz numérica
        // Dividimos por líneas y luego por espacios/tabulaciones/comas
        String[] lines = data.trim().split("\\n");
        int rows = lines.length;
        int cols = lines[0].trim().split("[\\s,]+").length;

        // 3. Actualizar los campos de texto de dimensiones (M y N)
        mInputTextField.setText(String.valueOf(rows));
        nInputTextField.setText(String.valueOf(cols));

        // 4. Generar el GridPane visualmente
        updateMatrix();

        // 5. Poblar el GridPane con los datos del String
        populateGridPaneFromText(lines, rows, cols);
    }

    private void populateGridPaneFromText(String[] lines, int m, int n) {
        for (int i = 0; i < m; i++) {
            String[] values = lines[i].trim().split("[\\s,]+");
            for (int j = 0; j < n; j++) {
                if (j < values.length) {
                    String val = values[j];
                    // Buscamos el TextField en la posición (j, i) del GridPane
                    TextField cell = getTextFieldFromGrid(j, i);
                    if (cell != null) {
                        cell.setText(val);
                    }
                }
            }
        }
    }

    // Método utilitario para encontrar un nodo específico por coordenadas en el GridPane
    private TextField getTextFieldFromGrid(int col, int row) {
        for (Node node : gridPaneMatrix.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r != null && r == row && c != null && c == col && node instanceof TextField) {
                return (TextField) node;
            }
        }
        return null;
    }

    private void updateMatrix() {
        gridPaneMatrix.getChildren().clear();
        String mText = mInputTextField.getText();
        String nText = nInputTextField.getText();
        if (mText == null || mText.trim().isEmpty() || nText == null || nText.trim().isEmpty()) {
            return;
        }
        try {
            int m = Integer.parseInt(mText);
            int n = Integer.parseInt(nText);
            if (m <= 0 || n <= 0 || m > MAX_SIZE || n > MAX_SIZE) {
                return;
            }
            for (int i = 0; i < m; i++) {          // Rows
                for (int j = 0; j < n; j++) {      // Columns
                    TextField cell = new TextField();
                    cell.setMinWidth(50);
                    cell.setMaxWidth(50);
                    cell.setPromptText("[" + i + "," + j + "]");
                    gridPaneMatrix.add(cell, j, i); // Column (j) goes first, then Row (i)
                }
            }
        } catch (NumberFormatException e) {
        }
    }

    @FXML
    public void printMatrix() {
        String mText = mInputTextField.getText();
        String nText = nInputTextField.getText();

        // Validamos que haya dimensiones válidas antes de intentar leer
        if (mText == null || mText.trim().isEmpty() || nText == null || nText.trim().isEmpty()) {
            System.out.println("Please set the matrix dimensions first.");
            return;
        }

        try {
            int m = Integer.parseInt(mText);
            int n = Integer.parseInt(nText);

            // Creamos un arreglo 2D tradicional para organizar los datos visualmente
            String[][] matrixData = new String[m][n];

            // Recorremos todos los elementos (hijos) dentro del GridPane
            for (Node node : gridPaneMatrix.getChildren()) {
                // Nos aseguramos de que el nodo sea un TextField
                if (node instanceof TextField) {
                    TextField cell = (TextField) node;

                    // ¡TRUCO DE JAVAFX! Si un nodo está en la fila/columna 0, JavaFX puede devolver null.
                    Integer rowIndex = GridPane.getRowIndex(node);
                    Integer colIndex = GridPane.getColumnIndex(node);

                    int row = (rowIndex == null) ? 0 : rowIndex;
                    int col = (colIndex == null) ? 0 : colIndex;

                    // Nos aseguramos de no salirnos de los límites
                    if (row < m && col < n) {
                        String cellValue = cell.getText();
                        // Si la celda está vacía, le asignamos "0" por defecto para que la matriz se vea bien
                        matrixData[row][col] = (cellValue == null || cellValue.isEmpty()) ? "0" : cellValue;
                    }
                }
            }

            // Imprimimos la matriz en consola con formato tabular
            System.out.println("--- Current Matrix ---");
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    System.out.print(matrixData[i][j] + "\t"); // \t agrega un espacio de tabulación
                }
                System.out.println(); // Salto de línea al terminar una fila
            }
            System.out.println("----------------------");

        } catch (NumberFormatException e) {
            System.out.println("Error parsing dimensions. Cannot print matrix.");
        }
    }

    private void updateButtonsStyle(Button activeBtn, Button inactiveBtn) {
        activeBtn.getStyleClass().remove("tabSelectedBtn");
        activeBtn.getStyleClass().remove("tabUnselectedBtn");

        inactiveBtn.getStyleClass().remove("tabSelectedBtn");
        inactiveBtn.getStyleClass().remove("tabUnselectedBtn");

        activeBtn.getStyleClass().add("tabSelectedBtn");
        inactiveBtn.getStyleClass().add("tabUnselectedBtn");
    }

    @FXML
    private void showPlainText() {
        // Mostrar el input y ocultar la tabla
        plainTextVBox.setVisible(true);
        plainTextVBox.setManaged(true);
        gridpaneVBox.setVisible(false);
        gridpaneVBox.setManaged(false);
        updateButtonsStyle(showPlainTextBtn, showGridpaneBtn);
    }

    @FXML
    private void showGridpane() {
        gridpaneVBox.setVisible(true);
        gridpaneVBox.setManaged(true);
        plainTextVBox.setVisible(false);
        plainTextVBox.setManaged(false);
        updateButtonsStyle(showGridpaneBtn, showPlainTextBtn);
    }
}