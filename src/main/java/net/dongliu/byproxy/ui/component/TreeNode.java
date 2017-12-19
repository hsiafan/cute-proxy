package net.dongliu.byproxy.ui.component;

import java.io.Serializable;

/**
 * Tree tree view non-leaf node
 *
 * @author Liu Dong
 */
class TreeNode extends Item implements Serializable {

    private static final long serialVersionUID = -3691517330281517901L;
    private final String pattern;
    private int count;

    public TreeNode(String pattern) {
        this.pattern = pattern;
    }

    public void increaseChildren() {
        count++;
    }

    @Override
    public String displayText() {
        return pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public int getCount() {
        return count;
    }


    // domain equals pattern
    static final int EQUAL = Integer.MAX_VALUE;
    // means domain is super domain of host
    static final int IS_SUPER = Integer.MAX_VALUE - 1;
    // means domain is sub domain of pattern
    static final int IS_SUB = Integer.MAX_VALUE - 2;
    static final int MISS = 0;

    /**
     * MISS means not share common part;
     * EQUAL means equals;
     * IS_SUB means domain is sub domain of pattern
     * IS_SUPER means pattern is sub domain of host
     * larger than 0 means common suffix len
     */
    public int match(String domain) {
        if (pattern.equals(domain)) {
            return EQUAL;
        }

        int parts = 0;
        int pos = 0;
        int i = 0;
        for (; i < Math.min(domain.length(), pattern.length()); i++) {
            if (pattern.charAt(pattern.length() - i - 1) != domain.charAt(domain.length() - i - 1)) {
                break;
            }
            if (pattern.charAt(pattern.length() - i - 1) == '.') {
                pos = i;
                parts++;
            }
        }

        if (i == pattern.length() && domain.length() > i && domain.charAt(domain.length() - i - 1) == '.') {
            return IS_SUB;
        }
        if (i == domain.length() && pattern.length() > i && pattern.charAt(pattern.length() - i - 1) == '.') {
            return IS_SUPER;
        }

        if (parts >= 2) {
            return pos;
        }


        return MISS;
    }

    @Override
    public String toString() {
        return "TreeNode{domain='" + pattern + '\'' + '}';
    }
}
