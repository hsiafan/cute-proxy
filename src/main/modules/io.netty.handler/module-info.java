module io.netty.handler {
    requires transitive io.netty.buffer;
    requires transitive io.netty.common;
    requires transitive io.netty.transport;

    exports io.netty.handler.flow;
    exports io.netty.handler.flush;
    exports io.netty.handler.ipfilter;
    exports io.netty.handler.logging;
    exports io.netty.handler.ssl;
    exports io.netty.handler.stream;
    exports io.netty.handler.timeout;
    exports io.netty.handler.traffic;
}