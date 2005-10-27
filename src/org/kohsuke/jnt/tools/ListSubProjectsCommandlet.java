package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class ListSubProjectsCommandlet extends Commandlet {
    public String getShortDescription() {
        return "list all the subprojects of a given project(s)";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: listSubProjects [-r] <project> ... ");
        out.println("List all the subprojects of a given project(s)");
        out.println("  -r : list subprojects recursively");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        boolean recursive = false;
        int idx=0;
        if(args[0].equals("-r")) {
            recursive = true;
            idx++;
        }

        while(idx<args.length) {
            JNProject proj = connection.connect().getProject(args[idx++]);
            listProjects(proj,recursive);
        }

        return 0;
    }

    private void listProjects(JNProject proj, boolean recursive) throws ProcessingException {
        for (JNProject sub : proj.getSubProjects()) {
            System.out.println(sub.getName());
            if (recursive)
                listProjects(sub, recursive);
        }

    }
}
