package net.dongliu.proxy;

import net.dongliu.proxy.netty.handler.ServerSSLContextManager;
import net.dongliu.proxy.setting.KeyStoreSetting;
import net.dongliu.proxy.setting.ProxySetting;
import net.dongliu.proxy.setting.ServerSetting;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Context and settings
 *
 * @author Liu Dong
 */
public class Context {
    private volatile ServerSetting serverSetting;
    private volatile KeyStoreSetting keyStoreSetting;
    private volatile ProxySetting proxySetting;
    private volatile ServerSSLContextManager sslContextManager;

    private static Context instance = new Context();

    private Context() {
    }


    /**
     * Set new keyStore. This may cause create new SslContextManager if keyStore file changed.
     */
    public void keyStoreSetting(KeyStoreSetting setting) {
        Objects.requireNonNull(setting);
        Path path = Paths.get(setting.usedKeyStore());
        if (sslContextManager == null || !path.equals(sslContextManager.getRootKeyStorePath())) {
            this.sslContextManager = new ServerSSLContextManager(path, setting.usedPassword().toCharArray());
        }
        this.keyStoreSetting = setting;
    }

    public void serverSetting(ServerSetting serverSetting) {
        this.serverSetting = Objects.requireNonNull(serverSetting);
    }

    public void proxySetting(ProxySetting proxySetting) {
        this.proxySetting = Objects.requireNonNull(proxySetting);
    }

    public static Context getInstance() {
        return instance;
    }

    public ServerSetting serverSetting() {
        return serverSetting;
    }

    public KeyStoreSetting keyStoreSetting() {
        return keyStoreSetting;
    }

    public ProxySetting proxySetting() {
        return proxySetting;
    }

    public ServerSSLContextManager sslContextManager() {
        return sslContextManager;
    }
}
