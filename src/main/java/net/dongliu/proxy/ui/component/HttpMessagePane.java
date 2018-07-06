package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import net.dongliu.commons.io.Readers;
import net.dongliu.proxy.data.HttpHeaders;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.store.BodyType;
import net.dongliu.proxy.ui.UIUtils;
import net.dongliu.proxy.ui.beautifier.*;
import net.dongliu.proxy.utils.StringUtils;
import net.dongliu.proxy.utils.Texts;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * For http/web-socket body
 *
 * @author Liu Dong
 */
public class HttpMessagePane extends TabPane {
    @FXML
    private TextArea cookieText;
    @FXML
    private TextArea headerText;
    @FXML
    private Label sizeLabel;
    @FXML
    private ComboBox<BodyType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;
    @FXML
    private ToggleButton beautifyButton;
    @FXML
    private BorderPane bodyPane;

    private ObjectProperty<HttpHeaders> headers = new SimpleObjectProperty<>();
    private ObjectProperty<Body> body = new SimpleObjectProperty<>();

    public HttpMessagePane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        // fxml cannot set font family?
//        headerText.setFont(Font.font("Monospaced", -1));
//        cookieText.setFont(Font.font("Monospaced", -1));

        headers.addListener((o, old, message) -> {
            if (message == null) {
                headerText.clear();
                cookieText.clear();
                return;
            }
            List<String> rawHeaders = Texts.toAlignText(headers.get().headers(), ": ");
            headerText.setText(String.join("\n", rawHeaders));
            List<String> cookies = Texts.toAlignText(headers.get().cookieValues(), "=");
            cookieText.setText(String.join("\n", cookies));
        });
        body.addListener((o, old, newValue) -> {
            try {
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
            bodyPane.setCenter(new Text());
            return;
        }

        BodyType storeType = body.type();

        charsetBox.setValue(body.charset().orElse(UTF_8));
        charsetBox.setManaged(storeType.isText());
        charsetBox.setVisible(storeType.isText());
        sizeLabel.setText(StringUtils.humanReadableSize(body.size()));

        bodyTypeBox.setValue(storeType);

        if (!body.finished()) {
            bodyPane.setCenter(new Text("Still reading..."));
            return;
        }

        if (body.size() == 0) {
            bodyPane.setCenter(new Text());
            return;
        }

        // handle images
        if (storeType.isImage()) {
            Node imagePane = UIUtils.getImagePane(body.getDecodedInputStream(), storeType);
            bodyPane.setCenter(imagePane);
            return;
        }

        // textual body
        if (storeType.isText()) {
            String text;
            try (InputStream input = body.getDecodedInputStream();
                 Reader reader = new InputStreamReader(input, body.charset().orElse(UTF_8))) {
                text = Readers.readAll(reader);
            }

            // beautify
            if (beautifyButton.selectedProperty().get()) {
                Beautifier beautifier = beautifiers.get(storeType);
                if (beautifier != null) {
                    text = beautifier.beautify(text, body.charset().orElse(UTF_8));
                }
            }

            TextArea textArea = new TextArea();
            textArea.setText(text);
            textArea.setEditable(false);
            bodyPane.setCenter(textArea);
            return;
        }

        // do not know how to handle
        Text t = new Text();
        long size = body.size();
        if (size > 0) {
            t.setText("Binary Body");
        }
        bodyPane.setCenter(t);
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
        url = StringUtils.before(url, "?");
        String fileName = StringUtils.afterLast(url, "/");
        if (fileName.isEmpty()) {
            fileName = StringUtils.beforeLast(url, "/");
            fileName = StringUtils.afterLast(fileName, "/");
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

    public void setHeaders(HttpHeaders httpHeaders) {
        this.headers.set(httpHeaders);
    }
}
