package net.dongliu.proxy.netty.codec.frame;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Flags;

public class Http2UnknownEvent extends Http2StreamEvent {
    private final Http2Flags flags;
    private final ByteBuf payload;

    public Http2UnknownEvent(byte frameType, int streamId, Http2Flags flags, ByteBuf payload) {
        super(frameType, streamId);
        this.flags = flags;
        this.payload = payload;
    }

    public Http2Flags flags() {
        return flags;
    }

    public ByteBuf payload() {
        return payload;
    }
}
