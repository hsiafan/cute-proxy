module net.dongliu.byproxy {
    requires javafx.fxml;
    requires bcprov.jdk15on;
    requires bcpkix.jdk15on;
    requires io.netty.handler;
    requires slf4j.api;
    requires io.netty.codec;
    requires io.netty.buffer;
    requires io.netty.transport;
    requires io.netty.handler.proxy;
    requires io.netty.common;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.swing;
    requires org.apache.commons.compress;
    requires io.netty.codec.socks;
    requires org.jsoup;
    requires image4j;
    requires dec;
    // gson requires this module
    requires java.sql;
    requires gson;

    opens net.dongliu.byproxy;
    opens net.dongliu.byproxy.ui;
    opens net.dongliu.byproxy.ui.component;
    opens styles;
    opens images;
}