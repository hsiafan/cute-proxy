package net.dongliu.byproxy.utils;


import lombok.ToString;

@ToString
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

}
