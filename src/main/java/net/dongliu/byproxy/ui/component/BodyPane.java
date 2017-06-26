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
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import lombok.SneakyThrows;
import net.dongliu.byproxy.store.BodyStore;
import net.dongliu.byproxy.store.BodyStoreType;
import net.dongliu.byproxy.ui.UIUtils;
import net.dongliu.byproxy.ui.beautifier.*;
import net.dongliu.byproxy.utils.StringUtils;

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
    private ComboBox<BodyStoreType> bodyTypeBox;
    @FXML
    private ComboBox<Charset> charsetBox;
    @FXML
    private ToggleButton beautifyButton;

    private ObjectProperty<BodyStore> body = new SimpleObjectProperty<>();

    @SneakyThrows
    public BodyPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_body.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    void initialize() {
        body.addListener((o, old, newValue) -> refreshBody(newValue));

        charsetBox.getItems().addAll(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII,
                StandardCharsets.ISO_8859_1,
                Charset.forName("GB18030"), Charset.forName("GBK"), Charset.forName("GB2312"),
                Charset.forName("BIG5"));
        bodyTypeBox.getItems().addAll(BodyStoreType.values());
    }

    private static final Map<BodyStoreType, Beautifier> beautifiers = ImmutableMap.of(
            BodyStoreType.json, new JsonBeautifier(),
            BodyStoreType.www_form, new FormEncodedBeautifier(),
            BodyStoreType.xml, new XMLBeautifier(),
            BodyStoreType.html, new HtmlBeautifier()
    );

    @SneakyThrows
    private void refreshBody(@Nullable BodyStore bodyStore) {
        if (bodyStore == null) {
            this.setCenter(new Text());
            return;
        }

        BodyStoreType storeType = bodyStore.getType();

        charsetBox.setValue(bodyStore.getCharset());
        charsetBox.setManaged(storeType.isText());
        charsetBox.setVisible(storeType.isText());

        boolean showBeautify = beautifiers.containsKey(storeType);
        beautifyButton.setSelected(bodyStore.isBeautify());
        beautifyButton.setManaged(showBeautify);
        beautifyButton.setVisible(showBeautify);

        bodyTypeBox.setValue(storeType);

        if (!bodyStore.isClosed()) {
            this.setCenter(new Text("Still reading..."));
            return;
        }

        if (bodyStore.size() == 0) {
            this.setCenter(new Text());
            return;
        }

        // handle images
        if (storeType.isImage()) {
            Node imagePane = UIUtils.getImagePane(bodyStore.finalInputStream(), storeType);
            this.setCenter(imagePane);
            return;
        }

        // textual body
        if (storeType.isText()) {
            String text;
            try (Reader reader = new InputStreamReader(bodyStore.finalInputStream(), bodyStore.getCharset())) {
                text = CharStreams.toString(reader);
            }

            // beautify
            if (bodyStore.isBeautify()) {
                Beautifier beautifier = beautifiers.get(storeType);
                text = beautifier.beautify(text, bodyStore.getCharset());
            }

            TextArea textArea = new TextArea();
            textArea.setText(text);
            textArea.setEditable(false);
            this.setCenter(textArea);
            return;
        }

        // do not know how to handle
        Text t = new Text();
        long size = bodyStore.size();
        if (size > 0) {
            t.setText("Binary Body");
        }
        this.setCenter(t);
    }

    @FXML
    void exportBody(ActionEvent e) throws IOException {
        BodyStore bodyStore = body.get();
        if (bodyStore == null || bodyStore.size() == 0) {
            UIUtils.showMessageDialog("This http message has nobody");
            return;
        }

        String fileName = suggestFileName(bodyStore.getUrl(), bodyStore.getType());
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file == null) {
            return;
        }
        try (InputStream in = bodyStore.finalInputStream();
             OutputStream out = new FileOutputStream(file)) {
            ByteStreams.copy(in, out);
        }
        UIUtils.showMessageDialog("Export Finished!");
    }


    static String suggestFileName(String url, BodyStoreType type) {
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

    private static String addExtension(String fileName, BodyStoreType type) {
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
        BodyStore bodyStore = body.get();
        if (bodyStore == null) {
            return;
        }
        bodyStore.setType(bodyTypeBox.getSelectionModel().getSelectedItem());
        if (bodyStore.isClosed() && bodyStore.size() != 0) {
            refreshBody(bodyStore);
        }
    }

    @FXML
    void setCharset(ActionEvent e) throws IOException {
        BodyStore bodyStore = body.get();
        if (bodyStore == null) {
            return;
        }
        bodyStore.setCharset(charsetBox.getSelectionModel().getSelectedItem());
        if (bodyStore.isClosed() && bodyStore.size() != 0 && bodyStore.getType().isText()) {
            refreshBody(bodyStore);
        }
    }

    @FXML
    void beautify(ActionEvent e) throws IOException {
        BodyStore bodyStore = body.get();
        if (bodyStore == null) {
            return;
        }
        bodyStore.setBeautify(beautifyButton.isSelected());
        if (bodyStore.isClosed() && bodyStore.size() != 0 && bodyStore.getType().isText()) {
            refreshBody(bodyStore);
        }
    }

    public BodyStore getBody() {
        return body.get();
    }

    public ObjectProperty<BodyStore> bodyProperty() {
        return body;
    }

    public void setBody(BodyStore body) {
        this.body.set(body);
    }
}
