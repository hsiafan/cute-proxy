package net.dongliu.proxy.data;

import java.time.Instant;
import java.util.Objects;

public class Cookie implements KeyValue {
    private final String domain;
    private final String path;
    private final String name;
    private final String value;
    private final Instant expiry;
    private final boolean secure;

    public Cookie(String domain, String path, String name, String value, Instant expiry, boolean secure) {
        this.domain = Objects.requireNonNull(domain);
        this.path = Objects.requireNonNull(path);
        this.name = Objects.requireNonNull(name);
        this.value = Objects.requireNonNull(value);
        this.expiry = expiry;
        this.secure = secure;
    }

    public boolean expired(Instant now) {
        return expiry != null && expiry.isBefore(now);
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public boolean isSecure() {
        return secure;
    }
}
