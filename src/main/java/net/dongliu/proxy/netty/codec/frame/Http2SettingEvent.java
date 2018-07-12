package net.dongliu.proxy.netty.codec.frame;

import io.netty.handler.codec.http2.Http2FrameTypes;
import io.netty.handler.codec.http2.Http2Settings;

/**
 * Whole http2 headers
 */
public class Http2SettingEvent extends Http2Event {
    private final Http2Settings http2Settings;

    public Http2SettingEvent(Http2Settings http2Settings) {
        super(Http2FrameTypes.SETTINGS);
        this.http2Settings = http2Settings;
    }

    public Http2Settings http2Settings() {
        return http2Settings;
    }
}
