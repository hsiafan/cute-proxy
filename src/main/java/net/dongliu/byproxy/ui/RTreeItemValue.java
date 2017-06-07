package net.dongliu.byproxy.ui;

import net.dongliu.byproxy.parser.Message;

/**
 * @author Liu Dong
 */
public abstract class RTreeItemValue {

    public static class Leaf extends RTreeItemValue {

        private final Message message;

        public Leaf(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return this.message;
        }

    }

    public static class Node extends RTreeItemValue {
        private final String pattern;
        private int count;

        public Node(String pattern) {
            this.pattern = pattern;
        }

        public void increaseChildren() {
            count++;
        }

        public String getPattern() {
            return pattern;
        }

        public int getCount() {
            return count;
        }
    }

}
