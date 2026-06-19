package me.julionxn.nobaitc.controllers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.julionxn.nobaitc.MainApplication;
import me.julionxn.nobaitc.data.nonbpa.NONBPAGeneratorService;
import me.julionxn.nobaitc.data.nonbpa.FractionResult;
import me.julionxn.nobaitc.util.ClipboardHelper;
import me.julionxn.nobaitc.util.ExcelWriter;
import me.julionxn.nobaitc.util.PdfReportWriter;
import org.controlsfx.control.spreadsheet.Grid;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador para la interfaz de generación de fracciones NONBPA.
 * Optimizado para mejor rendimiento y mantenibilidad.
 */
public class NONBPAController implements Initializable {

    // ==================== FXML Components ====================

    @FXML private GridPane factorsInputContainer;
    @FXML private Button removeFactorButton;
    @FXML private TextField fractionSizeField;
    @FXML private TextField numberOfFractionsField;
    @FXML private RadioButton randomFractionsRadio;
    @FXML private RadioButton customFractionsRadio;
    @FXML private TextField customFractionsField;
    @FXML private VBox tableViewBox, tableFractionsNONBPABox;
    @FXML private ScrollPane inputDataScrollPane;
    @FXML private Button dataInputViewBtn;
    @FXML private Button dataTableViewBtn;

    @FXML private Button sendToAliasBtn;
    @FXML private Button toExcelBtn, toPdfBtn;
    @FXML private Button copyTableBtn;

    @FXML private Label trLabel;
    @FXML private Label factorsCountLabel;
    @FXML private Label lcmLabel;
    @FXML private Label glLabel;
    @FXML private Label sfMinLabel;

    @FXML private TableView<FractionResult> resultsTable;
    @FXML private TableColumn<FractionResult, Integer> fractionNumberColumn;
    @FXML private TableColumn<FractionResult, String> fractionDataColumn;
    @FXML private TableColumn<FractionResult, Double> gbmColumn;
    @FXML private TableColumn<FractionResult, Double> j2Column;
    @FXML private TableColumn<FractionResult, Double> vifsColumn;

    private IntegerProperty sfmin = new SimpleIntegerProperty(0);

    @FXML private TextArea logTextArea;

    // ==================== Services & Data ====================

    private final NONBPAGeneratorService generatorService;
    private final ObservableList<FractionResult> fractionResults;

    // Constantes
    private static final int MIN_FACTORS = 1;
    private static final int INITIAL_FACTORS = 2;
    private static final int MAX_FACTORS = 15;
    private static final String NUMERIC_REGEX = "\\d*";
    private static final String NUMBER_FORMAT = "%.4f";

    public NONBPAController() {
        this.generatorService = new NONBPAGeneratorService();
        this.fractionResults = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupTableColumns();
        setupValidation();
        addInitialFactors();
        updateDesignInfo();
        handleShowDataInput();
        resultsTable.getItems().addListener((ListChangeListener.Change<? extends FractionResult> change) -> {
                    autoSizeColumn(fractionDataColumn);
                    autoSizeColumn(vifsColumn);
        });

        sfmin.addListener((observable, oldValue, newValue) -> {
            fractionSizeField.setText(String.valueOf(newValue));
        });
    }

    // ==================== UI Setup ====================

    private void setupUI() {
        setupRadioButtons();
        setupResultsTable();
        setupLogArea();
        setupCustomFractionsField();
    }

    private void setupRadioButtons() {
        ToggleGroup fractionTypeGroup = new ToggleGroup();
        randomFractionsRadio.setToggleGroup(fractionTypeGroup);
        customFractionsRadio.setToggleGroup(fractionTypeGroup);
        randomFractionsRadio.setSelected(true);
    }

    private void setupResultsTable() {
        resultsTable.setItems(fractionResults);
        resultsTable.setRowFactory(this::createTableRowFactory);
    }

    private TableRow<FractionResult> createTableRowFactory(TableView<FractionResult> tv) {
        TableRow<FractionResult> row = new TableRow<>();

        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                FractionResult rowData = row.getItem();
                handleFractionDoubleClick(rowData);
            }
        });

        return row;
    }

    private void handleFractionDoubleClick(FractionResult fractionResult) {
        copyFractionToClipboard(fractionResult);
        openDetailsWindow(fractionResult);
//        StringBuilder log = new StringBuilder();
//        log.append("Se copio la columna exitosamente!\n\n");
//        logTextArea.setText(log.toString());
//        handleCopyToClipboard();
    }

    private void setupLogArea() {
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
    }

    private void setupCustomFractionsField() {
        customFractionsRadio.selectedProperty().addListener((obs, oldVal, newVal) ->
                customFractionsField.setDisable(!newVal)
        );
        customFractionsField.setDisable(true);
    }

    private void setupTableColumns() {
        // Configurar value factories
        fractionNumberColumn.setCellValueFactory(new PropertyValueFactory<>("fractionNumber"));
        fractionDataColumn.setCellValueFactory(new PropertyValueFactory<>("fractionData"));
        gbmColumn.setCellValueFactory(new PropertyValueFactory<>("gbm"));
        j2Column.setCellValueFactory(new PropertyValueFactory<>("j2"));
        vifsColumn.setCellValueFactory(new PropertyValueFactory<>("vifsData"));

        // Formatear columnas numéricas
        setupNumericColumn(gbmColumn);
        setupNumericColumn(j2Column);
    }

    private void setupNumericColumn(TableColumn<FractionResult, Double> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format(NUMBER_FORMAT, value));
            }
        });
    }

    private void setupValidation() {
        addNumericValidation(fractionSizeField, true);
        addNumericValidation(numberOfFractionsField, false);
    }

    private void addNumericValidation(TextField field, boolean updateDesignInfo) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches(NUMERIC_REGEX)) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (updateDesignInfo) {
                updateDesignInfo();
            }
        });
    }

    // ==================== Factor Management ====================

    private void addInitialFactors() {
        for (int i = 0; i < INITIAL_FACTORS; i++) {
            addFactorInput();
        }
    }

    @FXML
    private void addFactorInput() {
        int currentSize = factorsInputContainer.getChildren().size();

        if (currentSize >= MAX_FACTORS) {
            return;
        }

        // 1. Quitar el evento de clic al último levelField (si existe)
        if (currentSize > 0) {
            // En un GridPane, los hijos mantienen el orden en el que fueron agregados
            VBox lastBox = (VBox) factorsInputContainer.getChildren().get(currentSize - 1);
            TextField lastLevelField = (TextField) lastBox.getChildren().get(1);
            lastLevelField.setOnMouseClicked(null);
        }

        // 2. Calcular la posición en el Grid (Máximo 5 columnas)
        int columna = currentSize % 5;
        int renglon = currentSize / 5;

        // 3. Crear el nuevo factor
        VBox factorBox = createFactorInputBox(currentSize + 1);

        // 4. Agregarlo al GridPane especificando columna y renglón
        factorsInputContainer.add(factorBox, columna, renglon);

        updateRemoveButtonState();
        updateDesignInfo();
    }

    private VBox createFactorInputBox(int factorNumber) {
        VBox factorBox = new VBox(0);

        Label label = new Label("Factor " + factorNumber + ":");
        label.getStyleClass().add("mathLabel");
        label.setMinWidth(128);

        factorBox.setAlignment(Pos.CENTER_LEFT);
        factorBox.setSpacing(4);

        TextField levelField = createLevelTextField();
        levelField.setMinWidth(64);

        // El detector de clic para generar un nuevo factor
        levelField.setOnMouseClicked(event -> addFactorInput());
        factorBox.getChildren().addAll(label, levelField);

        return factorBox;
    }

    private TextField createLevelTextField() {
        TextField levelField = new TextField();
        levelField.setPromptText("Niveles");
        levelField.setPrefWidth(100);

        levelField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches(NUMERIC_REGEX)) {
                levelField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            updateDesignInfo();
        });

        return levelField;
    }

    @FXML
    private void removeFactorInput() {
        if (factorsInputContainer.getChildren().size() > MIN_FACTORS) {
            // 1. Remover el último elemento
            int lastIndex = factorsInputContainer.getChildren().size() - 1;
            factorsInputContainer.getChildren().remove(lastIndex);

            // 2. Recuperar el "nuevo" último elemento para reactivar su evento
            int newSize = factorsInputContainer.getChildren().size();
            if (newSize > 0) {
                // Obtenemos el VBox que ahora quedó al final
                VBox newLastBox = (VBox) factorsInputContainer.getChildren().get(newSize - 1);

                // Extraemos su TextField (índice 1)
                TextField newLastLevelField = (TextField) newLastBox.getChildren().get(1);

                // Le volvemos a asignar la capacidad de crear nuevos factores
                newLastLevelField.setOnMouseClicked(event -> addFactorInput());
            }

            updateRemoveButtonState();
            updateDesignInfo();
        }
    }

    private void updateRemoveButtonState() {
        removeFactorButton.setDisable(
                factorsInputContainer.getChildren().size() <= MIN_FACTORS
        );
    }

    // ==================== Design Info ====================

    private void updateDesignInfo() {
        try {
            int[] design = getDesignArray();

            if (design.length == 0) {
                clearDesignInfo();
                return;
            }

            NONBPAGeneratorService.DesignParameters params =
                    generatorService.calculateParameters(design);

            displayDesignParameters(params);
            displayValidationStatus(design, params);

        } catch (Exception e) {
            clearDesignInfo();
        }
    }

    public void setSfmin(int value) {
        sfmin.set(value);
    }
    private void displayDesignParameters(NONBPAGeneratorService.DesignParameters params) {
        trLabel.setText("Número de corridas (TR): " + params.tr());
        factorsCountLabel.setText("Factores: " + params.factors());
        lcmLabel.setText("Mínimo común múltiplo (LCM): " + params.lcm());
        glLabel.setText("Grados de libertad (GL): " + params.gl());
        int sfmin = params.sfMin();
        setSfmin(sfmin);
        sfMinLabel.setText("Tamaño minimo de la fraccion (SF min): " + sfmin);
    }

    private void displayValidationStatus(int[] design, NONBPAGeneratorService.DesignParameters params) {
        boolean isValid = generatorService.validateDesign(design);
        String status = isValid ?
                "Diseño válido" :
                "Diseño no válido (TR ≠ LCM o > " + MAX_FACTORS + " factores)";

        logTextArea.setText("Estado: " + status);
    }

    private void clearDesignInfo() {
        trLabel.setText("Número de corridas (TR): -");
        factorsCountLabel.setText("Factores: -");
        lcmLabel.setText("Mínimo común múltiplo (LCM): -");
        glLabel.setText("Grados de libertad (GL): -");
        sfMinLabel.setText("Tamaño minimo de la fraccion (SF min): -");
        setSfmin(0);
        logTextArea.clear();
    }

    // ==================== Fraction Generation ====================

    @FXML
    private void generateFractions() {
        try {
            int[] design = validateAndGetDesign();
            int fractionSize = parseIntegerField(fractionSizeField, "Tamaño de fracción");
            int numberOfFractions = parseIntegerField(numberOfFractionsField, "Número de fracciones");

            List<FractionResult> results = generateFractionsBasedOnMode(
                    design, fractionSize, numberOfFractions
            );
            displayResults(results);
            handleOpenPreview();
            enableButtons();

        } catch (NumberFormatException e) {
            showError("Error de entrada", "Verifique que todos los campos numéricos sean válidos");
        } catch (IllegalArgumentException e) {
            showError("Error de validación", e.getMessage());
        } catch (Exception e) {
            showError("Error", "Error al generar fracciones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int[] validateAndGetDesign() {
        int[] design = getDesignArray();

        if (design.length == 0) {
            throw new IllegalArgumentException("Ingrese al menos un factor con niveles válidos");
        }

        if (!generatorService.validateDesign(design)) {
            throw new IllegalArgumentException("El diseño no es válido para NONBPA");
        }

        return design;
    }

    private int parseIntegerField(TextField field, String fieldName) {
        String text = field.getText().trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }

        return Integer.parseInt(text);
    }

    private List<FractionResult> generateFractionsBasedOnMode(
            int[] design, int fractionSize, int numberOfFractions) {

        if (randomFractionsRadio.isSelected()) {
            return generatorService.generateRandomFractions(
                    design, fractionSize, numberOfFractions
            );
        } else {
            List<Integer> customFractions = parseCustomFractions(
                    customFractionsField.getText()
            );
            return generatorService.generateCustomFractions(
                    design, fractionSize, customFractions
            );
        }
    }

    private void displayResults(List<FractionResult> results) {
        fractionResults.clear();
        fractionResults.addAll(results);

        logTextArea.setText(buildResultsSummary(results));
    }

    private String buildResultsSummary(List<FractionResult> results) {
        StringBuilder log = new StringBuilder();
        log.append("Generación completada exitosamente!\n\n");
        log.append("Resumen de fracciones generadas:\n");
        log.append("─".repeat(50)).append("\n");

        for (FractionResult result : results) {
            log.append(String.format(
                    "Fracción %d - GBM: %.4f, J2: %.4f, Max VIF: %.4f\n",
                    result.getFractionNumber(),
                    result.getGbm(),
                    result.getJ2(),
                    Arrays.stream(result.getVifs()).max().orElse(0.0)
            ));
        }

        log.append("─".repeat(50)).append("\n");
        log.append("Total: ").append(results.size()).append(" fracciones\n");
//        log.append("\nDoble clic en una fila para ver detalles y copiar al portapapeles");

        return log.toString();
    }

    @FXML
    private void clearResults() {
        fractionResults.clear();
        logTextArea.clear();

        fractionSizeField.clear();
        numberOfFractionsField.clear();
        customFractionsField.clear();

        factorsInputContainer.getChildren().clear();
        addInitialFactors();
        clearDesignInfo();
        disableButtons();
        handleShowDataInput();
    }

    private int[] getDesignArray() {
        return factorsInputContainer.getChildren().stream()
                .map(node -> (VBox) node)
                .map(Vbox -> (TextField) Vbox.getChildren().get(1))
                .map(TextField::getText)
                .filter(text -> !text.isEmpty())
                .mapToInt(Integer::parseInt)
                .filter(level -> level > 0)
                .toArray();
    }

    private List<Integer> parseCustomFractions(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Campo de fracciones personalizadas vacío");
        }

        String cleaned = input.replaceAll("[\\[\\]\\s]", "");

        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public void copyFractionToClipboard(FractionResult fractionResult) {
        double[][] data = fractionResult.getFraction();
        StringBuilder sb = new StringBuilder();

        for (double[] row : data) {
            for (int i = 0; i < row.length; i++) {
                sb.append(row[i]);
                sb.append(i != row.length - 1 ? "\t" : "\n");
            }
        }

        ClipboardHelper.copyToClipboard(sb.toString());
    }

    private void openDetailsWindow(FractionResult data) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.getResourceURL("fxml/fraction-result-details.fxml")
            );
            Parent root = loader.load();
            FractionResultDetailsController controller = loader.getController();
            controller.setData(data);

            Stage stage = new Stage();
            stage.setTitle("Detalles - Fracción " + data.getFractionNumber());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            showError("Error", "No se pudo abrir la ventana de detalles");
            e.printStackTrace();
        }
    }

    // ==================== Excel Methods ====================
    @FXML
    private void handleExportExcel() {
        List<FractionResult> lista = resultsTable.getItems();
        if (lista.isEmpty()) return;

        Stage mainStage = (Stage) resultsTable.getScene().getWindow();

        ExcelWriter.generateReport(lista, mainStage);
    }
    // ============
    // ======== PDF Methods ====================
    @FXML
    private void handleExportPdf() {
        List<FractionResult> lista = resultsTable.getItems();
        if (lista.isEmpty()) return;
        Stage mainStage = (Stage) resultsTable.getScene().getWindow();
        PdfReportWriter.generateReport(lista, mainStage);
    }
    @FXML
    private void handleOpenPreview() {
        List<FractionResult> lista = resultsTable.getItems();
        if (lista.isEmpty()) return;
//        try {
//            FXMLLoader loader = new FXMLLoader(
//                    MainApplication.getResourceURL("fxml/excel-preview.fxml")
//            );
//            Parent root = loader.load();
//
//            ExcelPreviewController controller = loader.getController();
//            controller.initData(lista);
//
//            tableViewBox.getChildren().setAll(root);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        handleShowTableView();

    }

    // Este es el que vinculas al botón en el FXML
    @FXML
    private void handleOpenPreviewClick(ActionEvent event) {
        List<FractionResult> lista = resultsTable.getItems();
        // Llamas al método lógico pasándole la lista
        openExcelPreview(lista);
    }

    // Este método NO debe estar vinculado directamente al FXML si requiere la lista
    public void openExcelPreview(List<FractionResult> lista) {
        if (lista == null || lista.isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.getResourceURL("fxml/excel-preview.fxml"));
            Parent root = loader.load();
            ExcelPreviewController controller = loader.getController();
            controller.initData(lista);

            Stage previewStage = new Stage();
            previewStage.setScene(new javafx.scene.Scene(root));
            previewStage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
    private void handleCopyToClipboard() {
        // 1. Obtener el objeto seleccionado
        FractionResult selected = resultsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Atención", "Por favor, selecciona una fila de la tabla primero.");
            return;
        }

        // 2. Extraer todos los datos del objeto
        int fractionNumber = selected.getFractionNumber();
        double[][] matrix = selected.getFraction(); // Datos de la columna 'fractionDataColumn'
        double gbm = selected.getGbm();
        double j2 = selected.getJ2();
        double[] vifs = selected.getVifs();

        // 3. Construir la cadena de texto
        StringBuilder sb = new StringBuilder();

        // Encabezados o metadatos de la fila
        sb.append("Resumen de Fracción\n");
        sb.append("----------------------------\n");
        sb.append("Número de Fracción: ").append(fractionNumber).append("\n");
        sb.append("GBM: ").append(gbm).append("\n");
        sb.append("J2: ").append(j2).append("\n");
        sb.append("VIFs: ").append(java.util.Arrays.toString(vifs)).append("\n\n");

        // Título para la matriz
        sb.append("Datos de la Matriz:\n");

        // Recorremos la matriz
        if (matrix != null) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    sb.append(matrix[i][j]);
                    if (j < matrix[i].length - 1) {
                        sb.append("\t"); // Tabulador para saltar de celda en Excel
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("Sin datos de matriz disponibles.");
        }

        // 4. Copiar al portapapeles
        ClipboardHelper.copyToClipboard(sb.toString());

        System.out.println("Todos los datos de la fracción " + fractionNumber + " han sido copiados.");
    }
    // ==================== Alert Methods ====================

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type.toString());
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Variable para guardar la referencia al controlador principal
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void sendToAlias(ActionEvent event) {
        String matrixData = getMatrixDataAsString();
        if (mainController != null) {
            mainController.loadAliasModuleWithData(matrixData);
        } else {
            System.err.println("Error: MainController no está enlazado a NONBPAController.");
        }
    }

    // Reemplaza el método de prueba anterior por este:
    private String getMatrixDataAsString() {
        // 1. Obtenemos la fila seleccionada en tu tabla
        FractionResult selected = resultsTable.getSelectionModel().getSelectedItem();

        // 2. Si no hay nada seleccionado, devolvemos un texto vacío o un aviso
        if (selected == null) {
            System.out.println("No hay ninguna fracción seleccionada en la tabla.");
            return "";
        }

        // 3. Extraemos la matriz de la columna (asumiendo que getFraction() devuelve double[][])
        double[][] matrix = selected.getFraction();

        // (Opcional) Extraer variables extra si también las quieres en el TextArea
        // double j2 = selected.getJ2();
        // double gbm = selected.getGbm();

        // 4. Construimos el texto
        StringBuilder sb = new StringBuilder();

        // Si quieres incluir J2 y GBM al principio, descomenta estas dos líneas:
        // sb.append("J2: ").append(j2).append("\n");
        // sb.append("GBM: ").append(gbm).append("\n\n");

        // Recorremos la matriz para armar el texto con tabuladores y saltos de línea
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j]);
                // Agregamos un tabulador entre columnas
                if (j < matrix[i].length - 1) {
                    sb.append("\t");
                }
            }
            // Agregamos un salto de línea al final de cada fila
            sb.append("\n");
        }

        // 5. Devolvemos el texto real armado
        return sb.toString();
    }
    public void enableButtons() {
        sendToAliasBtn.setDisable(false);
        toExcelBtn.setDisable(false);
        toPdfBtn.setDisable(false);
        copyTableBtn.setDisable(false);
        dataTableViewBtn.setDisable(false);
        dataTableViewBtn.setVisible(true);
    }

    public void disableButtons() {
        sendToAliasBtn.setDisable(true);
        toExcelBtn.setDisable(true);
        toPdfBtn.setDisable(true);
        copyTableBtn.setDisable(true);
        dataTableViewBtn.setDisable(true);
        dataTableViewBtn.setVisible(false);
    }

    @FXML
    private void handleShowDataInput() {

        inputDataScrollPane.setVisible(true);
        inputDataScrollPane.setManaged(true);
        tableViewBox.setVisible(false);
        tableViewBox.setManaged(false);
        tableFractionsNONBPABox.setVisible(false);
        tableFractionsNONBPABox.setManaged(false);
        updateButtonsStyle(dataInputViewBtn, dataTableViewBtn);
    }

    @FXML
    private void handleShowTableView() {
        // Mostrar la tabla y ocultar el input
        tableViewBox.setVisible(true);
        tableViewBox.setManaged(true);
        tableFractionsNONBPABox.setVisible(true);
        tableFractionsNONBPABox.setManaged(true);
        inputDataScrollPane.setVisible(false);
        inputDataScrollPane.setManaged(false);

        updateButtonsStyle(dataTableViewBtn, dataInputViewBtn);
    }


    private void autoSizeColumn(TableColumn<FractionResult, ?> column) {
        Text headerText = new Text(column.getText());
        double maxWidth = headerText.getLayoutBounds().getWidth() + 35; // +35 para el padding y el ícono de ordenamiento

        Text dataTextNode = new Text();

        for (FractionResult item : resultsTable.getItems()) {
            if (column.getCellObservableValue(item) != null) {
                Object cellValue = column.getCellObservableValue(item).getValue();
                if (cellValue != null) {
                    dataTextNode.setText(cellValue.toString());
                    double width = dataTextNode.getLayoutBounds().getWidth() + 20; // +20 para el padding de las celdas

                    if (width > maxWidth) {
                        maxWidth = width;
                    }
                }
            }
        }
        column.setPrefWidth(maxWidth);
    }
}