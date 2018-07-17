package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import net.dongliu.commons.Strings;
import net.dongliu.commons.io.Readers;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.store.BodyType;
import net.dongliu.proxy.ui.UIUtils;
import net.dongliu.proxy.ui.beautifier.*;
import net.dongliu.proxy.utils.Storages;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * For http/web-socket body
 *
 * @author Liu Dong
 */
public class BodyPane extends BorderPane {
    @FXML
    private Label sizeLabel;
    @FXML
    private ComboBox<BodyType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;
    @FXML
    private ToggleButton beautifyButton;

    private ObjectProperty<Body> body = new SimpleObjectProperty<>();

    public BodyPane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/body.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        body.addListener((o, old, newValue) -> {
            try {
                if (newValue != null) {
                    beautifyButton.selectedProperty().setValue(newValue.beautify());
                } else {
                    beautifyButton.selectedProperty().set(false);
                }
                refreshBody(newValue);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        charsetBox.getItems().addAll(UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII,
                StandardCharsets.ISO_8859_1, Charset.forName("GB18030"), Charset.forName("GBK"),
                Charset.forName("GB2312"), Charset.forName("BIG5")
        );
        bodyTypeBox.getItems().addAll(BodyType.values());
        beautifyButton.selectedProperty().addListener((ob, old, value) -> {
            try {
                Body body = this.body.get();
                body.beautify(value);
                refreshBody(body);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static final List<Beautifier> beautifiers = List.of(
            new JsonBeautifier(),
            new FormEncodedBeautifier(),
            new XMLBeautifier(),
            new HtmlBeautifier()
    );

    private void refreshBody(Body body) throws IOException {
        if (body == null) {
            this.setCenter(new Text());
            return;
        }

        BodyType bodyType = body.type();
        Charset charset = body.charset().orElse(UTF_8);

        charsetBox.setValue(charset);
        charsetBox.setManaged(bodyType.isText());
        charsetBox.setVisible(bodyType.isText());
        sizeLabel.setText(Storages.toHumanReadableSize(body.size()));

        bodyTypeBox.setValue(bodyType);

        if (!body.finished()) {
            this.setCenter(new Text("Still reading..."));
            return;
        }

        if (body.size() == 0) {
            this.setCenter(new Text());
            return;
        }

        // handle images
        if (bodyType.isImage()) {
            Node imagePane = UIUtils.getImagePane(body.getDecodedInputStream(), bodyType);
            this.setCenter(imagePane);
            return;
        }

        // textual body
        if (bodyType.isText()) {
            String text;
            try (InputStream input = body.getDecodedInputStream();
                 Reader reader = new InputStreamReader(input, charset)) {
                text = Readers.readAll(reader);
            }

            // beautify
            if (body.beautify()) {
                for (Beautifier beautifier : beautifiers) {
                    if (beautifier.accept(bodyType)) {
                        text = beautifier.beautify(text, charset);
                        break;
                    }
                }
            }

            TextArea textArea = new TextArea();
            textArea.setText(text);
            textArea.setEditable(false);
            this.setCenter(textArea);
            return;
        }

        // do not know how to handle
        Text t = new Text();
        long size = body.size();
        if (size > 0) {
            t.setText("Binary Body");
        }
        this.setCenter(t);
    }

    @FXML
    private void exportBody(ActionEvent e) throws IOException {
        Body body = this.body.get();
        if (body == null || body.size() == 0) {
            UIUtils.showMessageDialog("This http message has nobody");
            return;
        }

//        String fileName = suggestFileName(body.url(), body.type());
        //TODO: get url here
        String fileName = addExtension("", body.type());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        try (InputStream in = body.getDecodedInputStream();
             OutputStream out = new FileOutputStream(file)) {
            in.transferTo(out);
        }
        UIUtils.showMessageDialog("Export Finished!");
    }


    private static String suggestFileName(String url, BodyType type) {
        url = Strings.subStringBefore(url, "?");
        String fileName = Strings.subStringAfterLast(url, "/");
        if (fileName.isEmpty()) {
            fileName = Strings.subStringBeforeLast(url, "/");
            fileName = Strings.subStringAfterLast(fileName, "/");
            fileName = fileName.replace(".", "_");
        }
        if (!fileName.contains(".")) {
            fileName = addExtension(fileName, type);
        }
        return fileName;
    }

    private static String addExtension(String fileName, BodyType type) {
        switch (type) {
            case html:
                return fileName + ".html";
            case xml:
                return fileName + ".xml";
            case json:
                return fileName + ".json";
            case text:
            case www_form:
                return fileName + ".txt";
            case jpeg:
                return fileName + ".jpeg";
            case png:
                return fileName + ".png";
            case gif:
                return fileName + ".gif";
            case bmp:
                return fileName + ".bmp";
            default:
                return fileName;

        }
    }

    @FXML
    private void setMimeType(ActionEvent e) throws IOException {
        Body body = this.body.get();
        if (body == null) {
            return;
        }
        body.type(bodyTypeBox.getSelectionModel().getSelectedItem());
        if (body.finished() && body.size() != 0) {
            refreshBody(body);
        }
    }

    @FXML
    private void setCharset(ActionEvent e) throws IOException {
        Body body = this.body.get();
        if (body == null) {
            return;
        }
        body.charset(charsetBox.getSelectionModel().getSelectedItem());
        if (body.finished() && body.size() != 0 && body.type().isText()) {
            refreshBody(body);
        }
    }

    public Body getBody() {
        return body.get();
    }

    public void setBody(Body body) {
        this.body.set(body);
    }
}
