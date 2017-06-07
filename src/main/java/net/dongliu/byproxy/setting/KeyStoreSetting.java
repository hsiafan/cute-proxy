package net.dongliu.byproxy.setting;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Key store setting
 *
 * @author Liu Dong
 */
public class KeyStoreSetting implements Serializable {
    private static final long serialVersionUID = -8001899659204205513L;
    // path for keyStore file
    private String keyStore;
    private String keyStorePassword;
    private boolean useCustom;

    public KeyStoreSetting(String keyStore, String keyStorePassword, boolean useCustom) {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.useCustom = useCustom;
    }

    /**
     * The default key store file path
     */
    private static Path defaultKeyStorePath() {
        return Settings.getParentPath().resolve(Paths.get("ByProxy.p12"));
    }

    /**
     * The default key store password
     */
    private static String defaultKeyStorePassword() {
        return "123456";
    }

    public String usedKeyStore() {
        if (useCustom) {
            return keyStore;
        }
        return defaultKeyStorePath().toString();
    }

    public String usedPassword() {
        if (useCustom) {
            return keyStorePassword;
        }
        return defaultKeyStorePassword();
    }

    public static KeyStoreSetting getDefault() {
        return new KeyStoreSetting("", "", false);
    }

    public String getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public boolean isUseCustom() {
        return useCustom;
    }
}
