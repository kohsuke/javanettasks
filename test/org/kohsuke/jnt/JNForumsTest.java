package org.kohsuke.jnt;

import junit.textui.TestRunner;

import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNForumsTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNForumsTest.class);
    }

    /**
     * Tests properties of {@link JNForum}.
     */
    public void test1() throws ProcessingException {
        JNProject project = con.getProject("java-net");
        JNForums forums = project.getForums();

        assertEquals(9,forums.getForums().size());

        JNForum forum = forums.getForum("Wish List");
        assertNotNull(forum);
        assertTrue(forum.getMessageCount()>200);
        assertEquals("This is a place for community member to discuss features they would like to see on java.net",forum.getDescription());
        assertEquals(93,forum.getId());
    }

    /**
     * Test to create a new forum and then delete.
     */
    public void test2() throws ProcessingException {
        JNProject project = con.getProject("javanettasks-test");
        JNForums forums = project.getForums();
        assertEquals(0,forums.getForums().size());
        String description = "forum for testing";
        JNForum forum = forums.createForum("unittest",description);

        assertEquals(1,forums.getForums().size());
        assertTrue(forums.getForums().contains(forum));

        // check the properties of the new forum
        assertEquals(0,forum.getMessageCount());
        assertEquals(description,forum.getDescription());

        forum.delete();
    }
}
