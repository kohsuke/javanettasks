package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNMailingList;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.SubscriptionMode;

import java.io.PrintStream;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubscribersCommandlet extends Commandlet {
    public String getShortDescription() {
        return "list the subscribers of a mailing list";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: subscribers <project> <list> [digest|normal]");
        out.println("List up all e-mail addresses that subscribe to the given ML");
        out.println("  project : name of the project");
        out.println("  list    : mailing list name to list subscribers from");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=3) {
            printUsage(System.err);
            return -1;
        }

        JNProject project = connection.connect().getProject(args[0]);
        JNMailingList list = project.getMailingLists().get(args[1]);

        List<String> l = list.getSubscribers(SubscriptionMode.valueOf(args[2].toUpperCase()));
        for (String adrs : l)
            System.out.println(adrs);

        return 0;
    }
}
