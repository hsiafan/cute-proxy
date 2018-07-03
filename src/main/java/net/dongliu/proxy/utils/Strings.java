package net.dongliu.proxy.utils;

public class Strings {

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

    public static final long KB = 1024;
    public static final long MB = KB * 1024;
    public static final long GB = MB * 1024;
    public static final long TB = GB * 1024;
    public static final long PB = TB * 1024;

    public static String humanReadableSize(long size) {
        if (size < 0) {
            return String.valueOf(size);
        }
        if (size < KB) {
            return size + " Bytes";
        }
        if (size < MB) {
            return String.format("%.2f KB", size / (double) KB);
        }
        if (size < GB) {
            return String.format("%.2f MB", size / (double) MB);
        }
        if (size < TB) {
            return String.format("%.2f GB", size / (double) GB);
        }
        if (size < PB) {
            return String.format("%.2f TB", size / (double) TB);
        }
        return String.format("%.2f PB", size / (double) PB);
    }
}
