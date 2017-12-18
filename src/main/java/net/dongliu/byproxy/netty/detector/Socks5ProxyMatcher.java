package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.proxy.ProxyHandler;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.proxy.Socks5ProxyHandshakeHandler;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Matcher for socks5 proxy protocol
 */
public class Socks5ProxyMatcher extends ProtocolMatcher {
    private static final Logger logger = LoggerFactory.getLogger(Socks5ProxyMatcher.class);

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;
    @Nullable
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public Socks5ProxyMatcher(@Nullable MessageListener messageListener,
                              @Nullable SSLContextManager sslContextManager,
                              Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        if (first == 5) {
            return MATCH;
        }
        return MISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(Socks5ServerEncoder.DEFAULT);
        pipeline.addLast(new Socks5InitialRequestDecoder());
        pipeline.addLast(new Socks5ProxyHandshakeHandler(messageListener, sslContextManager, proxyHandlerSupplier));
    }
}
