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
import net.dongliu.commons.Strings;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

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
    private final ChannelPipeline clientPipeline;

    public HttpUpgradeHandler(boolean ssl, NetAddress address, MessageListener messageListener,
                              ChannelPipeline clientPipeline) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = messageListener;
        this.clientPipeline = clientPipeline;
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
        if ("Upgrade".equalsIgnoreCase(headers.get("Connection"))) {
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
        String host = Objects.requireNonNullElse(headers.get("Host"), address.getHost());
        StringBuilder sb = new StringBuilder(ssl ? "wss" : "ws").append("://").append(host);
        if (!(ssl && address.getPort() == 443 || !ssl && address.getPort() == 80)) {
            sb.append(":").append(address.getPort());
        }
        sb.append(request.uri());
        return sb.toString();
    }

    // read response
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpResponse)) {
            logger.error("not http message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
            return;
        }

        // either upgradeWebSocket or upgradeH2c should be true
        HttpResponse response = (HttpResponse) msg;
        if (upgradeWebSocket) {
            ctx.fireChannelRead(msg);
            if (!webSocketUpgraded(response)) {
                ctx.pipeline().remove(this);
                logger.debug("webSocket upgrade failed");
                return;
            }
            logger.debug("upgrade to web-socket");
            //TODO: websocket message url
            ctx.pipeline().replace("http-interceptor", "ws-interceptor",
                    new WebSocketInterceptor(address.getHost(), wsUrl, messageListener));
            ctx.pipeline().remove(HttpClientCodec.class);
            WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(false, true, 65536, false);
            WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(true);
            ctx.pipeline().addBefore("ws-interceptor", "ws-decoder", frameDecoder);
            ctx.pipeline().addBefore("ws-interceptor", "ws-encoder", frameEncoder);

            clientPipeline.remove(HttpServerCodec.class);
            WebSocketFrameDecoder clientFrameDecoder = new WebSocket13FrameDecoder(true, true, 65536, false);
            WebSocketFrameEncoder clientFrameEncoder = new WebSocket13FrameEncoder(false);
            clientPipeline.addBefore("replay-handler", "ws-decoder", clientFrameDecoder);
            clientPipeline.addBefore("replay-handler", "ws-encoder", clientFrameEncoder);
        } else if (upgradeH2c) {
            ctx.fireChannelRead(msg);
            if (!h2cUpgraded(response)) {
                ctx.pipeline().remove(this);
                logger.debug("h2c upgrade failed");
                return;
            }
            logger.debug("upgrade to h2c");
        } else {
            ctx.fireChannelRead(msg);
            ctx.pipeline().remove(this);
            logger.warn("no upgrade found but get a response?");
        }
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