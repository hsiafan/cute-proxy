package net.dongliu.byproxy.ui;

import net.dongliu.byproxy.parser.*;
import net.dongliu.byproxy.proxy.MessageListener;
import net.dongliu.byproxy.store.BodyStore;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Listener to send message to ui
 *
 * @author Liu Dong
 */
public class UIMessageListener implements MessageListener {
    private static Logger logger = LoggerFactory.getLogger(UIMessageListener.class);
    private final Consumer<Message> consumer;
    private final ConcurrentMap<String, HttpMessage> httpMap;

    public UIMessageListener(Consumer<Message> consumer) {
        this.consumer = consumer;
        this.httpMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onHttpRequest(String messageId, String host, String url, RequestHeaders requestHeaders,
                              BodyStore body) {
        HttpMessage item = new HttpMessage(messageId, host, url, requestHeaders, body);
        this.httpMap.put(messageId, item);
        Platform.runLater(() -> consumer.accept(item));
    }

    @Override
    public void onHttpResponse(String messageId, ResponseHeaders responseHeaders, BodyStore body) {
        HttpMessage item = this.httpMap.get(messageId);
        if (item == null) {
            logger.error("Cannot found request item for id: {}", messageId);
            return;
        }
        item.setResponseHeaders(responseHeaders);
        item.setResponseBody(body);
    }

    @Override
    public void onWebSocket(String messageId, String host, String url, int type, boolean request, BodyStore body) {
        WebSocketMessage message = new WebSocketMessage(messageId, host, url, type, request);
        message.setBodyStore(body);
        Platform.runLater(() -> consumer.accept(message));
    }
}
