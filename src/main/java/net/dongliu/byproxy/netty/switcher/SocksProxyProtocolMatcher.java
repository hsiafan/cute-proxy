package net.dongliu.byproxy.netty.switcher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import net.dongliu.byproxy.netty.SocksProxyAuthHandler;

/**
 * Matcher for socks4/socks5 proxy protocol
 */
public class SocksProxyProtocolMatcher implements ProtocolMatcher {
    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return PENDING;
        }
        byte first = buf.getByte(0);
        byte second = buf.getByte(1);
        if (first == 4 || first == 5) {
            return MATCH;
        }
        return DISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new SocksPortUnificationServerHandler());
        pipeline.addLast(new SocksProxyAuthHandler());
    }
}
