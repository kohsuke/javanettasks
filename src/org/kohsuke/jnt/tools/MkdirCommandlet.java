package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;

import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * @author Kohsuke Kawaguchi
 */
public class MkdirCommandlet extends Commandlet {
    public String getShortDescription() {
        return "creates a folder in the documents&files section";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage : mkdir <project> <path>");
        out.print("Path should be something like /foo/bar");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        JavaNet conn = connection.connect();

        if(args.length!=2) {
            printUsage(System.err);
            return -1;
        }

        JNProject project = conn.getProject(args[0]);

        JNFileFolder f = project.getRootFolder();
        StringTokenizer dir = new StringTokenizer(args[1],"/");
        while(dir.hasMoreTokens()) {
            String step = dir.nextToken();
            if(f.getSubFolder(step)==null)
                f = f.createFolder(step,"");
            else
                f = f.getSubFolder(step);
        }

        return 0;
    }
}
