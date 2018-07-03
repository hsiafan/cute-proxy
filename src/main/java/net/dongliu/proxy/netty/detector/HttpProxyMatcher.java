package net.dongliu.proxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.proxy.ProxyHandler;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.handler.HttpProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for plain http proxy request.
 */
public class HttpProxyMatcher extends ProtocolMatcher {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyMatcher.class);

    private static Set<String> methods = Set.of("GET", "POST", "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE",
            "TRACE");

    private final MessageListener messageListener;
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public HttpProxyMatcher(MessageListener messageListener, Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 8) {
            return PENDING;
        }

        int index = buf.indexOf(0, 8, (byte) ' ');
        if (index < 0) {
            return MISMATCH;
        }

        int firstURIIndex = index + 1;
        if (buf.readableBytes() < firstURIIndex + 1) {
            return PENDING;
        }

        String method = buf.toString(0, index, US_ASCII);
        char firstURI = (char) (buf.getByte(firstURIIndex + buf.readerIndex()) & 0xff);
        if (!methods.contains(method) || firstURI == '/') {
            return MISMATCH;
        }


        return MATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast("", new HttpServerExpectContinueHandler());
        pipeline.addLast(new HttpProxyHandler(messageListener, proxyHandlerSupplier));
    }
}
