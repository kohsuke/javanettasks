/*
 * Use is subject to the license terms.
 */
package org.kohsuke.jnt.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.jnt.JNFile;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class FileDeleteTask extends AbstractJavaNetTaskForProject {
    
    /** File name to be deleted. Path separators are normalized to '/' */
    private String fileName;
    
    public void setFile( String value ) {
        this.fileName = value;
    }
    
    public void run(JNProject cmd) throws BuildException, ProcessingException {
        
        // mandatory attributes check
        if( fileName==null )
            throw new BuildException("file name is not set");
        
        log("moving to the target folder",Project.MSG_VERBOSE);
                JNFileFolder folder = cmd.getFolder(getTargetFolder());

        JNFile file = folder.getFiles().get(getTargetFileName());

        if( file==null ) {
            log("file "+getTargetFileName()+" doesn't exist",Project.MSG_WARN);
            return;
        }
        
        log("deleting file",Project.MSG_VERBOSE);
        file.delete();
        
        log("done",Project.MSG_VERBOSE);
    }
    
    private String getTargetFolder() {
        int idx = fileName.lastIndexOf('/');
        if(idx==-1)     return "";
        else            return fileName.substring(0,idx);
    }
    
    private String getTargetFileName() {
        int idx = fileName.lastIndexOf('/');
        if(idx==-1)     return fileName;
        else            return fileName.substring(idx+1);
    }
}

