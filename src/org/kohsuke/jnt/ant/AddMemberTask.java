package org.kohsuke.jnt.ant;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.jnt.JavaNet;
import org.apache.tools.ant.BuildException;

/**
 * Adds an user to a project.
 *
 * @author Kohsuke Kawaguchi
 */
public class AddMemberTask extends AbstractJavaNetTaskForProject {
    private String role;

    private String user;

    public void setRole(String role) {
        this.role = role;
    }

    public void setUser(String user) {
        this.user = user;
    }

    protected void run(JNProject project) throws ProcessingException, BuildException {
        JavaNet conn = project.getConnection();
        project.getMembership().grantRole(conn.getUser(user),role);
    }
}
