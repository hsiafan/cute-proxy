package net.dongliu.proxy.data;

import net.dongliu.proxy.exception.HttpDecodeException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * The http request line
 *
 * @author Liu Dong
 */
public class RequestLine implements Serializable {
    private static final long serialVersionUID = -6420118468955265866L;
    private String method;
    private String path;
    private String version;
    private transient String raw;

    public RequestLine(String method, String path, String version) {
        this.method = requireNonNull(method);
        this.path = requireNonNull(path);
        this.version = requireNonNull(version);
    }

    public static RequestLine parse(String str) {
        String[] items = str.split(" ");
        if (items.length != 3) {
            throw new HttpDecodeException("Invalid http request line:" + str);
        }
        RequestLine requestLine = new RequestLine(items[0], items[1], items[2]);
        requestLine.raw = str;
        return requestLine;
    }

    public boolean isHttp10() {
        return "HTTP/1.0".equalsIgnoreCase(version);
    }

    public boolean isHttp11() {
        return "HTTP/1.1".equalsIgnoreCase(version);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(method);
        out.writeUTF(path);
        out.writeUTF(version);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        method = in.readUTF();
        path = in.readUTF();
        version = in.readUTF();
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public String version() {
        return version;
    }

    public String rawRequestLine() {
        if (raw == null) {
            raw = method + " " + path + " " + version;
        }
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestLine that = (RequestLine) o;

        if (!method.equals(that.method)) return false;
        if (!path.equals(that.path)) return false;
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("RequestLine(%s %s %s)", method, path, version);
    }

}
