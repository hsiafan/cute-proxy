package net.dongliu.proxy.data;

import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.store.Body;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * Http2 request and response
 *
 * @author Liu Dong
 */
public class Http2Message extends HttpMessage implements Serializable {
    private static final long serialVersionUID = 2578040550199755205L;
    private final Http2RequestHeaders requestHeader;
    private final Body requestBody;
    private volatile Http2ResponseHeaders responseHeader;
    private volatile Body responseBody;

    public Http2Message(HostPort address, Http2RequestHeaders requestHeader, Body body) {
        super(requestHeader.getHeader("Host").orElse(address.host()), getUrl(address, requestHeader));
        this.requestHeader = requireNonNull(requestHeader);
        this.requestBody = body;
    }

    private static String getUrl(HostPort address, Http2RequestHeaders requestHeader) {
        String scheme = requestHeader.scheme();
        int port = address.ensurePort();
        StringBuilder sb = new StringBuilder(scheme).append("://");
        sb.append(requestHeader.getHeader("Host").orElse(address.host()));
        if (scheme.equals("https") && port != 443 || scheme.equals("http") && port != 80) {
            sb.append(":").append(port);
        }
        sb.append(requestHeader.path());
        return sb.toString();
    }

    @Override
    public String displayText() {
        return url();
    }

    public Http2RequestHeaders requestHeader() {
        return requestHeader;
    }

    public Body requestBody() {
        return requestBody;
    }

    public Http2ResponseHeaders responseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(Http2ResponseHeaders responseHeader) {
        this.responseHeader = requireNonNull(responseHeader);
    }

    public Body responseBody() {
        return responseBody;
    }

    public void setResponseBody(Body responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public String toString() {
        return "Http2Message{url=" + url() + "}";
    }
}