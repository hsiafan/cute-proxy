package net.dongliu.proxy.ui.component;

import java.io.Serializable;
import java.util.Set;

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

    // top domains, and not country
    private static final Set<String> topDomains = Set.of("com", "net", "org", "co");

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

        String[] patternItems = pattern.split("\\.");
        String[] domainItems = domain.split("\\.");
        int size = Math.min(patternItems.length, domainItems.length);
        int length = 0;
        int topNum = 0;
        for (int i = 0; i < size; i++) {
            String patternItem = patternItems[patternItems.length - i - 1];
            String domainItem = domainItems[domainItems.length - i - 1];
            if (!patternItem.equals(domainItem)) {
                if (i - topNum < 2) {
                    return MISS;
                }
                return length - 1;
            }
            length += patternItem.length() + 1;
            if (i == 1) {
                if (topDomains.contains(patternItem)) {
                    topNum = 1;
                }
            }
        }


        if (patternItems.length == size) {
            return IS_SUB;
        }

        return IS_SUPER;
    }

    @Override
    public String toString() {
        return "TreeNode{domain='" + pattern + '\'' + '}';
    }
}
