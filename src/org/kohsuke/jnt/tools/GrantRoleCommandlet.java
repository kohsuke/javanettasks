package org.kohsuke.jnt.tools;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class GrantRoleCommandlet extends Commandlet {
    public String getShortDescription() {
        return "grant a role to an user on a project";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: grantRole <project> <user> <role>");
        out.println("Grant a role to an user on a project");
        out.println("  project : the name of the project in which the role is granted");
        out.println("  user    : the user that gets the new role");
        out.println("  role    : the role to be given, such as 'Observer'");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=3) {
            printUsage(System.err);
            return -1;
        }

        new RoleRequest(args[0],args[1],args[2]).grant(connection.connect());
        return 0;
    }
}
