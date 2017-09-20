package net.dongliu.byproxy.ui.component;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
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
import net.dongliu.byproxy.store.HttpBody;
import net.dongliu.byproxy.store.HttpBodyType;
import net.dongliu.byproxy.ui.UIUtils;
import net.dongliu.byproxy.ui.beautifier.*;
import net.dongliu.byproxy.utils.StringUtils;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * For http/web-socket body
 *
 * @author Liu Dong
 */
public class BodyPane extends BorderPane {
    @FXML
    private Label sizeLabel;
    @FXML
    private ComboBox<HttpBodyType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;
    @FXML
    private ToggleButton beautifyButton;

    private ObjectProperty<HttpBody> body = new SimpleObjectProperty<>();

    public BodyPane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_body.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    void initialize() {
        body.addListener((o, old, newValue) -> {
            try {
                refreshBody(newValue);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        charsetBox.getItems().addAll(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII,
                StandardCharsets.ISO_8859_1,
                Charset.forName("GB18030"), Charset.forName("GBK"), Charset.forName("GB2312"),
                Charset.forName("BIG5"));
        bodyTypeBox.getItems().addAll(HttpBodyType.values());
    }

    private static final Map<HttpBodyType, Beautifier> beautifiers = ImmutableMap.of(
            HttpBodyType.json, new JsonBeautifier(),
            HttpBodyType.www_form, new FormEncodedBeautifier(),
            HttpBodyType.xml, new XMLBeautifier(),
            HttpBodyType.html, new HtmlBeautifier()
    );

    private void refreshBody(@Nullable HttpBody httpBody) throws IOException {
        if (httpBody == null) {
            this.setCenter(new Text());
            return;
        }

        HttpBodyType storeType = httpBody.getType();

        charsetBox.setValue(httpBody.getCharset());
        charsetBox.setManaged(storeType.isText());
        charsetBox.setVisible(storeType.isText());
        sizeLabel.setText(FileUtils.byteCountToDisplaySize(httpBody.size()));

        boolean showBeautify = beautifiers.containsKey(storeType);
        beautifyButton.setSelected(httpBody.isBeautify());
        beautifyButton.setManaged(showBeautify);
        beautifyButton.setVisible(showBeautify);

        bodyTypeBox.setValue(storeType);

        if (!httpBody.isFinished()) {
            this.setCenter(new Text("Still reading..."));
            return;
        }

        if (httpBody.size() == 0) {
            this.setCenter(new Text());
            return;
        }

        // handle images
        if (storeType.isImage()) {
            Node imagePane = UIUtils.getImagePane(httpBody.getDecodedInputStream(), storeType);
            this.setCenter(imagePane);
            return;
        }

        // textual body
        if (storeType.isText()) {
            String text;
            try (InputStream input = httpBody.getDecodedInputStream();
                 Reader reader = new InputStreamReader(input, httpBody.getCharset())) {
                text = CharStreams.toString(reader);
            }

            // beautify
            if (httpBody.isBeautify()) {
                Beautifier beautifier = beautifiers.get(storeType);
                text = beautifier.beautify(text, httpBody.getCharset());
            }

            TextArea textArea = new TextArea();
            textArea.setText(text);
            textArea.setEditable(false);
            this.setCenter(textArea);
            return;
        }

        // do not know how to handle
        Text t = new Text();
        long size = httpBody.size();
        if (size > 0) {
            t.setText("Binary Body");
        }
        this.setCenter(t);
    }

    @FXML
    void exportBody(ActionEvent e) throws IOException {
        HttpBody httpBody = body.get();
        if (httpBody == null || httpBody.size() == 0) {
            UIUtils.showMessageDialog("This http message has nobody");
            return;
        }

//        String fileName = suggestFileName(httpBody.getUrl(), httpBody.getType());
        //TODO: get url here
        String fileName = addExtension("", httpBody.getType());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        try (InputStream in = httpBody.getDecodedInputStream();
             OutputStream out = new FileOutputStream(file)) {
            ByteStreams.copy(in, out);
        }
        UIUtils.showMessageDialog("Export Finished!");
    }


    private static String suggestFileName(String url, HttpBodyType type) {
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

    private static String addExtension(String fileName, HttpBodyType type) {
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
    void setMimeType(ActionEvent e) throws IOException {
        HttpBody httpBody = body.get();
        if (httpBody == null) {
            return;
        }
        httpBody.setType(bodyTypeBox.getSelectionModel().getSelectedItem());
        if (httpBody.isFinished() && httpBody.size() != 0) {
            refreshBody(httpBody);
        }
    }

    @FXML
    void setCharset(ActionEvent e) throws IOException {
        HttpBody httpBody = body.get();
        if (httpBody == null) {
            return;
        }
        httpBody.setCharset(charsetBox.getSelectionModel().getSelectedItem());
        if (httpBody.isFinished() && httpBody.size() != 0 && httpBody.getType().isText()) {
            refreshBody(httpBody);
        }
    }

    @FXML
    void beautify(ActionEvent e) throws IOException {
        HttpBody httpBody = body.get();
        if (httpBody == null) {
            return;
        }
        httpBody.setBeautify(beautifyButton.isSelected());
        if (httpBody.isFinished() && httpBody.size() != 0 && httpBody.getType().isText()) {
            refreshBody(httpBody);
        }
    }

    public HttpBody getBody() {
        return body.get();
    }

    public ObjectProperty<HttpBody> bodyProperty() {
        return body;
    }

    public void setBody(HttpBody body) {
        this.body.set(body);
    }
}
