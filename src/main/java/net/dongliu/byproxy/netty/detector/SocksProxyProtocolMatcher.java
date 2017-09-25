package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.tcp.Socks4ProxyAuthHandler;
import net.dongliu.byproxy.netty.tcp.Socks5ProxyAuthHandler;
import net.dongliu.byproxy.ssl.SSLContextManager;

import javax.annotation.Nullable;

/**
 * Matcher for socks4/socks5 proxy protocol
 */
public class SocksProxyProtocolMatcher extends ProtocolMatcher {

    private int socksVersion;

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;

    public SocksProxyProtocolMatcher(@Nullable MessageListener messageListener,
                                     @Nullable SSLContextManager sslContextManager) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        if (first == 4 || first == 5) {
            socksVersion = first;
            return MATCH;
        }
        return DISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        if (socksVersion == 4) {
            pipeline.addLast(Socks4ServerEncoder.INSTANCE);
            pipeline.addLast(new Socks4ServerDecoder());
            pipeline.addLast(new Socks4ProxyAuthHandler(messageListener, sslContextManager));
            return;
        }
        if (socksVersion == 5) {
            pipeline.addLast(Socks5ServerEncoder.DEFAULT);
            pipeline.addLast(new Socks5InitialRequestDecoder());
            pipeline.addLast(new Socks5ProxyAuthHandler(messageListener, sslContextManager));
            return;
        }
        throw new RuntimeException("should not happen");
    }
}
