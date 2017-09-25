package net.dongliu.byproxy.netty.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.netty.NettyUtils;
import net.dongliu.byproxy.ssl.SSLContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class Socks5ProxyAuthHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger logger = LoggerFactory.getLogger(Socks5ProxyAuthHandler.class);

    @Nullable
    private final MessageListener messageListener;
    @Nullable
    private final SSLContextManager sslContextManager;

    public Socks5ProxyAuthHandler(@Nullable MessageListener messageListener,
                                  @Nullable SSLContextManager sslContextManager) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        if (socksRequest.version() != SocksVersion.SOCKS5) {
            ctx.close();
            return;
        }
        if (socksRequest instanceof Socks5InitialRequest) {
            // auth support example
            //ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
            //ctx.write(new DefaultSocks5AuthMethodResponse(Socks5AuthMethod.PASSWORD));
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
        } else if (socksRequest instanceof Socks5CommandRequest) {
            Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
            if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                ctx.pipeline().addLast(new Socks5ProxyConnectHandler(messageListener, sslContextManager));
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(socksRequest);
            } else {
                ctx.close();
            }
        } else {
            logger.error("unknown socks5 command: {}", socksRequest.getClass().getSimpleName());
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("", e);
        NettyUtils.closeOnFlush(ctx.channel());
    }
}