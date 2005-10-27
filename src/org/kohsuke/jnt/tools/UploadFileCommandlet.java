package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.FileStatus;
import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;

import java.io.File;
import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class UploadFileCommandlet extends Commandlet {
    public String getShortDescription() {
        return "upload a file to java.net";
    }

    public void printUsage(PrintStream out) {
        System.out.println("Usage: uploadFile <projectName> <filePath on server> <description> <status> <source file>");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=5) {
            printUsage(System.err);
            return 1;
        }

        String projectName = args[0];
        String folderPath = args[1];
        String description = args[2];
        FileStatus status = FileStatus.parse(args[3]);
        File src = new File(args[4]);
        if(!src.isFile() || !src.exists()) {
            System.err.println("File "+src+" does not exist");
            return 1;
        }

        int idx = folderPath.lastIndexOf('/');
        if(idx<0)
            throw new IllegalArgumentException(folderPath+" doesn't have a file name");
        String fileName = folderPath.substring(idx+1);
        folderPath = folderPath.substring(0,idx);

        JNFileFolder folder = connection.connect().getProject(projectName).getFolder(folderPath);
        if(folder==null)
            throw new IllegalArgumentException("No such folder "+folderPath);
        JNFile file = folder.getFiles().get(fileName);

        if( file!=null ) {
            file.delete();
        }

        folder.uploadFile(fileName,description,status,src);
        return 0;
    }
}
