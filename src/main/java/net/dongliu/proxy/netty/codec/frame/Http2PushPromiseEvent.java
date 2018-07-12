package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * Whole http2 headers
 */
public class Http2PushPromiseEvent extends Http2StreamEvent {
    private final int promisedStreamId;
    private final Http2Headers headers;
    private final int padding;

    public Http2PushPromiseEvent(int streamId, int promisedStreamId, Http2Headers headers, int padding) {
        super(Http2FrameTypes.PUSH_PROMISE, streamId);
        this.promisedStreamId = promisedStreamId;
        this.headers = headers;
        this.padding = padding;
    }

    public int promisedStreamId() {
        return promisedStreamId;
    }

    public Http2Headers headers() {
        return headers;
    }

    public int padding() {
        return padding;
    }

}
