package net.dongliu.byproxy.struct;

import net.dongliu.byproxy.store.HttpBody;

import java.io.Serializable;

public class HttpMessage implements Serializable {
    private static final long serialVersionUID = 5094098542868403395L;
    private HttpHeader header;
    private HttpBody body;

    public HttpMessage(HttpHeader header, HttpBody body) {
        this.header = header;
        this.body = body;
    }

    public HttpHeader getHeader() {
        return header;
    }

    public HttpBody getBody() {
        return body;
    }
}
