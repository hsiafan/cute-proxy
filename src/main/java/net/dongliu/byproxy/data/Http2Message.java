package net.dongliu.byproxy.data;

import net.dongliu.byproxy.store.Body;
import net.dongliu.byproxy.utils.NetAddress;

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

    public Http2Message(NetAddress address, Http2RequestHeaders requestHeader, Body body) {
        super(requestHeader.getHeader("Host").orElse(address.getHost()), getUrl(address, requestHeader));
        this.requestHeader = requireNonNull(requestHeader);
        this.requestBody = body;
    }

    private static String getUrl(NetAddress address, Http2RequestHeaders requestHeader) {
        String scheme = requestHeader.getScheme();
        int port = address.getPort();
        StringBuilder sb = new StringBuilder(scheme).append("://");
        sb.append(requestHeader.getHeader("Host").orElse(address.getHost()));
        if (scheme.equals("https") && port != 443 || scheme.equals("http") && port != 80) {
            sb.append(":").append(port);
        }
        sb.append(requestHeader.getPath());
        return sb.toString();
    }

    @Override
    public String displayText() {
        return getUrl();
    }

    public Http2RequestHeaders getRequestHeader() {
        return requestHeader;
    }

    public Body getRequestBody() {
        return requestBody;
    }

    public Http2ResponseHeaders getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(Http2ResponseHeaders responseHeader) {
        this.responseHeader = requireNonNull(responseHeader);
    }

    public Body getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(Body responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public String toString() {
        return "Http2Message{url=" + getUrl() + "}";
    }
}