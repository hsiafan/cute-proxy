package net.dongliu.byproxy;

import net.dongliu.byproxy.setting.KeyStoreSetting;
import net.dongliu.byproxy.setting.ProxySetting;
import net.dongliu.byproxy.setting.ServerSetting;
import net.dongliu.byproxy.ssl.SSLContextManager;

import java.io.IOException;
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
    private volatile ServerSetting serverSetting;
    private volatile KeyStoreSetting keyStoreSetting;
    private volatile ProxySetting proxySetting;
    private volatile SSLContextManager sslContextManager;

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
