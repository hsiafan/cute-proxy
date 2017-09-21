package net.dongliu.byproxy.netty.switcher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

/**
 * Matcher for protocol.
 */
public interface ProtocolMatcher {

    int MATCH = 1;
    int DISMATCH = -1;
    int PENDING = 0;

    /**
     * If match the protocol.
     *
     * @return 1:match, -1:not match, 0:still can not judge now
     */
    int match(ByteBuf buf);

    /**
     * Deal with the pipeline when matched
     */
    void handlePipeline(ChannelPipeline pipeline);
}
