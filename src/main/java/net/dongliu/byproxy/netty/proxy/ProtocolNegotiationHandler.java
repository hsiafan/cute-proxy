package net.dongliu.byproxy.netty.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_1_1;
import static io.netty.handler.ssl.ApplicationProtocolNames.HTTP_2;

/**
 * Application Protocol handler for ALPN. This handler must be add after SSLHandler.
 * This mainly used for distinct http1.1/http2 traffics now.
 */
public class ProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolNegotiationHandler.class);

    protected ProtocolNegotiationHandler() {
        super(HTTP_1_1);
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {

        logger.info("protocol: {}", protocol);
        if (HTTP_2.equals(protocol)) {
        } else if (HTTP_1_1.equals(protocol)) {
        } else {
            throw new IllegalStateException("unknown protocol: " + protocol);
        }
    }
}
