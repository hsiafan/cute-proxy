package net.dongliu.byproxy.ui;

import javafx.application.Platform;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;
import net.dongliu.byproxy.struct.Message;
import net.dongliu.byproxy.struct.WebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Listener to send message to ui
 *
 * @author Liu Dong
 */
public class UIMessageListener implements MessageListener {
    private static Logger logger = LoggerFactory.getLogger(UIMessageListener.class);
    private final Consumer<Message> consumer;

    public UIMessageListener(Consumer<Message> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onHttpRequest(HttpRoundTripMessage message) {
        Platform.runLater(() -> consumer.accept(message));
    }

    @Override
    public void onWebSocket(WebSocketMessage message) {
        Platform.runLater(() -> consumer.accept(message));
    }
}
