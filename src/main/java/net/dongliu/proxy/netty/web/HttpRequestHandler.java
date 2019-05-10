package net.dongliu.proxy.netty.web;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import net.dongliu.proxy.netty.NettyUtils;
import net.dongliu.proxy.netty.handler.ServerSSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Handle plain, non-proxy http request.
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private final ServerSSLContextManager sslContextManager;

    public HttpRequestHandler(ServerSSLContextManager sslContextManager) {
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        String uri = request.uri();

        if (uri.equals("/") || uri.equals("/index.html")) { // index page
            ResourceLoader.getInstance().loadClassPathResource("/www/html/index.html")
                    .thenApply(WebResponse::html).exceptionally(WebResponse::fromThrowable)
                    .thenAcceptAsync(r -> sendWebResponse(ctx, r, keepAlive), ctx.executor());
            return;
        }

        if (uri.equals("/CuteProxy.crt")) {
            WebResponse response;
            if (sslContextManager != null) {
                byte[] bytes;
                try {
                    bytes = sslContextManager.getKeyStoreGenerator().exportRootCert(false);
                    response = new WebResponse(OK, "application/x-x509-ca-cert", bytes);
                } catch (Exception e) {
                    response = WebResponse.fromThrowable(e);
                }
            } else {
                response = WebResponse.text("No ssl manager specified");
            }
            sendWebResponse(ctx, response, keepAlive);
            return;
        }

        if (uri.equals("/CuteProxy.pem")) {
            WebResponse response;
            if (sslContextManager != null) {
                byte[] bytes;
                try {
                    bytes = sslContextManager.getKeyStoreGenerator().exportRootCert(true);
                    response = new WebResponse(OK, "application/x-pem-file", bytes);
                } catch (Exception e) {
                    response = WebResponse.fromThrowable(e);
                }
            } else {
                response = WebResponse.text("No ssl manager specified");
            }
            sendWebResponse(ctx, response, keepAlive);
            return;
        }

        // 404
        sendWebResponse(ctx, WebResponse.notFound(uri), keepAlive);
    }

    private static void sendWebResponse(ChannelHandlerContext ctx, WebResponse r, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, r.getStatus(),
                Unpooled.wrappedBuffer(r.getData()));
        response.headers().set(CONTENT_TYPE, r.getContentType());
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        ChannelFuture future = ctx.writeAndFlush(response);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("", cause);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}
