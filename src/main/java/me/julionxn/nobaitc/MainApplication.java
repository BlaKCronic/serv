package me.julionxn.nobaitc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        double[] size = { 1240, 680 };
        FXMLLoader fxmlLoader = new FXMLLoader(getResourceURL("fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), size[0], size[1]);
        stage.setTitle("Diseño Experimental | Generador de Fracciones");
        Image icon = new Image(getResourceURL("images/icons8-fraction-48.png").toString());
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.show();
    }
    @Override
    public void stop() throws Exception {
        me.julionxn.nobaitc.util.AppExecutor.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static URL getResourceURL(String path) {
        return MainApplication.class.getResource(path);
    }
}