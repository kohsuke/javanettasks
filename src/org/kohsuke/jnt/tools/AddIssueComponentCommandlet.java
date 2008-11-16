package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNIssueComponent;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;

import java.io.PrintStream;

/**
 * Adds issue tracker component/subcomponent
 * @author Kohsuke Kawaguchi
 */
public class AddIssueComponentCommandlet extends Commandlet {
    public String getShortDescription() {
        return "posts a new announcement";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage : add-issue-component <project> <component> <new-subcomponent-name> <description> <initial owner> <qa contact>");
        out.print("Adds a new issue tracker subcomponent");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        JavaNet conn = connection.connect();

        if(args.length!=6) {
            printUsage(System.err);
            return -1;
        }

        JNProject project = conn.getProject(args[0]);
        JNIssueComponent c = project.getIssueTracker().getComponents().get(args[1]);
        c.add(args[2],args[3],args[4],args[5]);
        return 0;
    }
}
