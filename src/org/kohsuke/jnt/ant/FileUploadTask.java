/*
 * Use is subject to the license terms.
 */
package org.kohsuke.jnt.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.kohsuke.jnt.FileStatus;
import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;

import java.io.File;

/**
 * Uploads a file to a java.net file sharing section. 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class FileUploadTask extends AbstractJavaNetTaskForProject {
    
    /** File to be uploaded. */
    private File fromFile;
    
    /** File name to be written. Path separators are normalized to '/' */
    private String toFileName;
    
    /** Status of the file. */
    private FileStatus fileStatus;
    
    /** File description. */
    private String fileDescription;
    
    /** Overwrite if a file already exists? */
    private boolean overwrite;
    
    public void setFromFile( File value ) {
        this.fromFile = value;
    }
    public void setToFile( String value ) {
        this.toFileName = value.replace('\\','/');
    }
    
    public void setFileStatus( String value ) {
        this.fileStatus = FileStatus.parse(value);
    }
    public void setFileDescription( String value ) {
        this.fileDescription = value;
    }
    public void setOverwrite( boolean value ) {
        this.overwrite = value;
    }
    
    public void run(JNProject cmd) throws BuildException, ProcessingException {
        
        // mandatory attributes check
        if( toFileName==null )
            throw new BuildException("target folder is not specified");
        if( fromFile==null )
            throw new BuildException("source file is not specified");
        if( !fromFile.canRead() )
            throw new BuildException(fromFile+" cannot be read");
            
        log("moving to the target folder",Project.MSG_VERBOSE);
        JNFileFolder folder = cmd.getFolder(getTargetFolder());

        JNFile file = folder.getFiles().get(getTargetFileName());

        if( file!=null ) {
            if( overwrite ) {
                log("deleting existing file '"+toFileName+"' first", Project.MSG_INFO);
                file.delete();
            } else {
                log("file '"+toFileName+"' already exists",Project.MSG_ERR);
                throw new BuildException();
            }
        }
        
        log("uploading file",Project.MSG_VERBOSE);
        folder.uploadFile(getTargetFileName(),fileDescription,fileStatus,fromFile);
        
        log("done",Project.MSG_VERBOSE);
    }
    
    private String getTargetFolder() {
        int idx = toFileName.lastIndexOf('/');
        if(idx==-1)     return "";
        else            return toFileName.substring(0,idx);
    }
    
    private String getTargetFileName() {
        int idx = toFileName.lastIndexOf('/');
        if(idx==-1)     return toFileName;
        else            return toFileName.substring(idx+1);
    }
}
