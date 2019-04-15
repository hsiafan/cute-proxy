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
import net.dongliu.commons.Strings;
import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.netty.codec.Http2EventCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS;
import static java.util.Objects.requireNonNullElse;

/**
 * Handle http upgrade(websocket, http2).
 * Note: http2 client may send preface frame directly, with no upgrade request.
 */
public class HttpUpgradeHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpUpgradeHandler.class);

    // the current http request/response
    private boolean upgradeWebSocket;
    private String wsUrl;
    private boolean upgradeH2c;
    private boolean upgradeWebSocketSucceed;
    private boolean upgradeH2cSucceed;

    private final boolean ssl;
    private final HostPort address;
    private final MessageListener messageListener;
    private final ChannelPipeline localPipeline;

    // for h2c upgrade
    private String method;
    private String path;

    public HttpUpgradeHandler(boolean ssl, HostPort address, MessageListener messageListener,
                              ChannelPipeline localPipeline) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = messageListener;
        this.localPipeline = localPipeline;
    }

    // read request
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof HttpMessage)) {
            logger.debug("not http message: {}", msg.getClass().getName());
            ctx.write(msg, promise);
            return;
        }

        if (msg instanceof HttpRequest) {
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
                        method = request.method().name();
                        path = request.uri();
                        break;
                    default:
                        logger.warn("unsupported upgrade header value: {}", upgrade);
                }
            }
        }

        if (msg instanceof HttpContent) {
            if (msg instanceof LastHttpContent) {
                if (!upgradeH2c && !upgradeWebSocket) {
                    ctx.pipeline().remove(this);
                }
            }
        }
        ctx.write(msg, promise);
    }

    private static String getUrl(boolean ssl, HostPort address, HttpRequest request) {
        HttpHeaders headers = request.headers();
        String host = requireNonNullElse(headers.get("Host"), address.host());
        StringBuilder sb = new StringBuilder(ssl ? "wss" : "ws").append("://").append(host);
        if (!host.contains(":")) {
            if (!(ssl && address.ensurePort() == 443 || !ssl && address.ensurePort() == 80)) {
                sb.append(":").append(address.port());
            }
        }
        sb.append(request.uri());
        return sb.toString();
    }

    // read response
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Http2Exception {
        if (!(msg instanceof HttpObject)) {
            logger.error("not http message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
            return;
        }

        if (msg instanceof HttpResponse) {
            // either upgradeWebSocket or upgradeH2c should be true
            HttpResponse response = (HttpResponse) msg;
            if (upgradeWebSocket) {
                upgradeWebSocketSucceed = webSocketUpgraded(response);
                if (!upgradeWebSocketSucceed) {
                    logger.debug("webSocket upgrade failed, url: {}", wsUrl);
                    ctx.fireChannelRead(msg);
                    ctx.pipeline().remove(this);
                    return;
                }
            } else if (upgradeH2c) {
                upgradeH2cSucceed = h2cUpgraded(response);
                if (!upgradeH2cSucceed) {
                    ctx.fireChannelRead(msg);
                    ctx.pipeline().remove(this);
                    logger.debug("h2c upgrade failed");
                    return;
                }
            } else {
                // when write request should have remove this handler
                logger.debug("no upgrade found but get a response?");
            }
        }

        if (msg instanceof HttpContent) {
            if (msg instanceof LastHttpContent) {
                ctx.fireChannelRead(msg);
                ctx.pipeline().remove(this);
                if (upgradeWebSocketSucceed) {
                    upgradeWebSocket(ctx);
                } else if (upgradeH2cSucceed) {
                    upgradeHttp2(ctx, method, path);
                }
                return;
            }
        }

        ctx.fireChannelRead(msg);
    }

    private void upgradeWebSocket(ChannelHandlerContext ctx) {
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
    }

    private void upgradeHttp2(ChannelHandlerContext ctx, String method, String path) throws Http2Exception {
        logger.debug("upgrade to h2c");

        var http2Interceptor = new Http2Interceptor(address, messageListener, true, method, path);
        localPipeline.replace("http-codec", "http2-frame-codec", new Http2EventCodec());
        ctx.pipeline().replace("http-codec", "http2-frame-codec", new Http2EventCodec());
        ctx.pipeline().replace("http-interceptor", "http2-interceptor", http2Interceptor);
    }

    private Collection<String> getHeaderValues(String value) {
        return Stream.of(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private boolean webSocketUpgraded(HttpResponse response) {
        return checkUpgradeResponse(response, "websocket");
    }

    private boolean checkUpgradeResponse(HttpResponse response, String protocol) {
        HttpHeaders headers = response.headers();
        if (!response.status().equals(SWITCHING_PROTOCOLS)) {
            return false;
        }
        String connectionHeader = Strings.nullToEmpty(headers.get("Connection"));
        String upgradeHeader = Strings.nullToEmpty(headers.get("Upgrade"));
        return connectionHeader.equals("Upgrade") && upgradeHeader.equalsIgnoreCase(protocol);
    }

    private boolean h2cUpgraded(HttpResponse response) {
        return checkUpgradeResponse(response, "h2c");
    }

}