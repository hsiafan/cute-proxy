package net.dongliu.byproxy.parser;

import net.dongliu.byproxy.exception.HttpParserException;

import java.io.Serializable;
import java.util.Objects;

/**
 * The http status line
 *
 * @author Liu Dong
 */
public class StatusLine implements Serializable {
    private static final long serialVersionUID = -4100839989293320399L;
    private final String version;
    private final int code;
    private final String reason;
    private transient String raw;

    public StatusLine(String version, int code, String reason) {
        this.version = version;
        this.code = code;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return String.format("StatusLine(%s %d %s)", version, code, reason);
    }

    public static StatusLine parse(String str) {
        Objects.requireNonNull(str);
        int idx = str.indexOf(' ');
        if (idx < 0) {
            throw new HttpParserException("Invalid http status line: " + str);
        }
        String version = str.substring(0, idx);
        int code;
        String msg;
        int idx2 = str.indexOf(' ', idx + 1);
        if (idx2 < 0) {
            code = Integer.parseInt(str.substring(idx + 1));
            msg = "";
        } else {
            code = Integer.parseInt(str.substring(idx + 1, idx2));
            msg = str.substring(idx2 + 1);
        }

        StatusLine statusLine = new StatusLine(version, code, msg);
        statusLine.raw = str;
        return statusLine;
    }

    public String getVersion() {
        return version;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public String raw() {
        if (raw == null) {
            raw = version + " " + code + " " + reason;
        }
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatusLine that = (StatusLine) o;

        if (code != that.code) return false;
        if (!version.equals(that.version)) return false;
        return reason.equals(that.reason);
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + code;
        result = 31 * result + reason.hashCode();
        return result;
    }
}
