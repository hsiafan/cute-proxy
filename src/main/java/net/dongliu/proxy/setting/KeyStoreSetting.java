package net.dongliu.proxy.setting;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

/**
 * Key store setting
 *
 * @author Liu Dong
 */
public class KeyStoreSetting implements Serializable {
    private static final long serialVersionUID = -8001899659204205513L;
    // path for keyStore file
    private String keyStore;
    //TODO: should use char[] to store password?
    private String keyStorePassword;
    private boolean useCustom;

    public KeyStoreSetting(String keyStore, String keyStorePassword, boolean useCustom) {
        this.keyStore = requireNonNull(keyStore);
        this.keyStorePassword = requireNonNull(keyStorePassword);
        this.useCustom = useCustom;
    }

    /**
     * The default key store file path
     */
    private static Path defaultKeyStorePath() {
        return Settings.getParentPath().resolve(Paths.get("CuteProxy.p12"));
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

    public static KeyStoreSetting newDefaultKeyStoreSetting() {
        return new KeyStoreSetting("", "", false);
    }

    public String keyStore() {
        return keyStore;
    }

    public String keyStorePassword() {
        return keyStorePassword;
    }

    public boolean useCustom() {
        return useCustom;
    }
}
