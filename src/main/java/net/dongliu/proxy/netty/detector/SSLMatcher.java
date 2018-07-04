package net.dongliu.proxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class SSLMatcher extends ProtocolMatcher {

    private Consumer<ChannelPipeline> consumer;

    public SSLMatcher(Consumer<ChannelPipeline> consumer) {
        this.consumer = requireNonNull(consumer);
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 3) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        byte third = buf.getByte(buf.readerIndex() + 2);
        if (first == 22 && second <= 3 && third <= 3) {
            return MATCH;
        }
        return MISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        consumer.accept(pipeline);
    }
}
