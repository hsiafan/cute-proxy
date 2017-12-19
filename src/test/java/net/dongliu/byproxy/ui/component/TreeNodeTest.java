package net.dongliu.byproxy.ui.component;

import org.junit.Test;

import static org.junit.Assert.*;

public class TreeNodeTest {

    @Test
    public void match() {

        TreeNode treeNode = new TreeNode("www.baidu.com");
        assertEquals(0, treeNode.match("www.baidu.com"));
        assertEquals(-1, treeNode.match("www.test.com"));
        assertEquals(9, treeNode.match("w.baidu.com"));
        assertEquals(-2, treeNode.match("v2.www.baidu.com"));
        assertEquals(-3, treeNode.match("baidu.com"));
        assertEquals(9, treeNode.match("www2.baidu.com"));
        assertEquals(9, treeNode.match("rm.www2.baidu.com"));

        assertEquals(9, new TreeNode("s.weibo.com").match("rm.api.weibo.com"));
    }
}