package net.dongliu.proxy.ui.beautifier;

import net.dongliu.proxy.store.BodyType;
import net.dongliu.proxy.utils.NameValues;

import java.nio.charset.Charset;

import static net.dongliu.proxy.utils.NameValues.parseUrlEncodedParams;

/**
 * Format www-form-encoded content text
 *
 * @author Liu Dong
 */
public class FormEncodedBeautifier implements Beautifier {

    @Override
    public boolean accept(BodyType type) {
        return type == BodyType.www_form;
    }

    @Override
    public String beautify(String s, Charset charset) {
        if (s.isEmpty()) {
            return s;
        }
        var nameValues = parseUrlEncodedParams(s, charset);
        return String.join("\n", NameValues.toAlignText(nameValues, " = "));
    }
}
