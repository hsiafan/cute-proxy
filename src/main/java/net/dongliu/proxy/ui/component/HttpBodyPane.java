package net.dongliu.proxy.ui.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import net.dongliu.commons.io.Readers;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.store.BodyType;
import net.dongliu.proxy.ui.UIUtils;
import net.dongliu.proxy.ui.beautifier.*;
import net.dongliu.proxy.utils.Strings;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * For http/web-socket body
 *
 * @author Liu Dong
 */
public class HttpBodyPane extends BorderPane {
    @FXML
    private Label sizeLabel;
    @FXML
    private ComboBox<BodyType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;

    private ObjectProperty<Body> body = new SimpleObjectProperty<>();
    private BooleanProperty beautify = new SimpleBooleanProperty();

    public HttpBodyPane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_body.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        body.addListener((o, old, newValue) -> {
            try {
                refreshBody(newValue);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        charsetBox.getItems().addAll(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII,
                StandardCharsets.ISO_8859_1, Charset.forName("GB18030"), Charset.forName("GBK"),
                Charset.forName("GB2312"), Charset.forName("BIG5")
        );
        bodyTypeBox.getItems().addAll(BodyType.values());
        beautify.addListener((ob, old, value) -> {
            try {
                refreshBody(body.get());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static final Map<BodyType, Beautifier> beautifiers = Map.of(
            BodyType.json, new JsonBeautifier(),
            BodyType.www_form, new FormEncodedBeautifier(),
            BodyType.xml, new XMLBeautifier(),
            BodyType.html, new HtmlBeautifier()
    );

    private void refreshBody(Body body) throws IOException {
        if (body == null) {
            this.setCenter(new Text());
            return;
        }

        BodyType storeType = body.getType();

        charsetBox.setValue(body.getCharset());
        charsetBox.setManaged(storeType.isText());
        charsetBox.setVisible(storeType.isText());
        sizeLabel.setText(Strings.humanReadableSize(body.size()));

        bodyTypeBox.setValue(storeType);

        if (!body.isFinished()) {
            this.setCenter(new Text("Still reading..."));
            return;
        }

        if (body.size() == 0) {
            this.setCenter(new Text());
            return;
        }

        // handle images
        if (storeType.isImage()) {
            Node imagePane = UIUtils.getImagePane(body.getDecodedInputStream(), storeType);
            this.setCenter(imagePane);
            return;
        }

        // textual body
        if (storeType.isText()) {
            String text;
            try (InputStream input = body.getDecodedInputStream();
                 Reader reader = new InputStreamReader(input, body.getCharset())) {
                text = Readers.readAll(reader);
            }

            // beautify
            if (beautify.get()) {
                Beautifier beautifier = beautifiers.get(storeType);
                if (beautifier != null) {
                    text = beautifier.beautify(text, body.getCharset());
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

//        String fileName = suggestFileName(body.getUrl(), body.getType());
        //TODO: get url here
        String fileName = addExtension("", body.getType());

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
        url = Strings.before(url, "?");
        String fileName = Strings.afterLast(url, "/");
        if (fileName.isEmpty()) {
            fileName = Strings.beforeLast(url, "/");
            fileName = Strings.afterLast(fileName, "/");
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
        body.setType(bodyTypeBox.getSelectionModel().getSelectedItem());
        if (body.isFinished() && body.size() != 0) {
            refreshBody(body);
        }
    }

    @FXML
    private void setCharset(ActionEvent e) throws IOException {
        Body body = this.body.get();
        if (body == null) {
            return;
        }
        body.setCharset(charsetBox.getSelectionModel().getSelectedItem());
        if (body.isFinished() && body.size() != 0 && body.getType().isText()) {
            refreshBody(body);
        }
    }

    public Body getBody() {
        return body.get();
    }

    public void setBody(Body body) {
        this.body.set(body);
    }

    public BooleanProperty beautifyProperty() {
        return beautify;
    }
}
