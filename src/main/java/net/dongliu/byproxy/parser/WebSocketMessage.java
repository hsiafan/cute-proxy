package net.dongliu.byproxy.parser;

import net.dongliu.byproxy.store.BodyStore;

import java.io.Serializable;

/**
 * WebSocket message
 *
 * @author Liu Dong
 */
public class WebSocketMessage extends Message implements Serializable {
    private static final long serialVersionUID = -6889944956935896027L;
    // type: 1 txt
    // type: 2 binary
    private int type;
    private boolean request;
    private volatile BodyStore bodyStore;

    public WebSocketMessage(String id, String host, String url, int type, boolean request) {
        super(id, host, url);
        this.type = type;
        this.request = request;
    }

    @Override
    public String getDisplay() {
        return getUrl();
    }

    public int getType() {
        return type;
    }

    public boolean isRequest() {
        return request;
    }

    public BodyStore getBodyStore() {
        return bodyStore;
    }

    public void setBodyStore(BodyStore bodyStore) {
        this.bodyStore = bodyStore;
    }
}
