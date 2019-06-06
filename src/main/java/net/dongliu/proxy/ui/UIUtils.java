package net.dongliu.proxy.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.dongliu.proxy.store.BodyType;
import net.dongliu.proxy.ui.component.ProgressDialog;
import net.dongliu.proxy.ui.ico.IconDecoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Comparator;

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
//        var images = ICODecoder.read(in);
//        var image = Collections.max(images, Comparator.comparingInt(BufferedImage::getWidth));

//        return SwingFXUtils.toFXImage(image, null);
        var image = Collections.max(IconDecoders.decode(in.readAllBytes()), Comparator.comparingDouble(Image::getWidth));
        return image;
    }

    public static void showMessageDialog(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText(message);
            alert.setHeaderText("");
            alert.showAndWait();
        });
    }

    /**
     * Copy text to clipboard
     */
    public static void copyToClipBoard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }


    /**
     * Run a task with process dialog
     */
    public static <T> void runTaskWithProcessDialog(Task<T> task, String failedMessage) {
        var progressDialog = new ProgressDialog();
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
