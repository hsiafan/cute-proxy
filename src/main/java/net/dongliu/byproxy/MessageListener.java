package net.dongliu.byproxy;

import net.dongliu.byproxy.struct.*;

/**
 * Listener to receive request data. The operation in the call back method must not block
 *
 * @author Liu Dong
 */
public interface MessageListener {
    /**
     * Http request received
     */
    void onHttpRequest(HttpRoundTripMessage message);

    /**
     * One receive a websocket message
     */
    void onWebSocket(WebSocketMessage message);
}
