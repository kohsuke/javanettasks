package org.kohsuke.jnt;


import java.util.Set;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNMyselfTest extends TestCaseBase {

    public void test1() throws ProcessingException {
        JNMyself myself = con.getMyself();
        assertEquals("javanettaskstest@dev.java.net",myself.getEmailAddress());
        assertEquals("javanettaskstest",myself.getName());

        Set projects = myself.getMyProjects();
        assertEquals(1,projects.size());
        assertTrue(projects.contains(con.getProject("javanettasks-test")));
    }
}
