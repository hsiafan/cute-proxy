package net.dongliu.proxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Match any income request, used for default handler
 */
public class AnyMatcher extends ProtocolMatcher {
    private Consumer<ChannelPipeline> consumer;

    public AnyMatcher(Consumer<ChannelPipeline> consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public int match(ByteBuf buf) {
        return MATCH;
    }

    @Override
    protected void handlePipeline(ChannelPipeline pipeline) {
        consumer.accept(pipeline);
    }
}
