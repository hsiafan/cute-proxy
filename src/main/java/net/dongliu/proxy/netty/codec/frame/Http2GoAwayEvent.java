package net.dongliu.proxy.netty.codec.frame;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * Whole http2 headers
 */
public class Http2GoAwayEvent extends Http2Event {
    private final int lastStreamId;
    private final long errorCode;
    private final ByteBuf debugData;

    public Http2GoAwayEvent(int lastStreamId, long errorCode, ByteBuf debugData) {
        super(Http2FrameTypes.GO_AWAY);
        this.lastStreamId = lastStreamId;
        this.errorCode = errorCode;
        this.debugData = debugData;
    }

    public int lastStreamId() {
        return lastStreamId;
    }

    public long errorCode() {
        return errorCode;
    }

    public ByteBuf debugData() {
        return debugData;
    }

}
