package net.dongliu.byproxy.setting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Liu Dong
 */
public class Settings {

    public static final int rootCertificateValidates = 3650;

    private static final Path parentPath = Paths.get(System.getProperty("user.home"), ".ByProxy");

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
