package net.dongliu.proxy.data;

import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.store.Body;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * Http request and response
 *
 * @author Liu Dong
 */
public class Http1Message extends HttpMessage implements Serializable {
    private static final long serialVersionUID = -8007788167253549079L;
    private final Http1RequestHeaders requestHeader;
    private final Body requestBody;
    private volatile Http1ResponseHeaders responseHeader;
    private volatile Body responseBody;

    public Http1Message(String scheme, HostPort address, Http1RequestHeaders requestHeader, Body requestBody) {
        super(requestHeader.getHeader("Host").orElse(address.host()), getUrl(scheme, address, requestHeader));
        this.requestHeader = requireNonNull(requestHeader);
        this.requestBody = requireNonNull(requestBody);
    }

    private static String getUrl(String scheme, HostPort address, Http1RequestHeaders requestHeader) {
        String host = requestHeader.getHeader("Host").orElse(address.host());
        StringBuilder sb = new StringBuilder(scheme).append("://").append(host);
        if (!host.contains(":")) {
            if (!(scheme.equals("https") && address.ensurePort() == 443
                    || scheme.equals("http") && address.ensurePort() == 80)) {
                sb.append(":").append(address.port());
            }
        }
        sb.append(requestHeader.requestLine().path());
        return sb.toString();
    }

    @Override
    public String displayText() {
        return url();
    }

    public Http1RequestHeaders requestHeader() {
        return requestHeader;
    }

    public Body requestBody() {
        return requestBody;
    }

    public Http1ResponseHeaders responseHeader() {
        return responseHeader;
    }

    public void responseHeader(Http1ResponseHeaders responseHeader) {
        this.responseHeader = requireNonNull(responseHeader);
    }

    public Body responseBody() {
        return responseBody;
    }

    public void responseBody(Body responseBody) {
        this.responseBody = requireNonNull(responseBody);
    }

    @Override
    public String toString() {
        return "Http1Message{url=" + url() + "}";
    }
}