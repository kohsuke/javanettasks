package org.kohsuke.jnt;

import junit.textui.TestRunner;

import java.util.List;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNNewsItemsTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNNewsItemsTest.class);
    }

    public void test1() throws ProcessingException {
        JNProject project = con.getProject("javanettasks-test");
        JNNewsItems newsItems = project.getNewsItems();

        for (Iterator itr = newsItems.getNewsItems().iterator(); itr.hasNext();) {
            JNNewsItem item = (JNNewsItem) itr.next();
            item.delete();
        }

        newsItems.createNewsItem("test news item 1");
        try {
            Thread.sleep(3000); // give the server the time to register the old news
        } catch (InterruptedException e) {
            ;
        }
        newsItems.createNewsItem("test news item 2");

        List items = newsItems.getNewsItems();
        assertEquals(2,items.size());

        JNNewsItem ni0 = (JNNewsItem) items.get(0);
        JNNewsItem ni1 = (JNNewsItem) items.get(1);

        assertSame(ni0.getProject(),project);
        assertSame(ni1.getProject(),project);
        assertEquals("test news item 1",ni1.getHeadline());
        assertEquals("test news item 2",ni0.getHeadline());
    }
}
