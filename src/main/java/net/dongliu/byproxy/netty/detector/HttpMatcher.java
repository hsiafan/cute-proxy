package net.dongliu.byproxy.netty.detector;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.proxy.HttpProxyConnectHandler;
import net.dongliu.byproxy.netty.proxy.HttpProxyHandler;
import net.dongliu.byproxy.netty.web.HttpRequestHandler;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for plain http protocol
 */
public class HttpMatcher extends ProtocolMatcher {
    private static final Logger logger = LoggerFactory.getLogger(HttpMatcher.class);

    private static Set<String> methods = ImmutableSet.of("GET", "POST", "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE",
            "TRACE", "CONNECT");

    private static final int CONNECT = 1;
    private static final int HTTP = 2;
    private static final int HTTP_PROXY = 3;

    private int type;

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;

    public HttpMatcher(@Nullable MessageListener messageListener,
                       @Nullable SSLContextManager sslContextManager) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 5) {
            return PENDING;
        }

        int index = buf.indexOf(0, 9, (byte) ' ');
        if (index < 0) {
            if (buf.readableBytes() < 8) {
                return PENDING;
            }
            return DISMATCH;
        }

        int firstURIIndex = index + 1;
        if (buf.readableBytes() < firstURIIndex + 1) {
            return PENDING;
        }

        String method = buf.toString(0, index, US_ASCII);
        char firstURI = (char) (buf.getByte(firstURIIndex + buf.readerIndex()) & 0xff);
        if (!methods.contains(method)) {
            return DISMATCH;
        }

        if (method.equals("CONNECT")) {
            logger.debug("http connect request matched");
            type = CONNECT;
        } else if (firstURI == '/') {
            logger.debug("http plain request matched");
            type = HTTP;
        } else {
            logger.debug("http proxy request matched");
            type = HTTP_PROXY;
        }

        return MATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        switch (type) {
            case HTTP:
                pipeline.addLast("http-server-codec", new HttpServerCodec());
                pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                pipeline.addLast("chunk-write-handler", new ChunkedWriteHandler());
                pipeline.addLast("http-compressor", new HttpContentCompressor());
                pipeline.addLast("http-request-handler", new HttpRequestHandler(sslContextManager));
                break;
            case CONNECT:
                pipeline.addLast("http-server-codec", new HttpServerCodec());
                pipeline.addLast("http-connector-proxy-handler", new HttpProxyConnectHandler(messageListener, sslContextManager));
                break;
            case HTTP_PROXY:
                pipeline.addLast("http-server-codec", new HttpServerCodec());
//                pipeline.addLast("", new HttpServerExpectContinueHandler());
                pipeline.addLast("http-proxy-handler", new HttpProxyHandler(messageListener));
                break;
            default:
                throw new RuntimeException("should not happen");
        }
    }
}
