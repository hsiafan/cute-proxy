package net.dongliu.proxy.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.*;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.data.WebSocketMessage;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.store.BodyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static net.dongliu.proxy.netty.NettyUtils.causedByClientClose;

/**
 * Websocket frame interceptor. This interceptor is set on connection to target server.
 */
public class WebSocketInterceptor extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketInterceptor.class);

    // for multi fragment web-socket message
    private WebSocketMessage requestMessage;
    private WebSocketMessage responseMessage;

    private final String host;
    private final String url;
    private final MessageListener messageListener;

    public WebSocketInterceptor(String host, String url, MessageListener messageListener) {
        this.host = host;
        this.url = url;
        this.messageListener = Objects.requireNonNull(messageListener);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        filterWebSocketFrame(ctx, msg, false);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        filterWebSocketFrame(ctx, msg, true);
        ctx.write(msg, promise);
    }

    private void filterWebSocketFrame(ChannelHandlerContext ctx, Object msg, boolean request) {
        if (msg instanceof EmptyByteBuf) {
            ((EmptyByteBuf) msg).release();
            return;
        }
        if (!(msg instanceof WebSocketFrame)) {
            logger.debug("not web-socket frame type: {}", msg.getClass().getName());
        }


        if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            logger.debug("BinaryWebSocketFrame received");
            newWebSocketMessage(ctx, frame, WebSocketMessage.TYPE_BINARY, request);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            logger.debug("TextWebSocketFrame received");
            newWebSocketMessage(ctx, frame, WebSocketMessage.TYPE_TEXT, request);
        } else if (msg instanceof PingWebSocketFrame) {
            logger.debug("PingWebSocketFrame received");
        } else if (msg instanceof PongWebSocketFrame) {
            logger.debug("PongWebSocketFrame received");
        } else if (msg instanceof CloseWebSocketFrame) {
            CloseWebSocketFrame frame = (CloseWebSocketFrame) msg;
            logger.debug("CloseWebSocketFrame received, status:{}, reason:{}", frame.statusCode(), frame.reasonText());
        } else if (msg instanceof ContinuationWebSocketFrame) {
            WebSocketMessage message;
            if (request) {
                message = requestMessage;
            } else {
                message = responseMessage;
            }
            if (message == null) {
                logger.error("ContinuationWebSocketFrame without first frame");
            } else {
                ContinuationWebSocketFrame frame = (ContinuationWebSocketFrame) msg;
                ByteBuf content = frame.content();
                message.body().append(content.nioBuffer());
                if (frame.isFinalFragment()) {
                    message.body().finish();
                    if (request) {
                        requestMessage = message;
                    } else {
                        responseMessage = message;
                    }
                }
            }

        } else {
            logger.warn("unsupported WebSocketFrame: {}", msg.getClass().getName());
        }
    }

    private void newWebSocketMessage(ChannelHandlerContext ctx, WebSocketFrame frame, int type, boolean request) {
        BodyType bodyType = type == WebSocketMessage.TYPE_TEXT ? BodyType.text : BodyType.binary;
        Body body = new Body(bodyType, Optional.empty(), "");
        ByteBuf content = frame.content();
        body.append(content.nioBuffer());
        WebSocketMessage message = new WebSocketMessage(host, url, type, request, body);
        messageListener.onMessage(message);
        if (frame.isFinalFragment()) {
            body.finish();
        } else {
            if (request) {
                responseMessage = message;
            } else {
                responseMessage = message;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (causedByClientClose(cause)) {
            logger.warn("client closed connection: {}", cause.getMessage());
        } else {
            logger.error("websocket error", cause);
        }
        ctx.close();
    }
}