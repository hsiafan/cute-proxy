package net.dongliu.byproxy.parser;

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
}