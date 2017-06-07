package net.dongliu.byproxy.utils;

import net.dongliu.commons.Strings;
import net.dongliu.commons.collection.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utils methods for deal with network
 *
 * @author Liu Dong
 */
public class NetUtils {

    private static Logger logger = LoggerFactory.getLogger(NetUtils.class);

    public static final int HOST_TYPE_IPV6 = 0;
    public static final int HOST_TYPE_IPV4 = 1;
    public static final int HOST_TYPE_DOMAIN = 2;

    /**
     * Ipv4, ipv6, or domain
     */
    public static int getHostType(String host) {
        if (host.contains(":") && !host.contains(".")) {
            return HOST_TYPE_IPV6;
        }
        if (host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
            return HOST_TYPE_IPV4;
        }
        return HOST_TYPE_DOMAIN;
    }

    public static boolean isIp(String host) {
        int type = getHostType(host);
        return type == HOST_TYPE_IPV4 || type == HOST_TYPE_IPV6;
    }

    public static boolean isDomain(String host) {
        int type = getHostType(host);
        return type == HOST_TYPE_DOMAIN;
    }


    public static String getHost(String target) {
        return Strings.before(target, ":");
    }

    public static int getPort(String target) {
        int idx = target.indexOf(":");
        if (idx > 0) {
            return Integer.parseInt(target.substring(idx + 1));
        }
        throw new RuntimeException("Target has no port: " + target);
    }


    @Nonnull
    public static List<NetworkInfo> getNetworkInfoList() {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.warn("cannot get local network interface ip address", e);
            return Lists.of();
        }
        List<NetworkInfo> list = new ArrayList<>();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            try {
                if (!networkInterface.isUp() || networkInterface.isPointToPoint()) {
                    continue;
                }
            } catch (SocketException e) {
                logger.warn("", e);
                continue;
            }
            String name = networkInterface.getName();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress instanceof Inet4Address) {
                    String ip = inetAddress.getHostAddress();
                    list.add(new NetworkInfo(name, ip));
                } else if (inetAddress instanceof Inet6Address) {
                    String ip = inetAddress.getHostAddress();
                    list.add(new NetworkInfo(name, ip));
                }
            }
        }
        return list;
    }


    /**
     * For uniq multi cdns, only with different index.
     * img1.fbcdn.com -> img*.fbcdn.com
     */
    public static String genericMultiCDNS(String host) {
        int idx = host.indexOf(".");
        if (idx < 2) {
            return host;
        }
        String first = host.substring(0, idx);
        if (!Character.isLetter(first.charAt(0))) {
            return host;
        }
        char c = first.charAt(first.length() - 1);
        if (!Character.isDigit(c)) {
            return host;
        }
        int firstEnd = first.length() - 2;
        while (Character.isDigit(first.charAt(firstEnd))) {
            firstEnd--;
        }
        return first.substring(0, firstEnd + 1) + "*." + host.substring(idx + 1);
    }

}
