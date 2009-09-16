package org.kohsuke.jnt;

/**
 * @author Kohsuke Kawaguchi
 */
public class JavaNetTest extends TestCaseBase {
    public void testLoggedIn() throws Exception {
        assertTrue(con.isLoggedIn());
        assertFalse(JavaNet.connectAnonymously().isLoggedIn());
    }

    public void testCopy() throws Exception {
        JNMyself me = con.getMyself();
        JavaNet copy = con.copy();

        assertNotSame(me, copy.getMyself());
        assertEquals(me.getName(), copy.getMyself().getName());
        assertTrue(copy.isLoggedIn());
    }
}
