package net.dongliu.byproxy;

import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.ProxySetting;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.ssl.SSLContextManager;

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
    private volatile SSLContextManager sslContextManager;

    private static Context instance = new Context();

    private Context() {
    }


    /**
     * Set new keyStore. This may cause create new SslContextManager if keyStore file changed.
     */
    public void setKeyStoreSetting(KeyStoreSetting setting) {
        Objects.requireNonNull(setting);
        Path path = Paths.get(setting.usedKeyStore());
        if (sslContextManager == null || !path.equals(sslContextManager.getRootKeyStorePath())) {
            this.sslContextManager = new SSLContextManager(path, setting.usedPassword().toCharArray());
        }
        this.keyStoreSetting = setting;
    }

    public void setServerSetting(ServerSetting serverSetting) {
        this.serverSetting = Objects.requireNonNull(serverSetting);
    }

    public void setProxySetting(ProxySetting proxySetting) {
        this.proxySetting = Objects.requireNonNull(proxySetting);
    }

    public static Context getInstance() {
        return instance;
    }

    public ServerSetting getServerSetting() {
        return serverSetting;
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
}
