open module io.netty.codec.http {
    requires transitive io.netty.buffer;
    requires transitive io.netty.codec;
    requires transitive io.netty.common;
    requires transitive io.netty.handler;
    requires transitive io.netty.transport;

    exports io.netty.handler.codec.http;
    exports io.netty.handler.codec.http.cookie;
    exports io.netty.handler.codec.http.cors;
    exports io.netty.handler.codec.http.multipart;
    exports io.netty.handler.codec.http.websocketx;
    exports io.netty.handler.codec.http.websocketx.extensions;
    exports io.netty.handler.codec.rtsp;
    exports io.netty.handler.codec.spdy;

}
