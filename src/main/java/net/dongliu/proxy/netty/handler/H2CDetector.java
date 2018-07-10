package net.dongliu.proxy.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http2.Http2CodecUtil.connectionPrefaceBuf;

/**
 * Detect h2c upgrade http2 connection.
 * Peek inbound message to determine current connection wants to start HTTP/2
 */
public class H2CDetector extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(H2CDetector.class);
    private static final ByteBuf CONNECTION_PREFACE = unreleasableBuffer(connectionPrefaceBuf());

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int prefaceLength = CONNECTION_PREFACE.readableBytes();
        int bytesRead = Math.min(in.readableBytes(), prefaceLength);

        if (!ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(),
                in, in.readerIndex(), bytesRead)) {
            ctx.pipeline().remove(this);
        } else if (bytesRead == prefaceLength) {
            // Full h2 preface match, removed source codec, using http2 codec to handle
            // following network traffic
            ctx.pipeline().remove(HttpServerCodec.class);
            ctx.pipeline().remove(HttpInterceptor.class);

//            ctx.pipeline().addLast(Http2FrameCodec);
            ctx.pipeline().remove(this);
        }
    }

}
