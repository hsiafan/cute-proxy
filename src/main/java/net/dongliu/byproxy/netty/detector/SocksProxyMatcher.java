package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.proxy.Socks4ProxyAuthHandler;
import net.dongliu.byproxy.netty.proxy.Socks5ProxyAuthHandler;
import net.dongliu.byproxy.ssl.SSLContextManager;

import javax.annotation.Nullable;

/**
 * Matcher for socks4/socks5 proxy protocol
 */
public class SocksProxyMatcher extends ProtocolMatcher {

    private int socksVersion;

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;

    public SocksProxyMatcher(@Nullable MessageListener messageListener,
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
            pipeline.addLast("socks4-server-encoder", Socks4ServerEncoder.INSTANCE);
            pipeline.addLast("socks4-server-decoder", new Socks4ServerDecoder());
            pipeline.addLast("socks4-proxy-auth-handler", new Socks4ProxyAuthHandler(messageListener, sslContextManager));
            return;
        }
        if (socksVersion == 5) {
            pipeline.addLast("socks5-server-encoder", Socks5ServerEncoder.DEFAULT);
            pipeline.addLast("socks5-request-decoder", new Socks5InitialRequestDecoder());
            pipeline.addLast("socks5-proxy-auth-handler", new Socks5ProxyAuthHandler(messageListener, sslContextManager));
            return;
        }
        throw new RuntimeException("should not happen");
    }
}
