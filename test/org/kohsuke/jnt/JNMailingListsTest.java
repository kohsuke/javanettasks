package org.kohsuke.jnt;

import static org.kohsuke.jnt.SubscriptionMode.NORMAL;
import junit.textui.TestRunner;

import java.util.Collections;

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

        assertEquals(4,lists.getLists().size());

        JNMailingList list1 = lists.getLists().get(0);
        assertEquals("cvs",list1.getName());
        assertSame(list1.getProject(),project);
    }

    /**
     * Test to create a new list and then delete.
     */
    public void test2() throws ProcessingException {
        JNProject project = con.getProject("javanettasks-test");
        JNMailingLists lists = project.getMailingLists();
        JNMailingList l = lists.create("unittest","test mailing list","kohsuke@dev.java.net","prefix",true,true,ListType.MODERATED,
                    Collections.singletonList("kk@kohsuke.org"),
                    Collections.singletonList("kohsuke@dev.java.net"));
        assertNotNull(l);
        assertEquals("unittest",l.getName());

        assertEquals(2,l.getSubscribers(NORMAL).size());
        l.massSubscribe(new String[]{"foo@bar.com","bar@foo.com"},NORMAL,null);
        assertEquals(4,l.getSubscribers(NORMAL).size());
        l.massUnsubscribe(new String[]{"foo@bar.com","bar@foo.com"},NORMAL,null);
        assertEquals(2,l.getSubscribers(NORMAL).size());

        l.delete();
    }
}
