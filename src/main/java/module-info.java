module net.dongliu.proxy {
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.sql;
    requires jdk.charsets;
    requires jdk.crypto.cryptoki;

    // modular jar
    requires net.dongliu.commons;
    requires org.slf4j;
    requires org.slf4j.simple;

    // dependency with stable module name
    requires io.netty.handler;
    requires io.netty.codec;
    requires io.netty.buffer;
    requires io.netty.transport;
    requires io.netty.handler.proxy;
    requires io.netty.common;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;
    requires io.netty.codec.socks;
    requires org.jsoup;
    requires org.tukaani.xz;

    // dependency without stable module name
    requires dec;
    requires com.google.gson;
    requires bcprov.jdk15on;
    requires bcpkix.jdk15on;

    // open for javafx reflection
    opens net.dongliu.proxy;
    opens net.dongliu.proxy.ui;
    opens net.dongliu.proxy.ui.component;
    opens styles;
    opens images;
}