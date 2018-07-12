package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * Whole http2 headers
 */
public class Http2PriorityHeadersEvent extends Http2StreamEvent implements IHttp2HeadersEvent {
    private final Http2Headers headers;
    private final int padding;
    private final boolean endOfStream;

    private final int streamDependency;
    private final short weight;
    private final boolean exclusive;

    public Http2PriorityHeadersEvent(int streamId, Http2Headers headers, int padding, boolean endOfStream,
                                     int streamDependency, short weight, boolean exclusive) {
        super(Http2FrameTypes.HEADERS, streamId);
        this.headers = headers;
        this.padding = padding;
        this.endOfStream = endOfStream;

        this.streamDependency = streamDependency;
        this.weight = weight;
        this.exclusive = exclusive;
    }

    public Http2Headers headers() {
        return headers;
    }

    public int padding() {
        return padding;
    }

    public int streamDependency() {
        return streamDependency;
    }

    public short weight() {
        return weight;
    }

    public boolean exclusive() {
        return exclusive;
    }

    public boolean endOfStream() {
        return endOfStream;
    }
}
