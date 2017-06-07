package net.dongliu.byproxy.ui.component;

import net.dongliu.byproxy.parser.HttpMessage;
import net.dongliu.byproxy.parser.ResponseHeaders;
import net.dongliu.byproxy.store.BodyStore;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import net.dongliu.commons.exception.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Liu Dong
 */
public class HttpMessagePane extends SplitPane {
    private static final Logger logger = LoggerFactory.getLogger(HttpMessagePane.class);
    @FXML
    private SplitPane requestPane;
    @FXML
    private SplitPane responsePane;
    @FXML
    private HttpHeadersPane responseHeaderPane;
    @FXML
    private BodyPane requestBodyPane;
    @FXML
    private HttpHeadersPane requestsHeaderPane;
    @FXML
    private BodyPane responseBodyPane;

    private ObjectProperty<HttpMessage> httpMessage = new SimpleObjectProperty<>();

    public HttpMessagePane() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/http_message.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw Throwables.throwAny(e);
        }
    }

    @FXML
    void initialize() {
        httpMessage.addListener((o, old, newValue) -> {
            requestsHeaderPane.setHeaders(newValue.getRequestHeaders());
            BodyStore requestBody = newValue.getRequestBody();
            if (requestBody == null || requestBody.size() == 0) {
                requestPane.getItems().remove(requestBodyPane);
            } else {
                requestBodyPane.setBody(requestBody);
                if (!requestPane.getItems().contains(requestBodyPane)) {
                    requestPane.getItems().add(requestBodyPane);
                }
            }
            ResponseHeaders responseHeaders = newValue.getResponseHeaders();
            if (responseHeaders != null) {
                responseHeaderPane.setHeaders(responseHeaders);
                BodyStore responseBody = newValue.getResponseBody();
                if (responseBody == null || (responseBody.isClosed() && responseBody.size() == 0)) {
                    responsePane.getItems().remove(responseBodyPane);
                } else {
                    responseBodyPane.setBody(responseBody);
                    if (!responsePane.getItems().contains(responseBodyPane)) {
                        responsePane.getItems().add(responseBodyPane);
                    }
                }
            }
        });
    }

    public HttpMessage getHttpMessage() {
        return httpMessage.get();
    }

    public ObjectProperty<HttpMessage> httpMessageProperty() {
        return httpMessage;
    }

    public void setHttpMessage(HttpMessage httpMessage) {
        this.httpMessage.set(httpMessage);
    }
}
