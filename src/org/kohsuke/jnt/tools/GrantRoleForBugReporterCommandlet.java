package org.kohsuke.jnt.tools;

import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class GrantRoleForBugReporterCommandlet extends Commandlet {
    public String getShortDescription() {
        return "reads issue tracker e-mail from stdin and approve a role to the submitter";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: grantRoleForBugReporter <projectName> <roleName>");
        out.println("See https://javanettasks.dev.java.net/nonav/maven/tools/grantRoleForBugReporter.html");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=2) {
            printUsage(System.err);
            return -1;
        }
        NewBugEmail nbe = new NewBugEmail(connection.connect(),new InputStreamReader(System.in));
        if(args[1].equals(nbe.project.getName())) {
            System.out.println("granting "+args[2]+" to "+nbe.reportedBy);
            nbe.project.getMembership().grantRole(nbe.reportedBy,args[2]);
        } else {
            System.out.println("project name didn't match");
        }
        return 0;
    }
}
