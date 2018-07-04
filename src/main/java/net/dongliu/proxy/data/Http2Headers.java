package net.dongliu.proxy.data;

import java.util.List;

/**
 * Http2 headers.
 */
public abstract class Http2Headers extends HttpHeaders {
    protected Http2Headers(List<Header> headers) {
        super(headers);
    }

}
