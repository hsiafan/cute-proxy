package net.dongliu.byproxy.data;

import net.dongliu.byproxy.store.Body;

import javax.annotation.Nullable;

public abstract class HttpMessage extends Message {
    protected HttpMessage(String host, String url) {
        super(host, url);
    }

    public abstract HttpHeaders getRequestHeader();

    public abstract Body getRequestBody();

    @Nullable
    public abstract HttpHeaders getResponseHeader();

    @Nullable
    public abstract Body getResponseBody();

}
