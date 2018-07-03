package net.dongliu.proxy.ui.beautifier;

import java.nio.charset.Charset;

/**
 * @author Liu Dong
 */
public interface Beautifier {

    /**
     * @param content
     * @param charset only for form-encoded content
     * @return
     */
    String beautify(String content, Charset charset);
}
