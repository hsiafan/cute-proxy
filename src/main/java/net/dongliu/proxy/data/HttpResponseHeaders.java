package net.dongliu.proxy.data;

import java.util.List;

/**
 * Common interface for http1.x/http2 request headers
 */
public interface HttpResponseHeaders {

    /**
     * Http status code
     */
    int statusCode();

    /**
     * all commons headers(exclude request line)
     */
    List<Header> headers();
}
