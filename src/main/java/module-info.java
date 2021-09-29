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
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires com.google.gson;
    requires org.tukaani.xz;

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

    // dependency without stable module name
    requires dec;

    // open for javafx reflection
    opens net.dongliu.proxy;
    opens net.dongliu.proxy.ui;
    opens net.dongliu.proxy.ui.component;
    opens styles;
    opens images;
}