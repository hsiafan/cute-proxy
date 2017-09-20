package net.dongliu.byproxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import net.dongliu.byproxy.parser.Headers;
import net.dongliu.byproxy.parser.HttpMessage;
import net.dongliu.byproxy.store.HttpBody;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Show one single http request or response
 *
 * @author Liu Dong
 */
public class HttpMessagePane extends SplitPane {
    public TextArea cookieText;
    public TextArea rawHeadersText;
    public BodyPane bodyPane;

    private ObjectProperty<HttpMessage> httpMessage = new SimpleObjectProperty<>();

    public HttpMessagePane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    void initialize() {
        httpMessage.addListener((o, old, message) -> {
            if (message == null) {
                rawHeadersText.clear();
                cookieText.clear();
                getItems().remove(bodyPane);
                return;
            }
            Headers headers = message.getHeaders();
            rawHeadersText.setText(String.join("\n", headers.toRawLines()));
            String s = headers.getCookieValues().stream()
                    .map(c -> c.getName() + "=" + c.getValue())
                    .collect(Collectors.joining("\n"));
            cookieText.setText(s);

            HttpBody body = message.getBody();

            if (body == null || body.size() == 0) {
                getItems().remove(bodyPane);
            } else {
                bodyPane.setBody(body);
                if (!getItems().contains(bodyPane)) {
                    getItems().add(bodyPane);
                }
            }
        });
    }

    public void setHttpMessage(HttpMessage message) {
        this.httpMessage.set(message);
    }
}
