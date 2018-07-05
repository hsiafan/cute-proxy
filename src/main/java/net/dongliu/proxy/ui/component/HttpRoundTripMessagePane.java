package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import net.dongliu.proxy.data.HttpMessage;

import java.io.IOException;

/**
 * Panel show Http message(request and response)
 *
 * @author Liu Dong
 */
public class HttpRoundTripMessagePane extends SplitPane {
    @FXML
    private HttpMessagePane requestBodyPane;
    @FXML
    private HttpMessagePane responseBodyPane;

    private ObjectProperty<HttpMessage> roundTripMessage = new SimpleObjectProperty<>();

    public HttpRoundTripMessagePane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_round_trip_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        roundTripMessage.addListener((o, old, newValue) -> {
            requestBodyPane.setHeaders(newValue.requestHeader());
            responseBodyPane.setHeaders(newValue.ResponseHeader());
            requestBodyPane.setBody(newValue.requestBody());
            responseBodyPane.setBody(newValue.responseBody());
        });
    }

    public void setRoundTripMessage(HttpMessage httpMessage) {
        this.roundTripMessage.set(httpMessage);
    }
}
