package net.dongliu.proxy.netty;

import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.dongliu.proxy.setting.ProxySetting;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * For using proxy when create connection to target
 */
public class ProxyHandlerSupplier implements Supplier<ProxyHandler> {
    private final ProxySetting proxySetting;
    private final InetSocketAddress address;


    public ProxyHandlerSupplier(ProxySetting proxySetting) {
        this.proxySetting = proxySetting;
        // Java InetSocketAddress always do dns resolver.
        // we just create InetSocketAddress now so dns resolve will not block netty event loop.
        this.address = new InetSocketAddress(proxySetting.host(), proxySetting.port());
    }

    @Override
    public ProxyHandler get() {
        if (!proxySetting.use()) {
            return null;
        }
        ProxyHandler proxyHandler = newProxyHandler();
        proxyHandler.setConnectTimeoutMillis(NettySettings.CONNECT_TIMEOUT);
        return proxyHandler;
    }

    public ProxyHandler newProxyHandler() {
        switch (proxySetting.type()) {
            case ProxySetting.TYPE_HTTP:
                if (proxySetting.user().isEmpty()) {
                    return new HttpProxyHandler(address);
                } else {
                    return new HttpProxyHandler(address, proxySetting.user(), proxySetting.password());
                }

            case ProxySetting.TYPE_SOCKS5:
                if (proxySetting.user().isEmpty()) {
                    return new Socks5ProxyHandler(address);
                } else {
                    return new Socks5ProxyHandler(address, proxySetting.user(), proxySetting.password());
                }
            case ProxySetting.TYPE_SOCKS4:
                if (proxySetting.user().isEmpty()) {
                    return new Socks4ProxyHandler(address);
                } else {
                    return new Socks4ProxyHandler(address, proxySetting.user());
                }
            default:
                throw new RuntimeException("unknown proxy type: " + proxySetting.type());
        }
    }
}
