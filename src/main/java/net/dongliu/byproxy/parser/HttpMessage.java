package net.dongliu.byproxy.parser;

import net.dongliu.byproxy.store.HttpBody;

import java.io.Serializable;

public class HttpMessage implements Serializable {
    private static final long serialVersionUID = 5094098542868403395L;
    private Headers headers;
    private HttpBody body;

    public HttpMessage(Headers headers, HttpBody body) {
        this.headers = headers;
        this.body = body;
    }

    public Headers getHeaders() {
        return headers;
    }

    public HttpBody getBody() {
        return body;
    }
}
