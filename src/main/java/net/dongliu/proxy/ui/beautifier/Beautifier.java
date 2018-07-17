package net.dongliu.proxy.ui.beautifier;

import net.dongliu.proxy.store.BodyType;

import java.nio.charset.Charset;

/**
 * Format http textual content.
 *
 * @author Liu Dong
 */
public interface Beautifier {

    /**
     * If this beautifier can handle this body type
     *
     * @param type the body type
     */
    boolean accept(BodyType type);

    /**
     * format th content
     *
     * @param content the http body content
     * @param charset the charset of http body. now only for form-encoded content
     * @return the formatted text
     */
    String beautify(String content, Charset charset);
}
