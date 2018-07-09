package net.dongliu.proxy.data;

import java.util.List;

/**
 * Common interface for http1.x/http2 request headers
 */
public interface HttpRequestHeaders {

    /**
     * request method
     */
    String method();

    /**
     * Request path
     */
    String path();

    /**
     * Http version
     */
    String version();

    /**
     * all commons headers(exclude request line)
     */
    List<Header> headers();
}
