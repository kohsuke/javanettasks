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
