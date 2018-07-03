package net.dongliu.proxy.utils;

import java.io.IOException;
import java.io.Reader;

public class Readers {

    public static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4 * 1024];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();

    }
}
