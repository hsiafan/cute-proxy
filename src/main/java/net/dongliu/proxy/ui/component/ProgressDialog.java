package net.dongliu.proxy.ui.component;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Process dialog
 *
 * @author Liu Dong
 */
public class ProgressDialog {
    private final Stage stage;
    private final Label label;
    private final ProgressBar progressBar = new ProgressBar();

    public ProgressDialog() {
        stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox vBox = new VBox();
        vBox.setSpacing(5);
        vBox.setAlignment(Pos.CENTER);
        label = new Label();
        vBox.getChildren().add(label);

        progressBar.setProgress(-1F);
        progressBar.setPrefWidth(400);
        vBox.getChildren().add(progressBar);

        Scene scene = new Scene(vBox);
        stage.setScene(scene);
    }

    public void bindTask(final Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        label.textProperty().bind(task.messageProperty());
    }

    public void close() {
        stage.close();
    }

    public void show() {
        stage.show();
    }
}
