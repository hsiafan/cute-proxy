package net.dongliu.proxy.ui.beautifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.nio.charset.Charset;

/**
 * @author Liu Dong
 */
public class JsonBeautifier implements Beautifier {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public JsonBeautifier() {
    }

    @Override
    public String beautify(String s, Charset charset) {
        JsonElement jsonElement = new JsonParser().parse(s);
        return gson.toJson(jsonElement);
    }
}
