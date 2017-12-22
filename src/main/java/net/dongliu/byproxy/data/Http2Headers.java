package net.dongliu.byproxy.data;

import java.util.List;

public abstract class Http2Headers extends HttpHeaders {
    protected Http2Headers(List<Header> headers) {
        super(headers);
    }

}
