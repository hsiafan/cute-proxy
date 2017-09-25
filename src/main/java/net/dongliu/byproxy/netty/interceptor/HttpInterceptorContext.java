package net.dongliu.byproxy.netty.interceptor;

import net.dongliu.byproxy.MessageListener;
import net.dongliu.byproxy.struct.HttpRoundTripMessage;
import net.dongliu.byproxy.utils.NetAddress;

import java.util.Objects;

/**
 * Interceptor the http requests sent to proxy server
 */
public class HttpInterceptorContext {
    private HttpRoundTripMessage httpMessage;
    private boolean discard = true;

    private final boolean ssl;
    private final NetAddress address;
    private final MessageListener messageListener;

    public HttpInterceptorContext(boolean ssl, NetAddress address, MessageListener messageListener) {
        this.ssl = ssl;
        this.address = address;
        this.messageListener = Objects.requireNonNull(messageListener);
    }

    public String joinUrl(String path) {
        StringBuilder sb = new StringBuilder();
        if (ssl) {
            sb.append("https");
        } else {
            sb.append("http");
        }
        sb.append("://").append(address.getHost());
        if (!(ssl && address.getPort() == 443 || !ssl && address.getPort() == 80)) {
            sb.append(":").append(address.getPort());
        }
        sb.append(path);
        return sb.toString();
    }

    public HttpRoundTripMessage getHttpMessage() {
        return httpMessage;
    }

    public void setHttpMessage(HttpRoundTripMessage httpMessage) {
        this.httpMessage = httpMessage;
    }

    public boolean isDiscard() {
        return discard;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    public boolean isSsl() {
        return ssl;
    }

    public NetAddress getAddress() {
        return address;
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }
}
