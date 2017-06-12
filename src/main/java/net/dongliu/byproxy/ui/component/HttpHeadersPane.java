package net.dongliu.byproxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import net.dongliu.byproxy.parser.Headers;
import net.dongliu.commons.Joiner;
import net.dongliu.commons.functional.UnChecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * @author Liu Dong
 */
public class HttpHeadersPane extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(HttpHeadersPane.class);
    @FXML
    private TextArea cookieText;
    @FXML
    private TextArea rawHeadersText;

    private ObjectProperty<Headers> headers = new SimpleObjectProperty<>();

    public HttpHeadersPane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_headers.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        UnChecked.run(fxmlLoader::load);
    }

    private static Joiner joiner = Joiner.of("\n");

    @FXML
    void initialize() {
        headers.addListener((o, old, newValue) -> {
            rawHeadersText.setText(joiner.join(newValue.toRawLines()));
            String s = newValue.getCookieValues().stream()
                    .map(c -> c.getName() + "=" + c.getValue())
                    .collect(Collectors.joining("\n"));
            cookieText.setText(s);
        });
    }

    public Headers getHeaders() {
        return headers.get();
    }

    public ObjectProperty<Headers> headersProperty() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers.set(headers);
    }
}
