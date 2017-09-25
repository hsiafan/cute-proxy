package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

import java.util.function.Consumer;

/**
 * Matcher for protocol.
 */
public abstract class ProtocolMatcher {

    static int MATCH = 1;
    static int DISMATCH = -1;
    static int PENDING = 0;

    private Consumer<ChannelPipeline> consumer = p -> {
    };

    /**
     * If match the protocol.
     *
     * @return 1:match, -1:not match, 0:still can not judge now
     */
    protected abstract int match(ByteBuf buf);

    public ProtocolMatcher onMatched(Consumer<ChannelPipeline> consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * Deal with the pipeline when matched
     */
    void handlePipeline(ChannelPipeline pipeline) {
        consumer.accept(pipeline);
    }
}
