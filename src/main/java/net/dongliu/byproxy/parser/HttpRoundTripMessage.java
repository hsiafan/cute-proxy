package net.dongliu.byproxy.parser;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Http request and response
 *
 * @author Liu Dong
 */
@Getter
public class HttpRoundTripMessage extends Message implements Serializable {
    private static final long serialVersionUID = -8007788167253549079L;
    private HttpMessage request;
    @Nullable
    @Setter
    private volatile HttpMessage response;

    public HttpRoundTripMessage(String id, String host, String url, HttpMessage request) {
        super(id, host, url);
        this.request = request;
    }

    @Override
    public String displayText() {
        return getUrl();
    }

}