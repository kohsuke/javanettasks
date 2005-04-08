package org.kohsuke.jnt;

import junit.textui.TestRunner;

import java.util.GregorianCalendar;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNIssueTrackerTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNIssueTrackerTest.class);
    }

    public void test1() throws ProcessingException {
        JNProject project = con.getProject("jaxb");
        JNIssueTracker it = project.getIssueTracker();

        // test single fetch
        JNIssue issue1 = it.get(2);
        assertNotNull(issue1);
        assertEquals(2,issue1.getId());
        assertEquals(Priority.P3,issue1.getPriority());

        // test bulk fetch
        Map<Integer,JNIssue> map = it.get(1,3);
        assertEquals(2,map.size());
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(3));

        // test update listing
        map = it.getUpdatedIssues(new GregorianCalendar(2005,02,01),new GregorianCalendar(2005,02,07));
        System.out.println(map.size());
        assertEquals(1,map.size());
        assertTrue(map.containsKey(41));
    }

}
