package net.dongliu.byproxy.ui;

import javafx.application.Platform;
import net.dongliu.byproxy.parser.HttpMessage;
import net.dongliu.byproxy.parser.HttpRoundTripMessage;
import net.dongliu.byproxy.parser.Message;
import net.dongliu.byproxy.parser.WebSocketMessage;
import net.dongliu.byproxy.proxy.MessageListener;
import net.dongliu.byproxy.store.HttpBody;
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
    private final ConcurrentMap<String, HttpRoundTripMessage> httpMap;

    public UIMessageListener(Consumer<Message> consumer) {
        this.consumer = consumer;
        this.httpMap = new ConcurrentHashMap<>();
    }

    @Override
    public void onHttpRequest(String messageId, String host, String url, HttpMessage message) {
        HttpRoundTripMessage item = new HttpRoundTripMessage(messageId, host, url, message);
        this.httpMap.put(messageId, item);
        Platform.runLater(() -> consumer.accept(item));
    }

    @Override
    public void onHttpResponse(String messageId, HttpMessage message) {
        HttpRoundTripMessage item = this.httpMap.get(messageId);
        if (item == null) {
            logger.error("Cannot found request item for id: {}", messageId);
            return;
        }
        httpMap.remove(messageId);
        item.setResponse(message);
    }

    @Override
    public void onWebSocket(String messageId, String host, String url, int type, boolean request, HttpBody body) {
        WebSocketMessage message = new WebSocketMessage(messageId, host, url, type, request);
        message.setHttpBody(body);
        Platform.runLater(() -> consumer.accept(message));
    }
}
