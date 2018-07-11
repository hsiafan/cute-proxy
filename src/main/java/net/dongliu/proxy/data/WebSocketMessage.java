package net.dongliu.proxy.data;

import net.dongliu.proxy.store.Body;

import java.io.Serializable;

/**
 * WebSocket message
 *
 * @author Liu Dong
 */
public class WebSocketMessage extends Message implements Serializable {
    private static final long serialVersionUID = -6889944956935896027L;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_BINARY = 2;
    // type: 1 txt
    // type: 2 binary
    private int type;
    private boolean request;
    private Body body;

    public WebSocketMessage(String host, String url, int type, boolean request, Body body) {
        super(host, url);
        this.type = type;
        this.request = request;
        this.body = body;
    }

    @Override
    public String displayText() {
        return url();
    }

    public int type() {
        return type;
    }

    public boolean isRequest() {
        return request;
    }

    public Body body() {
        return body;
    }

    @Override
    public String toString() {
        return "WebSocketMessage{url=" + url() + "}";
    }
}
