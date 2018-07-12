package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * Whole http2 headers
 */
public class Http2RstEvent extends Http2StreamEvent {

    private final long errorCode;

    public Http2RstEvent(int streamId, long errorCode) {
        super(Http2FrameTypes.RST_STREAM, streamId);
        this.errorCode = errorCode;
    }

    public long errorCode() {
        return errorCode;
    }
}
