package net.dongliu.proxy.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseTimeoutChannelHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(CloseTimeoutChannelHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            logger.info("connection timeout, close");
            ctx.channel().close();
        }
    }
}
