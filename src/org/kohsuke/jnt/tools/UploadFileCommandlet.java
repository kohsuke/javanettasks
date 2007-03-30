package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.FileStatus;
import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.ProcessingException;

import java.io.File;
import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class UploadFileCommandlet extends AbstractFileUploadCommandlet {
    public String getShortDescription() {
        return "upload a file to java.net";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: uploadFile <projectName> <filePath on server> <description> <status> <source file>");
        out.println("Upload a file to java.net document&files section");
        out.println("  projectName : the java.net project name to which the file is uploaded");
        out.println("  filePath    : the filepath in documents&files section, such as /releases/foo.zip");
        out.println("                or /bar.txt");
        out.println("  description : Human-readable file description. Can contain spaces, but no HTML.");
        out.println("  status      : one of the file status (draft/reviewed/baselined/stable/archival/obsolete)");
        out.println("  source file : the file on your file system to be uploaded");
    }

    protected int action(JNFileFolder folder, String fileName, String description, FileStatus status, String _src) throws ProcessingException {
        File src = new File(_src);
        if(!src.isFile() || !src.exists()) {
            System.err.println("File "+src+" does not exist");
            return 1;
        }
        JNFile file = folder.uploadFile(fileName, description, status, src);
        System.out.println("Posted "+file.getURL());
        return 0;
    }
}
