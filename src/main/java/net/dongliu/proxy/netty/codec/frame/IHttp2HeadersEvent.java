package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2Headers;

public interface IHttp2HeadersEvent {
    Http2Headers headers();

    int padding();

    boolean endOfStream();
}
