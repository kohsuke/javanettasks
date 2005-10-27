package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNMailingLists;
import org.kohsuke.jnt.JNMailingList;
import org.kohsuke.jnt.SubscriptionMode;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubscribeListCommandlet extends Commandlet {
    public String getShortDescription() {
        return "subscribe yourself to the mailing lists of a project";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: subscribeList <project> <list> ...");
        out.println("Subscribe to the specified mailing lists of the project");
        out.println("  project : name of the project");
        out.println("  list    : mailing list names to subscribe to");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length<2) {
            printUsage(System.err);
            return -1;
        }

        JNProject project = connection.connect().getProject(args[0]);
        JNMailingLists list = project.getMailingLists();

        for( int i=1; i<args.length; i++) {
            JNMailingList ml = list.get(args[i]);
            if(ml==null) {
                System.err.println("No such list: "+args[i]);
                continue;
            }

            ml.subscribe(SubscriptionMode.NORMAL);
        }

        return 0;
    }
}
