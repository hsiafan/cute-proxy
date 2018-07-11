package net.dongliu.proxy.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import net.dongliu.commons.Strings;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http2.Http2CodecUtil.HTTP_UPGRADE_STREAM_ID;

/**
 * Handle http upgrade(websocket, http2).
 * Note: http2 client may send preface frame directly, with no upgrade request.
 */
public class HttpUpgradeHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpInterceptor.class);

    // the current http request/response
    private boolean upgradeWebSocket;
    private String wsUrl;
    private boolean upgradeH2c;

    private final boolean ssl;
    private final NetAddress address;
    private final MessageListener messageListener;
    private final ChannelPipeline localPipeline;

    public HttpUpgradeHandler(boolean ssl, NetAddress address, MessageListener messageListener,
                              ChannelPipeline localPipeline) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = messageListener;
        this.localPipeline = localPipeline;
    }

    // read request
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof HttpRequest)) {
            logger.debug("not http message: {}", msg.getClass().getName());
            ctx.write(msg, promise);
            return;
        }

        HttpRequest request = (HttpRequest) msg;
        HttpHeaders headers = request.headers();
        var items = getHeaderValues(Strings.nullToEmpty(headers.get("Connection")));
        if (items.contains("upgrade")) {
            String upgrade = Strings.nullToEmpty(headers.get("Upgrade")).trim().toLowerCase();
            switch (upgrade) {
                case "websocket":
                    upgradeWebSocket = true;
                    wsUrl = getUrl(ssl, address, request);
                    break;
                case "h2c":
                    upgradeH2c = true;
                    break;
                default:
                    logger.warn("unsupported upgrade header value: {}", upgrade);
            }
        }
        if (!upgradeH2c && !upgradeWebSocket) {
            ctx.pipeline().remove(this);
        }
        ctx.write(msg, promise);
    }

    private static String getUrl(boolean ssl, NetAddress address, HttpRequest request) {
        HttpHeaders headers = request.headers();
        String host = Objects.requireNonNullElse(headers.get("Host"), address.host());
        StringBuilder sb = new StringBuilder(ssl ? "wss" : "ws").append("://").append(host);
        if (!host.contains(":")) {
            if (!(ssl && address.port() == 443 || !ssl && address.port() == 80)) {
                sb.append(":").append(address.port());
            }
        }
        sb.append(request.uri());
        return sb.toString();
    }

    // read response
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Http2Exception {
        if (!(msg instanceof HttpResponse)) {
            logger.error("not http message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
            return;
        }

        // either upgradeWebSocket or upgradeH2c should be true
        HttpResponse response = (HttpResponse) msg;
        if (upgradeWebSocket) {
            ctx.fireChannelRead(msg);
            ctx.pipeline().remove(this);
            if (!webSocketUpgraded(response)) {
                logger.debug("webSocket upgrade failed");
                return;
            }
            logger.debug("upgrade to web-socket");
            ctx.pipeline().replace("http-interceptor", "ws-interceptor",
                    new WebSocketInterceptor(address.host(), wsUrl, messageListener));
            ctx.pipeline().remove(HttpClientCodec.class);
            WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(false, true, 65536, false);
            WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(true);
            ctx.pipeline().addBefore("ws-interceptor", "ws-decoder", frameDecoder);
            ctx.pipeline().addBefore("ws-interceptor", "ws-encoder", frameEncoder);

            localPipeline.remove(HttpServerCodec.class);
            WebSocketFrameDecoder clientFrameDecoder = new WebSocket13FrameDecoder(true, true, 65536, false);
            WebSocketFrameEncoder clientFrameEncoder = new WebSocket13FrameEncoder(false);
            localPipeline.addBefore("replay-handler", "ws-decoder", clientFrameDecoder);
            localPipeline.addBefore("replay-handler", "ws-encoder", clientFrameEncoder);
        } else if (upgradeH2c) {
            //TODO: upgrade h2c http2 stream bug
            ctx.fireChannelRead(msg);
            ctx.pipeline().remove(this);
            if (!h2cUpgraded(response)) {
                //TODO: h2c upgrade
                logger.debug("h2c upgrade failed");
                return;
            }
            logger.debug("upgrade to h2c");

            var http2Interceptor = new Http2Interceptor(address, messageListener);
            Http2FrameCodec http2ClientCodec = Http2FrameCodecBuilder.forClient().build();
            Http2FrameCodec http2ServerCodec = Http2FrameCodecBuilder.forServer().build();
            ctx.pipeline().replace("http-codec", "http2-frame-codec", http2ClientCodec);
            ctx.pipeline().replace("http-interceptor", "http2-interceptor", http2Interceptor);
            localPipeline.replace("http-codec", "http2-frame-codec", http2ServerCodec);
            http2ClientCodec.connection().local().createStream(HTTP_UPGRADE_STREAM_ID, true);

        } else {
            ctx.fireChannelRead(msg);
            ctx.pipeline().remove(this);
            logger.warn("no upgrade found but get a response?");
        }
    }

    private Collection<String> getHeaderValues(String value) {
        return Stream.of(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private boolean webSocketUpgraded(HttpResponse response) {
        HttpHeaders headers = response.headers();
        String connection = Strings.nullToEmpty(headers.get("Connection"));
        String upgrade = Strings.nullToEmpty(headers.get("Upgrade"));
        return "Upgrade".equalsIgnoreCase(connection) && upgrade.equalsIgnoreCase("WebSocket");
    }

    private boolean h2cUpgraded(HttpResponse response) {
        return response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getMessage() == null || !cause.getMessage().contains("Connection reset by peer")) {
            logger.error("", cause);
        }
        super.exceptionCaught(ctx, cause);
    }

}