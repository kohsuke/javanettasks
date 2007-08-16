package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.JNProject;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class MoveProjectCommandlet extends Commandlet {
    public String getShortDescription() {
        return "change the parent of a project";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: moveProject <project> ... <newParent>");
        out.println("'project' becomes a child project of 'newParent'");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length>2) {
            printUsage(System.err);
            return -1;
        }

        JavaNet conn = connection.connect();

        JNProject parent = conn.getProject(args[args.length-1]);

        for( int i=0; i<args.length-1; i++ ) {
            System.out.println("Moving "+args[i]);
            JNProject child = conn.getProject(args[i]);
            child.setParent(parent);
        }

        return 0;
    }
}
