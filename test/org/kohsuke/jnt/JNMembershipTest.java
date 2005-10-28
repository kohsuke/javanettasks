package org.kohsuke.jnt;

import junit.textui.TestRunner;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNMembershipTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNMembershipTest.class);
    }

    /**
     * Makes sure that we get a reasonable error for choosing non-existent project
     */
    public void test1() throws ProcessingException {
        JNProject project = con.getProject("nosuchproject");
        try {
            project.getMembership().grantRole(con.getUser("kohsuke"),"Observer");
            fail();
        } catch (ProcessingException e) {
            assertTrue(e.getMessage().contains("nosuchproject"));
        }
    }

}
