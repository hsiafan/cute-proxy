package net.dongliu.proxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.proxy.ProxyHandler;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.handler.ServerSSLContextManager;
import net.dongliu.proxy.netty.handler.HttpConnectProxyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for http proxy connect tunnel.
 */
public class HttpConnectProxyMatcher extends ProtocolMatcher {
    private static final Logger logger = LoggerFactory.getLogger(HttpConnectProxyMatcher.class);

    private final MessageListener messageListener;
    private final ServerSSLContextManager sslContextManager;
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public HttpConnectProxyMatcher(MessageListener messageListener,
                                   ServerSSLContextManager sslContextManager,
                                   Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 8) {
            return PENDING;
        }

        String method = buf.toString(0, 8, US_ASCII);
        if (!"CONNECT ".equalsIgnoreCase(method)) {
            return MISMATCH;
        }

        return MATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpConnectProxyInitializer(messageListener, sslContextManager, proxyHandlerSupplier));
    }
}
