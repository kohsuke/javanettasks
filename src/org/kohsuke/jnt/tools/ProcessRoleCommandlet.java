package org.kohsuke.jnt.tools;

import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class ProcessRoleCommandlet extends Commandlet {
    public String getShortDescription() {
        return "process a role request automatically according to a policy file";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: processRole <policyFile>");
        out.println("See https://javanettasks.dev.java.net/nonav/maven/tools/processRole.html");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=1) {
            printUsage(System.err);
            return -1;
        }

        RoleRequestPolicy policy = new RoleRequestPolicy(new File(args[0]));
        RoleRequest request = new RoleRequest(new InputStreamReader(System.in));

        RoleRequestPolicy.Action action = policy.determineAction(request);
        if(action==null) {
            System.out.println("no action is taken");
        } else {
            System.out.println("action is to "+action);
            action.process(request);
        }
        return 0;
    }
}
