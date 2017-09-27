package net.dongliu.byproxy.netty;

import com.google.common.base.Strings;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.dongliu.byproxy.setting.ProxySetting;

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
        // create InetSocketAddress now so dns resolve will not block netty event loop.
        this.address = new InetSocketAddress(proxySetting.getHost(), proxySetting.getPort());
    }

    @Override
    public ProxyHandler get() {
        ProxyHandler proxyHandler = newProxyHandler();
        proxyHandler.setConnectTimeoutMillis(NettySettings.CONNECT_TIMEOUT);
        return proxyHandler;
    }

    public ProxyHandler newProxyHandler() {
        switch (proxySetting.getType()) {
            case ProxySetting.TYPE_HTTP:
                if (Strings.isNullOrEmpty(proxySetting.getUser())) {
                    return new HttpProxyHandler(address);
                } else {
                    return new HttpProxyHandler(address, proxySetting.getUser(), proxySetting.getPassword());
                }

            case ProxySetting.TYPE_SOCKS5:
                if (Strings.isNullOrEmpty(proxySetting.getUser())) {
                    return new Socks5ProxyHandler(address);
                } else {
                    return new Socks5ProxyHandler(address, proxySetting.getUser(), proxySetting.getPassword());
                }
            case ProxySetting.TYPE_SOCKS4:
                if (Strings.isNullOrEmpty(proxySetting.getUser())) {
                    return new Socks4ProxyHandler(address);
                } else {
                    return new Socks4ProxyHandler(address, proxySetting.getUser());
                }
            default:
                throw new RuntimeException("unknown proxy type: " + proxySetting.getType());
        }
    }
}
