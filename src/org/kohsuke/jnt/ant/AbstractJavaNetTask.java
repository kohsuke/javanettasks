/*
 * Use is subject to the license terms.
 */
package org.kohsuke.jnt.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

/**
 * Base implementation for Java.net web tasks.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class AbstractJavaNetTask extends Task {
    
    /** Account information. */
    private String userName;
    private String password;
    
    public void setUserName( String value ) {
        this.userName = value;
    }
    public void setPassword( String value ) {
        this.password = value;
    }
    
    public void execute() throws BuildException {
        // do the job
        try {
            JavaNet cmd;
            
            if (userName != null && userName.length()!=0) {
                log("authenticating", Project.MSG_VERBOSE);
                cmd = JavaNet.connect(userName,password);
            } else {
                log("using user default setting", Project.MSG_VERBOSE);
                cmd = JavaNet.connect();
            }
        
            run(cmd);
        } catch( ProcessingException e ) {
            throw new BuildException(e);
        }
    }
    
    /**
     * Executes the task with the specified connection.
     */
    abstract protected void run( JavaNet cmd ) throws ProcessingException, BuildException;
}
