package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNMailingList;
import org.kohsuke.jnt.JNMailingList.ProgressCallback;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.SubscriptionMode;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class UnsubscribeAllCommandlet extends Commandlet {
    public String getShortDescription() {
        return "Unsubscribe all list subscribers";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: unsubscribeAll <project> <list>");
        out.println("Remove all the current subscribers");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=2) {
            printUsage(System.err);
            return -1;
        }

        JNProject project = connection.connect().getProject(args[0]);
        JNMailingList list = project.getMailingLists().get(args[1]);

        SubscriptionMode mode = SubscriptionMode.NORMAL;
        // bug in java.net. can only remove one person at a time.
        for(String n : list.getSubscribers(mode)) {
            list.massUnsubscribe(new String[]{n},mode,new ProgressCallback() {
                public void onProgress(int current, int total) {
                    System.out.println(current+"/"+total);
                }
            });
        }

        return 0;
    }
}
