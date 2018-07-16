package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import net.dongliu.proxy.data.HttpHeaders;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.utils.NameValues;

import java.io.IOException;

/**
 * Http message(request or response)
 *
 * @author Liu Dong
 */
public class HttpMessagePane extends TabPane {
    @FXML
    private TextArea cookieText;
    @FXML
    private TextArea headerText;
    @FXML
    private BodyPane bodyPane;

    private ObjectProperty<HttpHeaders> headers = new SimpleObjectProperty<>();

    public HttpMessagePane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        headers.addListener((o, old, message) -> {
            if (message == null) {
                headerText.clear();
                headerText.clear();
                return;
            }
            var rawHeaders = headers.get().rawLines();
            String headerString = String.join("\n", rawHeaders);
            headerText.setText(headerString);
            var cookies = NameValues.toAlignText(headers.get().cookieValues(), " = ");
            cookieText.setText(String.join("\n", cookies));

        });

    }

    public Body getBody() {
        return bodyPane.getBody();
    }

    public void setBody(Body body) {
        bodyPane.setBody(body);
    }

    public void setHeaders(HttpHeaders httpHeaders) {
        this.headers.set(httpHeaders);
    }
}
