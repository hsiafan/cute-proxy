package net.dongliu.byproxy.utils;

public class StringUtils {

    public static String before(String str, String sep) {
        int idx = str.indexOf(sep);
        if (idx > 0) {
            return str.substring(0, idx);
        }
        return str;
    }

    public static String beforeLast(String str, String sep) {
        int idx = str.lastIndexOf(sep);
        if (idx > 0) {
            return str.substring(0, idx);
        }
        return str;
    }

    public static String afterLast(String str, String sep) {
        int idx = str.lastIndexOf(sep);
        if (idx > 0) {
            return str.substring(idx + sep.length());
        }
        return str;
    }

    public static int toInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
