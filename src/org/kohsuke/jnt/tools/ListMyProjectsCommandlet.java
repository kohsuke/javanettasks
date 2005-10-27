package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNProject;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class ListMyProjectsCommandlet extends Commandlet {
    public String getShortDescription() {
        return "list all projects that you belong to.";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: listMyProjects");
        out.println("List all projects that you belong to.");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        for (JNProject p : connection.connect().getMyself().getMyProjects())
            System.out.println(p.getName());
        return 0;
    }
}
