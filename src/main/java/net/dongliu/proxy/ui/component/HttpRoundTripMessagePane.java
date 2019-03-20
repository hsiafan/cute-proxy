package net.dongliu.proxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import net.dongliu.proxy.data.HttpMessage;

import java.io.IOException;

import static net.dongliu.proxy.ui.RequestCopyUtils.*;

/**
 * Panel show Http message(request and response)
 *
 * @author Liu Dong
 */
public class HttpRoundTripMessagePane extends BorderPane {
    @FXML
    private HttpMessagePane requestBodyPane;
    @FXML
    private HttpMessagePane responseBodyPane;

    private ObjectProperty<HttpMessage> httpMessage = new SimpleObjectProperty<>();

    public HttpRoundTripMessagePane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_round_trip_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        httpMessage.addListener((o, old, newValue) -> {
            requestBodyPane.setHeaders(newValue.requestHeader());
            responseBodyPane.setHeaders(newValue.responseHeader());
            requestBodyPane.setBody(newValue.requestBody());
            responseBodyPane.setBody(newValue.responseBody());
        });
    }

    public void setHttpMessage(HttpMessage httpMessage) {
        this.httpMessage.set(httpMessage);
    }

    @FXML
    private void copyAsCurl(ActionEvent e) {
        copyRequestAsCurl(httpMessage.get());
    }

    @FXML
    private void copyAsPython(ActionEvent e) {
        copyRequestAsPython(httpMessage.get());
    }

    @FXML
    private void copyAsJava(ActionEvent e) {
        copyRequestAsJava(httpMessage.get());
    }
}
