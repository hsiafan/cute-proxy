package net.dongliu.proxy.ui.beautifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dongliu.proxy.store.BodyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author Liu Dong
 */
public class JsonBeautifier implements Beautifier {
    private static final Logger logger = LoggerFactory.getLogger(JsonBeautifier.class);

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public JsonBeautifier() {
    }

    @Override
    public boolean accept(BodyType type) {
        return type == BodyType.json;
    }

    @Override
    public String beautify(String s, Charset charset) {
        try {
            JsonElement jsonElement = new JsonParser().parse(s);
            return gson.toJson(jsonElement);
        } catch (Exception e) {
            logger.debug("", e);
            return s;
        }
    }
}
