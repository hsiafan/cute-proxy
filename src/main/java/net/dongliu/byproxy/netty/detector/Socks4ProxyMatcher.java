package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.proxy.ProxyHandler;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.proxy.Socks4ProxyHandshakeHandler;
import net.dongliu.byproxy.netty.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Matcher for socks4/socks5 proxy protocol
 */
public class Socks4ProxyMatcher extends ProtocolMatcher {
    private static final Logger logger = LoggerFactory.getLogger(Socks4ProxyMatcher.class);

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;
    @Nullable
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public Socks4ProxyMatcher(@Nullable MessageListener messageListener,
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
        if (first == 4 && second == 1) {
            return MATCH;
        }
        return MISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(Socks4ServerEncoder.INSTANCE);
        pipeline.addLast(new Socks4ServerDecoder());
        pipeline.addLast(new Socks4ProxyHandshakeHandler(messageListener, sslContextManager, proxyHandlerSupplier));
    }
}
