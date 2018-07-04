package net.dongliu.proxy.data;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import static net.dongliu.commons.Strings.nullToEmpty;

public class CookieUtils {


    public static Cookie parseCookieHeader(String headerValue) {
        String[] items = headerValue.split("; ");
        var nameValue = parseCookieNameValue(items[0]);

        String domain = null;
        String path = null;
        Instant expiry = null;
        boolean secure = false;
        for (int i = 1; i < items.length - 1; i++) {
            var attribute = parseCookieAttribute(items[i]);
            switch (attribute.name().toLowerCase()) {
                case "domain":
                    domain = attribute.value();
                    break;
                case "path":
                    path = attribute.value();
                    break;
                case "expires":
                    try {
                        expiry = DateTimeFormatter.RFC_1123_DATE_TIME.parse(attribute.value(), Instant::from);
                    } catch (DateTimeParseException ignore) {
                        //TODO: we should ignore this cookie?
                    }
                    break;
                case "max-age":
                    try {
                        int seconds = Integer.parseInt(attribute.value());
                        if (seconds >= 0) {
                            expiry = Instant.now().plusSeconds(seconds);
                        }
                    } catch (NumberFormatException ignore) {
                        //TODO: we should ignore this cookie?
                    }
                    break;
                case "secure":
                    secure = true;
                    break;
                case "httponly":
                    // ignore http only
                    break;
                default:
            }
        }

        return new Cookie(nullToEmpty(domain), nullToEmpty(path),
                nameValue.name(), nameValue.value(), Optional.ofNullable(expiry), secure);
    }

    private static NameValue parseCookieNameValue(String str) {
        // Browsers always split the name and value on the first = symbol in the string
        int idx = str.indexOf("=");
        if (idx < 0) {
            // If there is no = symbol in the string at all, browsers treat it as the cookie with the empty-string name
            return new Header("", str);
        } else {
            return new Header(str.substring(0, idx), str.substring(idx + 1));
        }
    }

    private static NameValue parseCookieAttribute(String str) {
        int idx = str.indexOf("=");
        if (idx < 0) {
            return new Header(str, "");
        } else {
            return new Header(str.substring(0, idx), str.substring(idx + 1));
        }
    }
}
