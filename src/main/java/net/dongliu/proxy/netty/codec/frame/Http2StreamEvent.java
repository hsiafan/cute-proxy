package net.dongliu.proxy.netty.codec.frame;

/**
 * Abstract for common http2 frames.
 */
public abstract class Http2StreamEvent extends Http2Event {

    private final int streamId;

    public Http2StreamEvent(byte frameType, int streamId) {
        super(frameType);
        this.streamId = streamId;
    }

    public int streamId() {
        return streamId;
    }
}
