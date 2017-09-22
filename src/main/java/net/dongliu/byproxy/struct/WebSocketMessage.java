package net.dongliu.byproxy.struct;

import net.dongliu.byproxy.store.HttpBody;

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
    private volatile HttpBody httpBody;

    public WebSocketMessage(String host, String url, int type, boolean request) {
        super(host, url);
        this.type = type;
        this.request = request;
    }

    @Override
    public String displayText() {
        return getUrl();
    }

    public int getType() {
        return type;
    }

    public boolean isRequest() {
        return request;
    }

    public HttpBody getHttpBody() {
        return httpBody;
    }

    public void setHttpBody(HttpBody httpBody) {
        this.httpBody = httpBody;
    }
}
