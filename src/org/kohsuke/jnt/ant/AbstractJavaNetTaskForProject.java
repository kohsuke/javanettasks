package org.kohsuke.jnt.ant;

import org.apache.tools.ant.BuildException;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

/**
 * Ant task that works on a java&#x2E;project.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class AbstractJavaNetTaskForProject extends AbstractJavaNetTask {
    
    /** Name of the project. Such as "java-ws-xml" or "msv". */
    private String projectName;
    
    public void setProjectName( String value ) {
        this.projectName = value;
    }

    protected final void run(JavaNet cmd) throws ProcessingException, BuildException {
        // mandatory attributes check
        if( projectName==null )
            throw new BuildException("project name is not specified");
        
        run(cmd.getProject(projectName));
    }

    protected abstract void run(JNProject project) throws ProcessingException, BuildException;
}
