package net.dongliu.proxy.ui.beautifier;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

/**
 * @author Liu Dong
 */
public class XMLBeautifierTest {
    @Test
    public void apply() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><tag><nested>hello</nested></tag>";
        String formatted = new XMLBeautifier().beautify(xml, StandardCharsets.UTF_8);
        assertTrue(formatted.split("\n").length >= 4);
    }

}