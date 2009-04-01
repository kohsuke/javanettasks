package org.kohsuke.jnt;

import java.util.Set;
import junit.textui.TestRunner;

/**
 * @author psterk
 * Note: a number of unit tests are yet to be implemented
 */
public class JNMembershipTest extends TestCaseBase {
    JNMembership membership;

    public static void main(String[] args) {
        TestRunner.run(JNMembershipTest.class);
    }

    public void setUp() throws ProcessingException {
        super.setUp();
        membership = con.getProject("javanettasks").getMembership();
    }

    public void tearDown() {
        membership = null;
    }

    /**
     * Test of getMembers method, of class JNMembership.
     */
    public void testGetMembers() throws ProcessingException {
        assertNotNull(membership.getMembers());
    }

    /**
     * Test of getRoles method, of class JNMembership.
     */
    public void testGetRoles() throws ProcessingException {
        assertNotNull(membership.getRoles());
    }

    /**
     * Check that the roles for each user look reasonble
     */
    public void testGetRolesForEachUser() throws ProcessingException {
        Set<JNUser> users = membership.getMembers();
        for (JNUser user : users) {
            Set<JNRole> roles = membership.getRolesOf(user);
            for (JNRole role : roles) {
                String roleName = role.getName();
                assertTrue("There should not be any newlines in the role name: "+roleName,
                           roleName.indexOf('\n')== -1?true:false);
            }
        }
    }

    /**
     * This test checks that a set of users can be retrieved based on a
     * specific role. In this case, it is 'Developer' role.
     */
    public void testGetDeveloperRoleForEachUser() throws ProcessingException {
        Set<JNUser> users = membership.getUserOf(con.getRole("Developer"));
        // There should be at least 1 user that has the Developer role
        assertNotNull(users);
    }
}
