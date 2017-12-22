package net.dongliu.byproxy.data;

import java.util.Map;

/**
 * @author Liu Dong
 */
public interface KeyValue extends Map.Entry<String, String> {
    @Override
    default String setValue(String value) {
        throw new UnsupportedOperationException();
    }

    String getName();

    @Override
    default String getKey() {
        return getName();
    }
}
