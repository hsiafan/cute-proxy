package net.dongliu.byproxy.parser;

import net.dongliu.byproxy.store.BodyStore;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * @author Liu Dong
 */
public class HttpMessage extends Message implements Serializable {
    private static final long serialVersionUID = -8007788167253549079L;
    private RequestHeaders requestHeaders;
    private BodyStore requestBody;
    @Nullable
    private volatile ResponseHeaders responseHeaders;
    private volatile BodyStore responseBody;

    public HttpMessage(String id, String host, String url, RequestHeaders requestHeaders, BodyStore requestBody) {
        super(id, host, url);
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
    }

    @Override
    public String getDisplay() {
        return getUrl();
    }

    public RequestHeaders getRequestHeaders() {
        return requestHeaders;
    }

    public BodyStore getRequestBody() {
        return requestBody;
    }

    @Nullable
    public ResponseHeaders getResponseHeaders() {
        return responseHeaders;
    }

    public BodyStore getResponseBody() {
        return responseBody;
    }

    public void setResponseHeaders(ResponseHeaders responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setResponseBody(BodyStore responseBody) {
        this.responseBody = responseBody;
    }
}
