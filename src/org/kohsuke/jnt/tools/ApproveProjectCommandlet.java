package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JavaNet;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class ApproveProjectCommandlet extends Commandlet {
    public String getShortDescription() {
        return "approves a project";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage : approveProject <project> ...");
        out.print("Approve all the specified projects.");
        out.print("Only community leaders can do this.");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        JavaNet conn = connection.connect();

        for(String arg : args) {
            System.out.println("Approving "+arg);
            conn.getProject(arg).approve();
        }
        return 0;
    }
}
