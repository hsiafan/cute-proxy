package net.dongliu.byproxy;

import net.dongliu.byproxy.proxy.SSLContextManager;
import net.dongliu.byproxy.proxy.SSLUtils;
import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.MainSetting;
import net.dongliu.byproxy.setting.ProxySetting;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Context and settings
 *
 * @author Liu Dong
 */
public class Context {
    private volatile MainSetting mainSetting;
    private volatile KeyStoreSetting keyStoreSetting;
    private volatile ProxySetting proxySetting;
    private volatile SSLContextManager sslContextManager;
    private volatile Dialer dialer;
    private volatile Proxy proxy = Proxy.NO_PROXY;

    private static Context instance = new Context();

    private Context() {
    }


    public void setKeyStoreSetting(KeyStoreSetting setting) {
        Objects.requireNonNull(setting);
        Path path = Paths.get(setting.usedKeyStore());
        try {
            if (sslContextManager == null ||
                    Files.isSameFile(path, Paths.get(this.sslContextManager.getKeyStorePath()))) {
                this.sslContextManager = new SSLContextManager(setting.usedKeyStore(),
                        setting.usedPassword().toCharArray());
            }
        } catch (IOException e) {
            this.sslContextManager = new SSLContextManager(setting.usedKeyStore(),
                    setting.usedPassword().toCharArray());
        }
        this.keyStoreSetting = setting;
    }

    public void setMainSetting(MainSetting mainSetting) {
        this.mainSetting = Objects.requireNonNull(mainSetting);
    }

    public void setProxySetting(ProxySetting proxySetting) {
        Objects.requireNonNull(proxySetting);
        if (proxySetting.isUse()) {
            dialer = (host, port) -> {
                InetSocketAddress proxyAddress = new InetSocketAddress(proxySetting.getHost(), proxySetting.getPort());
                if (proxySetting.getType().equals("socks5")) {
                    proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
                } else if (proxySetting.getType().equals("http")) {
                    proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
                } else {
                    throw new RuntimeException("unsupported proxy type: " + proxySetting.getType());
                }
                Socket socket = new Socket(proxy);
                socket.connect(InetSocketAddress.createUnresolved(host, port));
                return socket;
            };
        } else {
            dialer = Socket::new;
            proxy = Proxy.NO_PROXY;
        }


        if (proxySetting.isUse() && (this.proxySetting == null ||
                !proxySetting.getUser().equals(this.proxySetting.getUser()) ||
                !proxySetting.getPassword().equals(this.proxySetting.getPassword()))) {
            if (proxySetting.getUser().isEmpty()) {
                Authenticator.setDefault(null);
            } else {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxySetting.getUser(),
                                proxySetting.getPassword().toCharArray());
                    }
                });
            }
        }
        this.proxySetting = proxySetting;
    }

    public static Context getInstance() {
        return instance;
    }

    /**
     * create plain socket
     */
    public Socket createSocket(String host, int port) throws IOException {
        return dialer.dial(host, port);
    }

    /**
     * create trust-all ssl socket
     */
    public SSLSocket createSSLSocket(String host, int port) throws IOException {
        SSLContext clientSSlContext = SSLUtils.createClientSSlContext();
        SSLSocketFactory factory = clientSSlContext.getSocketFactory();
        return (SSLSocket) factory.createSocket(createSocket(host, port), host, port, true);
    }

    public MainSetting getMainSetting() {
        return mainSetting;
    }

    public KeyStoreSetting getKeyStoreSetting() {
        return keyStoreSetting;
    }

    public ProxySetting getProxySetting() {
        return proxySetting;
    }

    public SSLContextManager getSslContextManager() {
        return sslContextManager;
    }

    public Dialer getDialer() {
        return dialer;
    }

    public Proxy getProxy() {
        return proxy;
    }
}
