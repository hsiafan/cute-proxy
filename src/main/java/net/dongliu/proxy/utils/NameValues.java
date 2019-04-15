package net.dongliu.proxy.utils;

import net.dongliu.commons.collection.Lists;
import net.dongliu.proxy.data.NameValue;
import net.dongliu.proxy.data.Parameter;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class NameValues {

    /**
     * key are ascii chars
     */
    public static List<String> toAlignText(List<? extends NameValue> nameValues, String sep) {
        int maxLen = nameValues.stream().mapToInt(n -> n.name().length()).max().orElse(0);
        int paddingLen = Math.min(maxLen, 20);
        return Lists.convert(nameValues, nv -> {
            int paddingCount = Math.max(paddingLen - nv.name().length(), 0);
            String padding = " ".repeat(paddingCount);
            return nv.name() + padding + sep + nv.value();
        });
    }


    /**
     * Parse url encoded key values.
     */
    public static List<? extends NameValue> parseUrlEncodedParams(String text, Charset charset) {
        if (text.isEmpty()) {
            return List.of();
        }
        var params = new ArrayList<Parameter>();
        for (String segment : text.split("&")) {
            segment = segment.trim();
            if (segment.isEmpty()) {
                continue;
            }
            int idx = segment.indexOf("=");
            if (idx >= 0) {
                String name = segment.substring(0, idx).trim();
                String value = segment.substring(idx + 1).trim();
                value = URLDecoder.decode(value, charset);
                name = URLDecoder.decode(name, charset);
                params.add(new Parameter(name, value));
            } else {
                String value = URLDecoder.decode(segment, charset);
                params.add(new Parameter("", value));
            }
        }
        return params;
    }
}
