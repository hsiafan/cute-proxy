package net.dongliu.proxy.netty.codec.frame;

/**
 * Abstract for common http2 frames.
 *
 * <p>
 * Note: those frame are not http2 spec frames, but event used by Http2FrameReader and Http2FrameWriter
 * </p>
 */
public abstract class Http2Event {
    // see Http2FrameTypes
    private final byte frameType;

    protected Http2Event(byte frameType) {
        this.frameType = frameType;
    }

    public byte frameType() {
        return frameType;
    }
}
