package net.dongliu.proxy.utils;

import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Lists;
import net.dongliu.proxy.data.NameValue;

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
            String padding = Strings.repeat(" ", paddingCount);
            return nv.name() + padding + sep + nv.value();
        });
    }
}
