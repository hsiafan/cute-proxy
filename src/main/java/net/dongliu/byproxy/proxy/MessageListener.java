package net.dongliu.byproxy.proxy;

import net.dongliu.byproxy.parser.HttpMessage;
import net.dongliu.byproxy.store.HttpBody;

/**
 * Listener to receive request data
 *
 * @author Liu Dong
 */
public interface MessageListener {
    /**
     * Http request received
     */
    void onHttpRequest(String messageId, String host, String url, HttpMessage requestMessage);

    /**
     * On response received
     */
    void onHttpResponse(String messageId, HttpMessage message);

    /**
     * One receive a websocket message
     */
    void onWebSocket(String messageId, String host, String url, int type, boolean request, HttpBody httpBody);
}
