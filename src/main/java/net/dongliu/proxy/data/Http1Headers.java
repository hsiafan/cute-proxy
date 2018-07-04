package net.dongliu.proxy.data;


import java.io.Serializable;
import java.util.List;

/**
 * Commons for http1 request headers and response headers
 *
 * @author Liu Dong
 */
public abstract class Http1Headers extends HttpHeaders implements Serializable {
    private static final long serialVersionUID = 8364988912653478880L;

    public Http1Headers(List<Header> headers) {
        super(headers);
    }


    /**
     * If is chunked http body
     */
    public boolean chunked() {
        return getHeader("Transfer-Encoding").map(v -> v.equalsIgnoreCase("chunked")).orElse(false);
    }

    /**
     * The content-len set in header.
     *
     * @return -1 if not found
     */
    public long contentLen() {
        return getHeader("Content-Length").map(Long::parseLong).orElse(-1L);
    }

    /**
     * If should close connection after this msg.
     * Note: Only for http 1.1, 1.0 not supported
     */
    public boolean shouldClose() {
        return getHeader("Connection").map(v -> v.equalsIgnoreCase("close")).orElse(false);
    }


}
