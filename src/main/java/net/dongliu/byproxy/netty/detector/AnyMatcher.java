package net.dongliu.byproxy.netty.detector;

import io.netty.buffer.ByteBuf;

public class AnyMatcher extends ProtocolMatcher {

    public AnyMatcher() {
    }

    @Override
    public int match(ByteBuf buf) {
        return MATCH;
    }

}
