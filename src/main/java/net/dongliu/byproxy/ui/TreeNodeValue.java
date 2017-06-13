package net.dongliu.byproxy.ui;

import lombok.Getter;

import java.io.Serializable;

/**
 * Tree tree view non-leaf node
 *
 * @author Liu Dong
 */
public class TreeNodeValue implements ItemValue, Serializable {

    private static final long serialVersionUID = -3691517330281517901L;
    @Getter
    private final String pattern;
    @Getter
    private int count;

    public TreeNodeValue(String pattern) {
        this.pattern = pattern;
    }

    public void increaseChildren() {
        count++;
    }

    @Override
    public String displayText() {
        return pattern;
    }
}
