package net.dongliu.proxy.setting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hold default settings.
 *
 * @author Liu Dong
 */
public class Settings {

    public static final int rootCertValidityDays = 3650;

    public static final int certValidityDays = 10;
    public static final char[] rootKeyStorePassword = "123456".toCharArray();
    public static final char[] keyStorePassword = "123456".toCharArray();

    /**
     * alias name for self assigned cert
     */
    public static final String certAliasName = "CuteProxy app";

    private static final Path parentPath = Paths.get(System.getProperty("user.home"), ".CuteProxy");

    public static Path getParentPath() {
        if (!Files.exists(parentPath)) {
            try {
                Files.createDirectory(parentPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return parentPath;

    }
}
