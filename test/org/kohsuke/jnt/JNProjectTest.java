package org.kohsuke.jnt;

import junit.textui.TestRunner;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNProjectTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNProjectTest.class);
    }

//    public void test1() throws ProcessingException {
//        JNProject p = con.getProject("javanettasks-test");
//        assertEquals("javanettasks-test",p.getName());
//        assertEquals(con.getProject("javatools"),p.getOwnerCommunity());
//        assertEquals(con.getProject("javanettasks"),p.getParent());
//        assertEquals("https://javanettasks-test.dev.java.net",p.getURL());
//    }

    public void test2() throws ProcessingException {
        JNMembership mems = con.getProject("javanettasks-test").getMembership();
        mems.grantRole(con.getUser("kohsuke_agent"),"Observer");
        mems.revokeRole(con.getUser("kohsuke_agent"),"Observer");
    }

    public void test3() throws ProcessingException {
        assertTrue(
            con.getProject("javanettasks").getSubProjects().contains(con.getProject("javanettasks-test")));
    }
}
