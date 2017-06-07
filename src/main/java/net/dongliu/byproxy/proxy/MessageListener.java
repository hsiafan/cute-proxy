package net.dongliu.byproxy.proxy;

import net.dongliu.byproxy.parser.RequestHeaders;
import net.dongliu.byproxy.parser.ResponseHeaders;
import net.dongliu.byproxy.store.BodyStore;

/**
 * Listener to receive request data
 *
 * @author Liu Dong
 */
public interface MessageListener {
    /**
     * Http request received
     */
    void onHttpRequest(String messageId, String host, String url, RequestHeaders requestHeaders,
                       BodyStore body);

    /**
     * On response received
     */
    void onHttpResponse(String messageId, ResponseHeaders responseHeaders, BodyStore body);

    /**
     * One receive a websocket message
     */
    void onWebSocket(String messageId, String host, String url, int type, boolean request, BodyStore bodyStore);
}
