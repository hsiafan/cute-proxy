package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;

/**
 * Whole http2 headers
 */
public class Http2SettingAckEvent extends Http2Event {

    public Http2SettingAckEvent() {
        super(Http2FrameTypes.SETTINGS);
    }
}
