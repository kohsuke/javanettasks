package org.kohsuke.jnt.tools;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class DeclineRoleCommandlet extends Commandlet {
    public String getShortDescription() {
        return "decline a pending role request";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: declineRole <project> <user> <role> <reason>");
        out.println("Deny a pending role request");
        out.println("  project : the name of the project in which the request is denied");
        out.println("  user    : the user who is requesting the role");
        out.println("  role    : the role to be declined, such as 'Observer'");
        out.println("  reason  : this text will show in the e-mail that the user will receive");
        out.println("            give some reasons why his/her role request was denied");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=4) {
            printUsage(System.err);
            return -1;
        }
        new RoleRequest(args[0],args[1],args[2]).decline(connection.connect(),args[3]);
        return 0;
    }
}
