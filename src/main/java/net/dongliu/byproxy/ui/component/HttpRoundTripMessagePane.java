package net.dongliu.byproxy.ui.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import net.dongliu.byproxy.parser.HttpRoundTripMessage;
import net.dongliu.commons.functional.UnChecked;

/**
 * Panel show Http message(request and response)
 *
 * @author Liu Dong
 */
public class HttpRoundTripMessagePane extends SplitPane {
    public HttpMessagePane requestPane;
    public HttpMessagePane responsePane;

    private ObjectProperty<HttpRoundTripMessage> roundTripMessage = new SimpleObjectProperty<>();

    public HttpRoundTripMessagePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_round_trip_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        UnChecked.run(fxmlLoader::load);
    }

    @FXML
    void initialize() {
        roundTripMessage.addListener((o, old, newValue) -> {
            requestPane.setHttpMessage(newValue.getRequest());
            responsePane.setHttpMessage(newValue.getResponse());
        });
    }

    public void setRoundTripMessage(HttpRoundTripMessage httpRoundTripMessage) {
        this.roundTripMessage.set(httpRoundTripMessage);
    }
}
