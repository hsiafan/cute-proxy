package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * Whole http2 headers
 */
public class Http2WindowUpdateEvent extends Http2StreamEvent {
    private final int windowSizeIncrement;

    public Http2WindowUpdateEvent(int streamId, int windowSizeIncrement) {
        super(Http2FrameTypes.WINDOW_UPDATE, streamId);
        this.windowSizeIncrement = windowSizeIncrement;
    }

    public int windowSizeIncrement() {
        return windowSizeIncrement;
    }
}
