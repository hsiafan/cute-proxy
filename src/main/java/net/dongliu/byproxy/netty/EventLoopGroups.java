package net.dongliu.byproxy.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

class EventLoopGroups {
    static EventLoopGroup master = new NioEventLoopGroup(1);
    static EventLoopGroup worker = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
}
