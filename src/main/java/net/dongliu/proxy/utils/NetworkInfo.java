package net.dongliu.proxy.utils;


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
        return "NetworkInfo{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
