package org.kohsuke.jnt;

import junit.textui.TestRunner;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNMailingListsTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNMailingListsTest.class);
    }

    public void test1() throws ProcessingException {
        JNProject project = con.getProject("jaxb");
        JNMailingLists lists = project.getMailingLists();

        assertEquals(5,lists.getLists().size());

        JNMailingList list1 = (JNMailingList) lists.getLists().get(0);
        assertEquals("dev",list1.getName());
        assertSame(list1.getProject(),project);
    }
}
