package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * Whole http2 headers
 */
public class Http2PingEvent extends Http2Event {
    private final long data;

    public Http2PingEvent(long data) {
        super(Http2FrameTypes.PING);
        this.data = data;
    }

    public long data() {
        return data;
    }
}
