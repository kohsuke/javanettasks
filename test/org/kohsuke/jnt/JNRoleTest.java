package org.kohsuke.jnt;

import junit.textui.TestRunner;

import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNRoleTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNRoleTest.class);
    }

    public void test1() throws ProcessingException {
        JNMembership mems = con.getProject("javanettasks-test").getMembership();
        assertTrue(mems.getMembers().contains( con.getUser("kohsuke") ));
        assertFalse(mems.getMembers().contains( con.getUser("sekhar") ));
        Set roles = mems.getRolesOf(con.getUser("kohsuke"));
        assertEquals(roles.size(),1);
        assertTrue(roles.contains(con.getRole("Project Owner")));
    }
}
