package net.dongliu.byproxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;

import java.io.IOException;

/**
 * Panel show Http message(request and response)
 *
 * @author Liu Dong
 */
public class HttpRoundTripMessagePane extends SplitPane {
    @FXML
    private HttpHeadersPane requestHeaderPane;
    @FXML
    private HttpHeadersPane responseHeaderPane;
    @FXML
    private ToggleGroup bodyToggleGroup;
    @FXML
    private ToggleButton beautifyButton;
    @FXML
    private HttpBodyPane bodyPane;

    private ObjectProperty<HttpRoundTripMessage> roundTripMessage = new SimpleObjectProperty<>();

    public HttpRoundTripMessagePane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_round_trip_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    @FXML
    private void initialize() {
        roundTripMessage.addListener((o, old, newValue) -> {
            requestHeaderPane.setHttpHeader(newValue.getRequestHeader());
            responseHeaderPane.setHttpHeader(newValue.getResponseHeader());
            showHttpBody(bodyToggleGroup.getSelectedToggle());
        });
        bodyToggleGroup.selectedToggleProperty().addListener((ob, old, value) -> showHttpBody(value));
        beautifyButton.selectedProperty().bindBidirectional(bodyPane.beautifyProperty());
    }

    private void showHttpBody(Toggle value) {
        Object userData = value.getUserData();
        if (userData.equals("request")) {
            bodyPane.setBody(roundTripMessage.get().getRequestBody());
        } else if (userData.equals("response")) {
            bodyPane.setBody(roundTripMessage.get().getResponseBody());
        }
    }

    public void setRoundTripMessage(HttpRoundTripMessage httpRoundTripMessage) {
        this.roundTripMessage.set(httpRoundTripMessage);
    }
}
