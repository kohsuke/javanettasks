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

        assertEquals(6,lists.getLists().size());

        JNMailingList list1 = lists.getLists().get(0);
        assertEquals("dev",list1.getName());
        assertSame(list1.getProject(),project);
    }

    /**
     * Test to create a new list and then delete.
     */
    public void test2() throws ProcessingException {
        JNProject project = con.getProject("javanettasks-test");
        JNMailingLists lists = project.getMailingLists();

        // make sure there's no left over from the previous test
        if(lists.get("unittest")!=null)
            lists.get("unittest").delete();

        JNMailingList l = lists.create("unittest","test mailing list","kohsuke@dev.java.net","prefix",true,true,ListType.MODERATED,
                    Collections.singletonList("kk@kohsuke.org"),
                    Collections.singletonList("kohsuke@dev.java.net"));
        assertNotNull(l);
        assertEquals("unittest",l.getName());

        assertEquals(1,l.getSubscribers(NORMAL).size());
        l.massSubscribe(new String[]{"foo@bar.com","bar@foo.com"},NORMAL,null);
        assertEquals(3,l.getSubscribers(NORMAL).size());
        l.massUnsubscribe(new String[]{"foo@bar.com","bar@foo.com"},NORMAL,null);
        assertEquals(1,l.getSubscribers(NORMAL).size());

        l.delete();
    }
}
