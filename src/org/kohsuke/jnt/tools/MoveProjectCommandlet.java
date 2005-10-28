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
        out.println("Usage: moveProject <project> <newParent>");
        out.println("'project' becomes a child project of 'newParent'");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=2) {
            printUsage(System.err);
            return -1;
        }

        JavaNet conn = connection.connect();

        JNProject child = conn.getProject(args[0]);
        JNProject parent = conn.getProject(args[1]);

        child.setParent(parent);

        return 0;
    }
}
