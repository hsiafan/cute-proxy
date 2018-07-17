package net.dongliu.proxy.ui.beautifier;

import net.dongliu.proxy.store.BodyType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.Charset;

/**
 * @author Liu Dong
 */
public class HtmlBeautifier implements Beautifier {
    public HtmlBeautifier() {
    }

    @Override
    public boolean accept(BodyType type) {
        return type == BodyType.html;
    }

    @Override
    public String beautify(String s, Charset charset) {
        Document doc = Jsoup.parse(s);
        doc.outputSettings().indentAmount(4);
        return doc.toString();
    }
}