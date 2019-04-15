package net.dongliu.proxy.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers;
import net.dongliu.commons.collection.Iterables;
import net.dongliu.commons.net.HostPort;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.data.Header;
import net.dongliu.proxy.data.Http2Message;
import net.dongliu.proxy.data.Http2RequestHeaders;
import net.dongliu.proxy.data.Http2ResponseHeaders;
import net.dongliu.proxy.netty.codec.frame.*;
import net.dongliu.proxy.store.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static net.dongliu.proxy.netty.NettyUtils.causedByClientClose;

/**
 * Intercept http2 frames. This interceptor is set on connection to target server.
 */
public class Http2Interceptor extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(Http2Interceptor.class);

    private Map<Integer, Http2Message> messageMap = new HashMap<>();
    private final HostPort address;
    private final MessageListener messageListener;
    // if is from clear text upgrade
    private final boolean clearText;
    // only for clear text
    private final String method;
    private final String path;

    public Http2Interceptor(HostPort address, MessageListener messageListener, boolean clearText) {
        this.address = address;
        this.messageListener = messageListener;
        this.clearText = clearText;
        this.method = "";
        this.path = "";
    }

    public Http2Interceptor(HostPort address, MessageListener messageListener, boolean clearText,
                            String method, String path) {
        this.address = address;
        this.messageListener = messageListener;
        this.clearText = clearText;
        this.method = method;
        this.path = path;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof Http2StreamEvent)) {
            ctx.write(msg, promise);
            return;
        }
        var event = (Http2StreamEvent) msg;
        var streamId = event.streamId();
        logger.debug("send {} to {}, inId: {}", msg.getClass().getSimpleName(),
                ctx.channel().remoteAddress(), event.streamId());

        if (msg instanceof IHttp2HeadersEvent) {
            var headersEvent = (IHttp2HeadersEvent) msg;
            // create new Http2Message, set headers
            Http2RequestHeaders requestHeaders = onRequestHeaders(streamId, headersEvent.headers(),
                    headersEvent.endOfStream());
            logger.debug("http2 request received: {}, {}", address, requestHeaders);
        }

        if (msg instanceof Http2DataEvent) {
            var dataEvent = (Http2DataEvent) msg;
            Http2Message message = messageMap.get(streamId);
            if (message != null) {
                Body body = message.requestBody();
                body.append(dataEvent.data().nioBuffer());
                if (dataEvent.endOfStream()) {
                    body.finish();
                }

            } else {
                logger.error("message not found with stream id: {}", streamId);
                throw new RuntimeException("message not found with stream id: " + streamId);
            }
        }
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Http2Event)) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (!(msg instanceof Http2StreamEvent)) {
            ctx.fireChannelRead(msg);
            return;
        }
        var event = (Http2StreamEvent) msg;
        logger.debug("http2 event {} received from {}, steam id: {}", event.getClass().getSimpleName(),
                ctx.channel().remoteAddress(), event.streamId());
        var streamId = event.streamId();

        if (msg instanceof Http2PushPromiseEvent) {
            var pushPromiseEvent = (Http2PushPromiseEvent) event;
            onRequestHeaders(pushPromiseEvent.promisedStreamId(), pushPromiseEvent.headers(), true);
        }

        if (msg instanceof IHttp2HeadersEvent) {
            var headersEvent = (IHttp2HeadersEvent) event;
            Http2Message message = messageMap.get(streamId);
            if (message == null) {
                // h2c upgrade response
                if (clearText && streamId == 1) {
                    Http2RequestHeaders fakeRequestHeaders = new Http2RequestHeaders(
                            List.of(new Header("", "mock request for h2c upgrade. look back for upgrade request")),
                            "http", method, path);
                    Body body = fakeRequestHeaders.createBody();
                    body.finish();
                    message = new Http2Message(address, fakeRequestHeaders, body);
                    messageListener.onMessage(message);
                    messageMap.put(streamId, message);
                }
            }

            if (message != null) {
                Http2Headers nettyHeaders = headersEvent.headers();
                List<Header> headers = convertHeaders(nettyHeaders);
                var responseHeaders = new Http2ResponseHeaders(Integer.parseInt(nettyHeaders.status().toString()),
                        headers);
                message.setResponseHeader(responseHeaders);
                message.setResponseBody(responseHeaders.createBody());
                if (headersEvent.endOfStream()) {
                    message.responseBody().finish();
                    messageMap.remove(streamId);
                }
            } else {
                logger.error("message for stream id {} not found", streamId);
            }
        }

        if (msg instanceof Http2DataEvent) {
            var dataEvent = (Http2DataEvent) msg;
            Http2Message message = messageMap.get(streamId);
            if (message != null) {
                Body body = message.responseBody();
                body.append(dataEvent.data().nioBuffer());
                if (dataEvent.endOfStream()) {
                    body.finish();
                    messageMap.remove(streamId);
                }
            } else {
                logger.error("message for stream id {} not found", streamId);
            }
        }

        ctx.fireChannelRead(msg);
    }


    private Http2RequestHeaders onRequestHeaders(int streamId, Http2Headers http2Headers, boolean endOfStream) {
        logger.debug("request header for: {} {}, streamId: {}", http2Headers.method(), http2Headers.path(), streamId);
        List<Header> headers = convertHeaders(http2Headers);
        Http2RequestHeaders requestHeaders = new Http2RequestHeaders(headers,
                http2Headers.scheme().toString(),
                http2Headers.method().toString(),
                http2Headers.path().toString()
        );
        Http2Message message = new Http2Message(address, requestHeaders, requestHeaders.createBody());
        if (endOfStream) {
            Body body = message.requestBody();
            body.finish();
        }
        messageListener.onMessage(message);
        messageMap.put(streamId, message);
        return requestHeaders;
    }

    private List<Header> convertHeaders(Http2Headers nettyHeaders) {
        return Iterables.stream(nettyHeaders)
                .filter((e -> e.getKey().length() > 0))
                .map(e -> new Header(e.getKey().toString(), e.getValue().toString()))
                .collect(toList());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (causedByClientClose(cause)) {
            logger.warn("client closed connection: {}", cause.getMessage());
        } else {
            logger.error("http2 error", cause);
        }
        ctx.close();
    }
}