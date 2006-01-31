package org.kohsuke.jnt.ant;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.JNRole;
import org.apache.tools.ant.BuildException;

import java.util.Set;

/**
 * Checks if an user belongs to a project.
 *
 * @author Kohsuke Kawaguchi
 */
public class CheckMemberTask extends AbstractJavaNetTaskForProject {
    private String role;
    private String user;
    private String property;

    public void setRole(String role) {
        this.role = role;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    protected void run(JNProject project) throws ProcessingException, BuildException {
        JavaNet conn = project.getConnection();
        Set<JNRole> roles = project.getMembership().getRolesOf(conn.getUser(user));

        boolean r;
        if(role!=null)
            r = roles.contains(conn.getRole(role));
        else
            r = !roles.isEmpty();

        if(r) {
            getProject().setProperty(property, "true" );
        }
    }
}
