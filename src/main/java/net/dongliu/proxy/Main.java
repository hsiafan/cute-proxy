package net.dongliu.proxy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author Liu Dong
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Cute Proxy");
        stage.setMaximized(true);
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/icon.png")));
        stage.setScene(scene);

        stage.show();
        root.requestFocus();

        stage.setOnCloseRequest(e -> {
            CloseHooks.executeTasks();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
