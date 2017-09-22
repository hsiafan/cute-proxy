package net.dongliu.byproxy;

import net.dongliu.byproxy.struct.*;
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
    void onHttpRequest(HttpRoundTripMessage message);

    /**
     * One receive a websocket message
     */
    void onWebSocket(WebSocketMessage message);
}
