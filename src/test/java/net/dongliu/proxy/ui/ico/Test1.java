package net.dongliu.proxy.ui.ico;

import java.io.IOException;

public class Test1 {
    public static void main(String[] args) throws IOException {
        try (var in = Test1.class.getResourceAsStream("/icons/weibo.ico")) {
            IconDecoders.decode(in.readAllBytes());
        }
    }
}
