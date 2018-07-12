package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * Whole http2 headers
 */
public class Http2HeadersEvent extends Http2StreamEvent implements IHttp2HeadersEvent {
    private final Http2Headers headers;
    private final int padding;
    private final boolean endOfStream;

    public Http2HeadersEvent(int streamId, Http2Headers headers, int padding, boolean endOfStream) {
        super(Http2FrameTypes.HEADERS, streamId);
        this.headers = headers;
        this.padding = padding;
        this.endOfStream = endOfStream;
    }

    @Override
    public Http2Headers headers() {
        return headers;
    }

    @Override
    public int padding() {
        return padding;
    }

    @Override
    public boolean endOfStream() {
        return endOfStream;
    }
}
