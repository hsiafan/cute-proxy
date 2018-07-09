package net.dongliu.proxy.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;
import net.dongliu.proxy.MessageListener;
import net.dongliu.proxy.data.Header;
import net.dongliu.proxy.data.Http2Message;
import net.dongliu.proxy.data.Http2RequestHeaders;
import net.dongliu.proxy.data.Http2ResponseHeaders;
import net.dongliu.proxy.store.Body;
import net.dongliu.proxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Intercept http2 frames. This interceptor is set on connection to target server.
 */
public class Http2Interceptor extends Http2ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(Http2Interceptor.class);

    private Map<Integer, Http2FrameStream> streamMap = new HashMap<>();
    private Map<Integer, Http2FrameStream> reverseMap = new HashMap<>();
    private Map<Integer, Http2Message> messageMap = new HashMap<>();
    private final NetAddress address;
    private final MessageListener messageListener;

    public Http2Interceptor(NetAddress address, MessageListener messageListener) {
        this.address = address;
        this.messageListener = messageListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof Http2StreamFrame)) {
            ctx.fireChannelRead(msg);
            return;
        }
        int targetId = ((Http2StreamFrame) msg).stream().id();
        logger.debug("received {}, outId: {}", msg.getClass(), targetId);

        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame frame = (Http2HeadersFrame) msg;
            Http2Message message = messageMap.get(targetId);
            if (message != null) {
                Http2Headers nettyHeader = frame.headers();
                List<Header> headers = StreamSupport.stream(nettyHeader.spliterator(), false)
                        .filter(e -> e.getKey().charAt(0) != ':')
                        .map(e -> new Header(e.getKey().toString(), e.getValue().toString()))
                        .collect(toList());
                var responseHeaders = new Http2ResponseHeaders(Integer.parseInt(nettyHeader.status().toString()),
                        headers);
                message.setResponseHeader(responseHeaders);
                message.setResponseBody(responseHeaders.createBody());
                if (frame.isEndStream()) {
                    message.responseBody().finish();
                }
            } else {
                logger.error("message for stream id {} not found", targetId);
            }


            downFrame(ctx, frame, frame.isEndStream());
            return;
        }

        if (msg instanceof Http2DataFrame) {
            Http2DataFrame frame = (Http2DataFrame) msg;
            int bytes = frame.initialFlowControlledBytes();
            Http2FrameStream stream = frame.stream();
            Http2Message message = messageMap.get(targetId);
            if (message != null) {
                Body body = message.responseBody();
                body.append(frame.content().nioBuffer());
                if (frame.isEndStream()) {
                    body.finish();
                }
            } else {
                logger.error("message for stream id {} not found", targetId);
            }


            downFrame(ctx, frame, frame.isEndStream());
            // Update the flow controller
            ctx.write(new DefaultHttp2WindowUpdateFrame(bytes).stream(stream));
        }
    }

    public void downFrame(ChannelHandlerContext ctx, Http2StreamFrame frame, boolean isEndStream) {
        int targetId = frame.stream().id();
        Http2FrameStream sourceStream = reverseMap.get(targetId);
        if (sourceStream == null) {
            throw new RuntimeException("stream with id " + targetId + " not exists");
        }
        int sourceId = sourceStream.id();
        frame.stream(sourceStream);
        ctx.fireChannelRead(frame);
        if (isEndStream) {
            logger.debug("remove {} -> {}", sourceId, targetId);
            reverseMap.remove(targetId);
            streamMap.remove(sourceId);
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof Http2StreamFrame)) {
            ctx.write(msg, promise);
            return;
        }
        logger.debug("send {}, inId: {}", msg.getClass(), ((Http2StreamFrame) msg).stream().id());
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame frame = (Http2HeadersFrame) msg;
            // map streams
            Http2FrameStream fromStream = frame.stream();
            int sourceId = fromStream.id();
            Http2FrameStream stream = streamMap.get(sourceId);
            Http2FrameStream targetStream;
            if (stream == null) {
                stream = targetStream = newStream();
            } else {
                targetStream = null;
            }
            frame.stream(stream);

            // create new Http2Message, set headers
            Http2Headers nettyHeaders = frame.headers(); // netty http2 headers
            List<Header> headers = StreamSupport.stream(nettyHeaders.spliterator(), false)
                    .filter((e -> e.getKey().length() > 0))
                    .filter(e -> e.getKey().charAt(0) != ':')
                    .map(e -> new Header(e.getKey().toString(), e.getValue().toString()))
                    .collect(toList());
            Http2RequestHeaders requestHeaders = new Http2RequestHeaders(headers,
                    nettyHeaders.scheme().toString(), nettyHeaders.method().toString(), nettyHeaders.path().toString());
            Http2Message message = new Http2Message(address, requestHeaders, requestHeaders.createBody());

            logger.debug("http2 request received: {}, {}", address, requestHeaders);

            ctx.write(frame, promise);
            promise.addListener(fu -> {
                if (targetStream != null) {
                    streamMap.put(sourceId, targetStream);
                    int targetId = targetStream.id();
                    reverseMap.put(targetId, fromStream);
                    messageMap.put(targetId, message);
                    if (frame.isEndStream()) {
                        Body body = message.requestBody();
                        body.finish();
                        messageListener.onMessage(message);
                    }
                }
            });
            return;

        }

        if (msg instanceof Http2DataFrame) {
            Http2DataFrame frame = (Http2DataFrame) msg;
            int sourceId = frame.stream().id();
            Http2FrameStream targetStream = streamMap.get(sourceId);
            if (targetStream == null) {
                throw new RuntimeException("stream with id " + sourceId + " not exists");
            }
            int targetId = targetStream.id();
            Http2Message message = messageMap.get(targetId);
            if (message != null) {
                Body body = message.requestBody();
                body.append(frame.content().nioBuffer());
                if (frame.isEndStream()) {
                    body.finish();
                }

            } else {
                logger.error("message not found with stream id: {}", targetId);
                throw new RuntimeException("message not found with stream id: " + targetId);
            }
            frame.stream(targetStream);
            ctx.write(frame, promise);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("", cause);
        ctx.close();
    }
}