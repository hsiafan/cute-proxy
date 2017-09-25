package net.dongliu.byproxy.struct;

import net.dongliu.byproxy.store.Body;

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
    private RequestHeaders requestHeader;
    private Body requestBody;
    @Nullable
    private volatile ResponseHeaders responseHeader;
    @Nullable
    private volatile Body responseBody;

    public HttpRoundTripMessage(String host, String url, RequestHeaders requestHeader, Body requestBody) {
        super(host, url);
        this.requestHeader = Objects.requireNonNull(requestHeader);
        this.requestBody = Objects.requireNonNull(requestBody);
    }

    @Override
    public String displayText() {
        return getUrl();
    }

    public RequestHeaders getRequestHeader() {
        return requestHeader;
    }

    public Body getRequestBody() {
        return requestBody;
    }

    @Nullable
    public ResponseHeaders getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(ResponseHeaders responseHeader) {
        this.responseHeader = Objects.requireNonNull(responseHeader);
    }

    @Nullable
    public Body getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(@Nullable Body responseBody) {
        this.responseBody = Objects.requireNonNull(responseBody);
    }
}