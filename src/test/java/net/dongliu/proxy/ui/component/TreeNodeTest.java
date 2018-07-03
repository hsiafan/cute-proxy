package net.dongliu.proxy.ui.component;

import org.junit.Test;

import static org.junit.Assert.*;

public class TreeNodeTest {

    @Test
    public void match() {

        TreeNode treeNode = new TreeNode("www.baidu.com");
        assertEquals(TreeNode.EQUAL, treeNode.match("www.baidu.com"));
        assertEquals(TreeNode.MISS, treeNode.match("www.test.com"));
        assertEquals(9, treeNode.match("w.baidu.com"));
        assertEquals(TreeNode.IS_SUB, treeNode.match("v2.www.baidu.com"));
        assertEquals(TreeNode.IS_SUPER, treeNode.match("baidu.com"));
        assertEquals(9, treeNode.match("www2.baidu.com"));
        assertEquals(9, treeNode.match("rm.www2.baidu.com"));

        assertEquals(9, new TreeNode("s.weibo.com").match("rm.api.weibo.com"));

        assertEquals(TreeNode.MISS, new TreeNode("v.admaster.com.cn").match("sbeacon.sina.com.cn"));
    }
}