open module io.netty.handler.proxy {
    requires io.netty.buffer;
    requires io.netty.codec;
    requires io.netty.codec.socks;

    requires transitive io.netty.codec.http;
    requires transitive io.netty.common;
    requires transitive io.netty.transport;

    exports io.netty.handler.proxy;

}
