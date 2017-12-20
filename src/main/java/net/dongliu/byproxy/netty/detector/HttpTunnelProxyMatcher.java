package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.proxy.ProxyHandler;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.proxy.HttpTunnelProxyHandler;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for http proxy connect tunnel.
 */
public class HttpTunnelProxyMatcher extends ProtocolMatcher {
    private static final Logger logger = LoggerFactory.getLogger(HttpTunnelProxyMatcher.class);

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;
    @Nullable
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public HttpTunnelProxyMatcher(@Nullable MessageListener messageListener,
                                  @Nullable SSLContextManager sslContextManager,
                                  @Nullable Supplier<ProxyHandler> proxyHandlerSupplier) {
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
        pipeline.addLast(new HttpTunnelProxyHandler(messageListener, sslContextManager, proxyHandlerSupplier));
    }
}
