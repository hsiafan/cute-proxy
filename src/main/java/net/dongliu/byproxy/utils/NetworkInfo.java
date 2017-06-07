package net.dongliu.byproxy.utils;


import net.dongliu.commons.reflect.Beans;

/**
 * @author Liu Dong
 */
public class NetworkInfo {
    private String name;
    private String ip;

    public NetworkInfo(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return Beans.toString(this);
    }
}
