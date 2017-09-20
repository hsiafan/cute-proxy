package net.dongliu.byproxy.ui.beautifier;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Format www-form-encoded content text
 *
 * @author Liu Dong
 */
public class FormEncodedBeautifier implements Beautifier {

    public FormEncodedBeautifier() {
    }

    @Override
    public String beautify(String s, Charset charset) {
        if (s.isEmpty()) {
            return s;
        }
        String[] items = s.split("&");
        List<String> lines = new ArrayList<>(items.length);
        for (String item : items) {
            int idx = item.indexOf('=');
            String name, value;
            if (idx < 0) {
                lines.add(decode(item, charset));
            } else {
                name = item.substring(0, idx);
                value = item.substring(idx + 1);
                lines.add(decode(name, charset) + "=" + decode(value, charset));
            }
        }
        return String.join("\n", lines);
    }

    private String decode(String item, Charset charset) {
        try {
            return URLDecoder.decode(item, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
