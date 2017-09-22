package net.dongliu.byproxy.struct;

import net.dongliu.byproxy.store.HttpBody;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Http request and response
 *
 * @author Liu Dong
 */
public class HttpRoundTripMessage extends Message implements Serializable {
    private static final long serialVersionUID = -8007788167253549079L;
    private HttpRequestHeader requestHeader;
    private HttpBody requestBody;
    @Nullable
    private volatile HttpResponseHeader responseHeader;
    @Nullable
    private volatile HttpBody responseBody;

    public HttpRoundTripMessage(String host, String url, HttpRequestHeader requestHeader, HttpBody requestBody) {
        super(host, url);
        this.requestHeader = Objects.requireNonNull(requestHeader);
        this.requestBody = Objects.requireNonNull(requestBody);
    }

    @Override
    public String displayText() {
        return getUrl();
    }

    public HttpRequestHeader getRequestHeader() {
        return requestHeader;
    }

    public HttpBody getRequestBody() {
        return requestBody;
    }

    @Nullable
    public HttpResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(HttpResponseHeader responseHeader) {
        this.responseHeader = Objects.requireNonNull(responseHeader);
    }

    @Nullable
    public HttpBody getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(@Nullable HttpBody responseBody) {
        this.responseBody = Objects.requireNonNull(responseBody);
    }
}