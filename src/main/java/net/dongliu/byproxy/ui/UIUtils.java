package net.dongliu.byproxy.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.dongliu.byproxy.store.BodyType;
import net.dongliu.byproxy.ui.component.ProgressDialog;
import net.sf.image4j.codec.ico.ICODecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Liu Dong
 */
public class UIUtils {

    private static final Logger logger = LoggerFactory.getLogger(UIUtils.class);

    /**
     * Get a Pane show otherImage, in center
     */
    public static Node getImagePane(InputStream in, BodyType type) {
        if (type == BodyType.jpeg || type == BodyType.png || type == BodyType.gif
                || type == BodyType.bmp || type == BodyType.icon) {
            Image image;
            if (type == BodyType.icon) {
                try {
                    image = getIconImage(in);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                image = new Image(in);
            }

            ImageView imageView = new ImageView();
            imageView.setImage(image);

            ScrollPane scrollPane = new ScrollPane();
            StackPane stackPane = new StackPane(imageView);
            stackPane.minWidthProperty().bind(Bindings.createDoubleBinding(() ->
                    scrollPane.getViewportBounds().getWidth(), scrollPane.viewportBoundsProperty()));
            stackPane.minHeightProperty().bind(Bindings.createDoubleBinding(() ->
                    scrollPane.getViewportBounds().getHeight(), scrollPane.viewportBoundsProperty()));

            scrollPane.setContent(stackPane);
            scrollPane.setStyle("-fx-background-color:transparent");
            return scrollPane;
        } else {
            return new Text("Unsupported image format");
        }
    }

    private static Image getIconImage(InputStream in) throws IOException {
        List<BufferedImage> images = ICODecoder.read(in);
        BufferedImage image = Collections.max(images, Comparator.comparingInt(BufferedImage::getWidth));
        return SwingFXUtils.toFXImage(image, null);
    }

    public static void showMessageDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText(message);
            alert.setHeaderText("");
            alert.showAndWait();
        });
    }

    public static void copyToClipBoard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }


    public static <T> void runBackground(Task<T> task, String failedMessage) {
        ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.bindTask(task);

        task.setOnSucceeded(e -> Platform.runLater(progressDialog::close));
        task.setOnFailed(e -> {
            Platform.runLater(progressDialog::close);
            Throwable throwable = task.getException();
            logger.error(failedMessage, throwable);
            UIUtils.showMessageDialog(failedMessage + ": " + throwable.getMessage());
        });

        Thread thread = new Thread(task);
        thread.start();
        progressDialog.show();
    }

    public static ObservableValue<Boolean> observeNull(ObservableValue<?> observable) {
        return Bindings.createBooleanBinding(() -> observable.getValue() == null, observable);
    }
}
