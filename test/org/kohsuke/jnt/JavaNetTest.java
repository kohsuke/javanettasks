package org.kohsuke.jnt;

/**
 * @author Kohsuke Kawaguchi
 */
public class JavaNetTest extends TestCaseBase {
    public void testLoggedIn() throws Exception {
        assertTrue(con.isLoggedIn());
        assertFalse(JavaNet.connectAnonymously().isLoggedIn());
    }
}
