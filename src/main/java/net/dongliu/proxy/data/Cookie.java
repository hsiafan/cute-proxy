package net.dongliu.proxy.data;

import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Http Cookie
 */
public class Cookie implements NameValue {
    private final String domain;
    private final String path;
    private final String name;
    private final String value;
    private final Optional<Instant> expiry;
    private final boolean secure;

    public Cookie(String domain, String path, String name, String value, Optional<Instant> expiry, boolean secure) {
        this.domain = requireNonNull(domain);
        this.path = requireNonNull(path);
        this.name = requireNonNull(name);
        this.value = requireNonNull(value);
        this.expiry = requireNonNull(expiry);
        this.secure = secure;
    }

    public boolean expired(Instant now) {
        return expiry.isPresent() && expiry.get().isBefore(now);
    }

    public String domain() {
        return domain;
    }

    public String path() {
        return path;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    public Optional<Instant> expiry() {
        return expiry;
    }

    public boolean secure() {
        return secure;
    }
}
