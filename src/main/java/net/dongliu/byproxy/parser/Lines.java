package net.dongliu.byproxy.parser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

public class Lines {

    /**
     * Read ascii line, separated by '\r\n'
     */
    @Nullable
    public static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder(32);
        boolean flag = false;
        while (true) {
            int c = in.read();
            if (c == -1) {
                if (sb.length() == 0) {
                    return null;
                }
                break;
            }
            if (c == '\r') {
                if (flag) {
                    sb.append('\r');
                }
                flag = true;
            } else if (flag) {
                if (c == '\n') {
                    break;
                } else {
                    flag = false;
                    sb.append('\r').append((char) c);
                }
            } else {
                sb.append((char) c);
            }
        }

        if (sb.length() == 0) {
            return "";
        }
        return sb.toString();
    }
}
