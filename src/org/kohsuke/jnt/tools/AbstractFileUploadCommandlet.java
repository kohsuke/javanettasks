package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.FileStatus;
import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractFileUploadCommandlet extends Commandlet {
    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=5) {
            printUsage(System.err);
            return 1;
        }

        String projectName = args[0];
        String folderPath = args[1];
        String description = args[2];
        FileStatus status = FileStatus.parse(args[3]);
        String src = args[4];

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

        return action(folder, fileName, description, status, src);
    }

    protected abstract int action(JNFileFolder folder, String fileName, String description, FileStatus status, String src) throws Exception;
}
