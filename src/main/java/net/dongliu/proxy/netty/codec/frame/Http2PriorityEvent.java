package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * Whole http2 headers
 */
public class Http2PriorityEvent extends Http2StreamEvent {

    private final int streamDependency;
    private final short weight;
    private final boolean exclusive;

    public Http2PriorityEvent(int streamId, int streamDependency, short weight, boolean exclusive) {
        super(Http2FrameTypes.PRIORITY, streamId);
        this.streamDependency = streamDependency;
        this.weight = weight;
        this.exclusive = exclusive;
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
}
