package me.julionxn.nobaitc.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.julionxn.nobaitc.MainApplication;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private VBox mainVBox;

    @FXML
    private Button nonbpaBtn, otherBtn, aliasBtn;
    private List<Button> moduleButtons;
    private final Map<String, Node> viewCache = new HashMap<>();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupModuleButtons();
        moduleButtons = List.of(nonbpaBtn, aliasBtn, otherBtn);
        nonbpaBtn.setUserData((Runnable) this::loadNONBPAModule);
        aliasBtn.setUserData((Runnable) this::loadAliasModule);
        otherBtn.setUserData((Runnable) this::showComingSoon);
        for (Button btn : moduleButtons) {
            if (!btn.getStyleClass().contains("module-button")) {
                btn.getStyleClass().add("module-button");
            }
            btn.setOnAction(event -> {
                handleActiveState(btn);
                Runnable action = (Runnable) btn.getUserData();
                if (action != null) {
                    action.run();
                }
            });
        }
        handleActiveState(nonbpaBtn);
        loadNONBPAModule();
    }

    private void handleActiveState(Button selectedBtn) {
        for (Button btn : moduleButtons) {
            btn.getStyleClass().remove("active");
        }
        if (!selectedBtn.getStyleClass().contains("active")) {
            selectedBtn.getStyleClass().add("active");
        }
        selectedBtn.requestFocus();
    }
//    private void setupWelcomeScreen() {
//        welcomeLabel.setText("Bienvenido al Sistema NOBA-ITC");
//
//        String welcomeText = """
//                Módulos disponibles:
//                - Generador NONBPA
//
//                Selecciona un módulo del panel izquierdo para comenzar.
//                """;
//
//        infoTextArea.setText(welcomeText);
//        infoTextArea.setEditable(false);
//        infoTextArea.setWrapText(true);
//    }

    // Crear botones para cada módulo
    private void setupModuleButtons() {
        configureButton(nonbpaBtn, "NONBPA", "Genera fracciones...", this::loadNONBPAModule);
        configureButton(aliasBtn, "Alias Structure", "...", this::loadAliasModule);
        configureButton(otherBtn, "Otro", "Próximamente...", this::showComingSoon);
    }

    private Button createModuleButton(String title, String smallDescription, String moduleDescription, Runnable action) {
        Button button = new Button();
        button.setText(title);
        button.setTooltip(new Tooltip(smallDescription));
        button.setMaxWidth(Double.MAX_VALUE);

        button.setOnAction(e -> action.run());
//        button.setOnMouseEntered(e -> infoTextArea.setText(moduleDescription));
        return button;
    }

    private void configureButton(Button button, String title, String smallDescription, Runnable action) {
        if (button == null) return;
        button.setText(title);
        button.setTooltip(new Tooltip(smallDescription));
        button.setMaxWidth(Double.MAX_VALUE);
    }

    @FXML
    public void loadNONBPAModule() {
        mainVBox.getChildren().clear();
        loadModule("nonbpa-view.fxml");
    }

    public void loadAliasModule() {
        mainVBox.getChildren().clear();
        loadModule("alias-structure-view.fxml");
    }

    private void loadModule(String viewFile) {
        try {
            Node view;
            Object controller = null;

            // Si ya existe en memoria, lo recuperamos junto con su controlador
            if (viewCache.containsKey(viewFile)) {
                view = viewCache.get(viewFile);
                controller = view.getUserData(); // <-- Recuperamos el controlador del caché
            } else {
                // Si no existe, lo cargamos por primera vez
                FXMLLoader loader = new FXMLLoader(MainApplication.getResourceURL("fxml/" + viewFile));
                view = loader.load();
                controller = loader.getController();

                // <-- EL TRUCO: Guardamos el controlador dentro del Node
                view.setUserData(controller);

                viewCache.put(viewFile, view);
            }

            // Aquí hacemos la inyección si el controlador es el de NONBPA
            if (controller instanceof NONBPAController) {
                ((NONBPAController) controller).setMainController(this);
            }

            // Intercambiamos el contenido
            mainVBox.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error al cargar el módulo " + viewFile, e.getMessage());
        }
    }

    private void showComingSoon() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Próximamente");
        alert.setHeaderText("Módulo en desarrollo");
        alert.setContentText("Este módulo estará disponible en futuras versiones.");
        alert.showAndWait();
    }

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Sistema de Diseño Experimental");
        alert.setContentText("""
            Versión: 1.0.0
            
            Sistema para análisis y generación de diseños experimentales.
            Desarrollado para facilitar la creación de fracciones factoriales
            con métricas de balance y ortogonalidad.
            """);
        alert.showAndWait();
    }

    @FXML
    private void exitApplication() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salir");
        alert.setHeaderText("¿Está seguro que desea salir?");
        alert.setContentText("Se perderán los datos no guardados.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.exit(0);
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void loadAliasModuleWithData(String matrixData) {
        String viewFile = "alias-structure-view.fxml";
        try {
            Node view;
            Object controller = null;

            if (viewCache.containsKey(viewFile)) {
                view = viewCache.get(viewFile);
                controller = view.getUserData();
            } else {
                FXMLLoader loader = new FXMLLoader(MainApplication.getResourceURL("fxml/" + viewFile));
                view = loader.load();
                controller = loader.getController();
                view.setUserData(controller);
                viewCache.put(viewFile, view);
            }

            if (controller instanceof AliasStructureController) {
                ((AliasStructureController) controller).setMatrixData(matrixData);
            }

            // 1. Cambiamos la vista
            mainVBox.getChildren().setAll(view);

            forceAliasButtonActive();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error al cargar el módulo " + viewFile, e.getMessage());
        }
    }

    private void forceAliasButtonActive() {
        if (nonbpaBtn != null) nonbpaBtn.getStyleClass().remove("active");
        if (otherBtn != null) otherBtn.getStyleClass().remove("active");
        if (aliasBtn != null && !aliasBtn.getStyleClass().contains("active")) {
            aliasBtn.getStyleClass().add("active");
        }
    }
}