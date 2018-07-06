package net.dongliu.proxy.utils;

import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Lists;
import net.dongliu.proxy.data.NameValue;

import java.util.List;

public class Texts {

    /**
     * key are ascii chars
     */
    public static List<String> toAlignText(List<? extends NameValue> nameValues, String sep) {
        int maxLen = nameValues.stream().mapToInt(n -> n.name().length()).max().orElse(0);
        return Lists.convert(nameValues, nv -> {
            String padding = Strings.repeat(" ", maxLen - nv.name().length());
            return padding + nv.name() + sep + nv.value();
        });
    }
}
