package net.dongliu.proxy.netty.codec.frame;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * data steam
 */
public class Http2DataEvent extends Http2StreamEvent {
    private final ByteBuf data;
    private final int padding;
    private final boolean endOfStream;

    public Http2DataEvent(int streamId, ByteBuf data, int padding, boolean endOfStream) {
        super(Http2FrameTypes.DATA, streamId);
        this.data = data;
        this.padding = padding;
        this.endOfStream = endOfStream;
    }

    public ByteBuf data() {
        return data;
    }

    public int padding() {
        return padding;
    }

    public boolean endOfStream() {
        return endOfStream;
    }

}
