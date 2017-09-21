package net.dongliu.byproxy.struct;

import net.dongliu.byproxy.store.HttpBody;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Http request and response
 *
 * @author Liu Dong
 */
public class HttpRoundTripMessage extends Message implements Serializable {
    private static final long serialVersionUID = -8007788167253549079L;
    private HttpMessage request;
    @Nullable
    private volatile HttpMessage response;

    public HttpRoundTripMessage(String id, String host, String url, HttpMessage request) {
        super(id, host, url);
        this.request = request;
    }

    @Override
    public String displayText() {
        return getUrl();
    }

    public HttpMessage getRequest() {
        return request;
    }

    @Nullable
    public HttpMessage getResponse() {
        return response;
    }

    public void setResponse(HttpMessage response) {
        this.response = response;
    }

    public HttpHeader getRequestHeader() {
        return request.getHeader();
    }

    @Nullable
    public HttpHeader getResponseHeader() {
        HttpMessage response = this.response;
        if (response == null) {
            return null;
        }
        return response.getHeader();
    }

    public HttpBody getRequestBody() {
        return request.getBody();
    }

    @Nullable
    public HttpBody getResponseBody() {
        HttpMessage response = this.response;
        if (response == null) {
            return null;
        }
        return response.getBody();
    }
}