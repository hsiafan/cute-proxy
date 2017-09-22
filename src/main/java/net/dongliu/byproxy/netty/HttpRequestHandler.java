package net.dongliu.byproxy.netty;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handle plain, non-proxy http request.
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        String uri = request.uri();

        if (uri.equals("/") || uri.equals("/index.html")) { // index page
            CompletableFuture<Response> future = NettyUtils.callAsync(() -> {
                try (InputStream in = HttpRequestHandler.class.getResourceAsStream("/www/html/index.html")) {
                    return ByteStreams.toByteArray(in);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).thenApply(Response::html).exceptionally(Response::fromThrowable);

            future.thenAcceptAsync(r -> sendResponse(ctx, r, keepAlive), ctx.executor());
            return;
        }

        if (uri.equals("/ByProxy.crt")) {
            //TODO: send cert
        }

        if (uri.equals("/ByProxy.pem")) {

        }

        // 404
        sendResponse(ctx, Response.notFound(uri), keepAlive);
    }

    private static void sendResponse(ChannelHandlerContext ctx, Response r, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, r.status, Unpooled.wrappedBuffer(r.data));
        response.headers().set(CONTENT_TYPE, r.contentType);
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        ChannelFuture future = ctx.writeAndFlush(response);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static class Response {
        private HttpResponseStatus status;
        private String contentType;
        private byte[] data;

        public Response(HttpResponseStatus status, String contentType, byte[] data) {
            this.status = status;
            this.contentType = contentType;
            this.data = data;
        }

        public static Response notFound(String uri) {
            return new Response(NOT_FOUND, "text/plain; charset=utf-8", (uri + " not found").getBytes());
        }

        public static Response html(byte[] data) {
            return new Response(OK, "text/html; charset=utf-8", data);
        }

        public static Response fromThrowable(Throwable e) {
            return new Response(INTERNAL_SERVER_ERROR, "text/plain; charset=utf-8",
                    Throwables.getStackTraceAsString(e).getBytes(UTF_8));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("", cause);
        ctx.close();
    }
}
