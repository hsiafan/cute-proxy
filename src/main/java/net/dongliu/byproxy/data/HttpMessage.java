package net.dongliu.byproxy.data;

import net.dongliu.byproxy.store.Body;

public abstract class HttpMessage extends Message {
    protected HttpMessage(String host, String url) {
        super(host, url);
    }

    public abstract HttpHeaders getRequestHeader();

    public abstract Body getRequestBody();

    public abstract HttpHeaders getResponseHeader();

    public abstract Body getResponseBody();

}
