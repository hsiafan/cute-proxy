package net.dongliu.byproxy.netty.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.netty.detector.ProtocolDetector;
import net.dongliu.byproxy.netty.interceptor.HttpInterceptor;
import net.dongliu.byproxy.utils.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;

/**
 * Detect ssl and alpn protocol
 */
public class SSLDetector extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolDetector.class);

    private final Cumulator cumulator = MERGE_CUMULATOR;
    private ByteBuf buf;
    private boolean isSSL;
    private Queue<ByteBuf> queue;

    private final NetAddress address;
    private final MessageListener messageListener;
    private final Channel outboundChannel;
    private final ServerSSLContextManager sslContextManager;

    public SSLDetector(NetAddress address, MessageListener messageListener, Channel outboundChannel,
                       ServerSSLContextManager sslContextManager) {
        this.address = address;
        this.messageListener = messageListener;
        this.outboundChannel = outboundChannel;
        this.sslContextManager = sslContextManager;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            logger.error("unexpected message type for ProtocolDetector: {}", msg.getClass());
            NettyUtils.closeOnFlush(ctx.channel());
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        if (isSSL) {
            queue.add(in);
            return;
        }

        if (buf == null) {
            buf = in;
        } else {
            buf = cumulator.cumulate(ctx.alloc(), buf, in);
        }

        if (buf.readableBytes() < 3) {
            return;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        byte third = buf.getByte(buf.readerIndex() + 2);
        if (!(first == 22 && second <= 3 && third <= 3)) {
            // not ssl
            setHttpInterceptor(ctx, false);
            ctx.pipeline().remove(this);
            return;
        }


        isSSL = true;
        queue = new ArrayDeque<>(2);

        SslContext sslContext = ClientSSLContextManager.getInstance().get();
        SslHandler sslHandler = sslContext.newHandler(ctx.alloc(), address.getHost(), address.getPort());
        outboundChannel.pipeline().addLast("ssl-handler", sslHandler);
        outboundChannel.pipeline().addLast(new ApplicationProtocolNegotiationHandler(HTTP_1_1) {
            @Override
            protected void configurePipeline(ChannelHandlerContext c, String protocol) {
                logger.debug("alpn with target server {}: {}", address.getHost(), protocol);

                boolean useH2 = protocol.equalsIgnoreCase(HTTP_2);
                SslContext serverContext = sslContextManager.createSSlContext(address.getHost(), useH2);
                SslHandler serverSSLHandler = serverContext.newHandler(ctx.alloc());
                ctx.pipeline().addLast("ssl-handler", serverSSLHandler);
                ctx.pipeline().addLast(new ApplicationProtocolNegotiationHandler(HTTP_1_1) {
                    @Override
                    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                        logger.debug("alpn with client {}: {}", address.getHost(), protocol);
                        if (protocol.equalsIgnoreCase(HTTP_2)) {
                            setHttp2Interceptor(ctx);
                        } else {
                            setHttpInterceptor(ctx, true);
                        }
                    }
                });
                ctx.pipeline().remove(SSLDetector.this);
            }
        });
    }

    private void setHttp2Interceptor(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(outboundChannel));
        outboundChannel.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(ctx.channel()));
    }

    private void setHttpInterceptor(ChannelHandlerContext ctx, boolean ssl) {
        ctx.pipeline().addLast(new HttpServerCodec());
        ctx.pipeline().addLast("", new HttpServerExpectContinueHandler());
        ctx.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(outboundChannel));

        outboundChannel.pipeline().addLast(new HttpClientCodec());
        HttpInterceptor interceptor = new HttpInterceptor(ssl, address, messageListener, () -> {
            ctx.pipeline().remove(HttpServerCodec.class);
            WebSocketFrameDecoder frameDecoder = new WebSocket13FrameDecoder(true, true, 65536, false);
            WebSocketFrameEncoder frameEncoder = new WebSocket13FrameEncoder(false);
            ctx.pipeline().addBefore("tcp-tunnel-handler", "ws-decoder", frameDecoder);
            ctx.pipeline().addBefore("tcp-tunnel-handler", "ws-encoder", frameEncoder);
        });
        outboundChannel.pipeline().addLast("http-interceptor", interceptor);
        outboundChannel.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(ctx.channel()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (buf != null) {
            buf.release();
            buf = null;
        }
        logger.error("", cause);
        NettyUtils.closeOnFlush(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (buf != null) {
            ctx.fireChannelRead(buf);
        }
        if (queue != null) {
            ByteBuf b;
            while ((b = queue.poll()) != null) {
                ctx.fireChannelRead(b);
            }
        }
        ctx.flush();
        queue = null;
        buf = null;
    }
}

