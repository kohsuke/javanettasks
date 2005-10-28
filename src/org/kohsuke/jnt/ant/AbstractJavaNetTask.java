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

    /** Number of retries. */
    protected int retry = 1;

    public void setUserName( String value ) {
        this.userName = value;
    }
    public void setPassword( String value ) {
        this.password = value;
    }
    public void setRetry(int retry) {
        this.retry = retry;
    }

    public void execute() throws BuildException {
        int count = 0;

        while(true) {
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
                return;
            } catch( ProcessingException e ) {
                BuildException err = new BuildException(e);
                count++;
                if(count<retry) {
                    err.printStackTrace();
                    // retry
                } else {
                    throw err;
                }
            }
        }

    }
    
    /**
     * Executes the task with the specified connection.
     */
    abstract protected void run( JavaNet cmd ) throws ProcessingException, BuildException;
}
