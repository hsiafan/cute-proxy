package net.dongliu.proxy.netty.detector;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Simple and not accurate WebSocket Protocol detector, for both request and response
 */
public class WebSocketMatcher extends ProtocolMatcher {

    private static final ByteBuf HEAD_END = Unpooled.wrappedBuffer("\r\n\r\n".getBytes(US_ASCII));
    private static final ByteBuf WEB_SOCKET_UPGRADE = Unpooled.wrappedBuffer("\r\nUpgrade: websocket\r\n".getBytes(US_ASCII));

    @Override
    protected int match(ByteBuf buf) {
        if (ByteBufUtil.indexOf(HEAD_END, buf) == -1) {
            if (buf.readableBytes() > 8096) {
                return MISMATCH;
            }
            return PENDING;
        }

        if (ByteBufUtil.indexOf(WEB_SOCKET_UPGRADE, buf) == -1) {
            return MISMATCH;
        }
        return MATCH;
    }
}
