package net.dongliu.byproxy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Liu Dong
 */
@Slf4j
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("ByProxy");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        root.requestFocus();

        stage.setOnCloseRequest(e -> {
            ShutdownHooks.shutdownAll();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
