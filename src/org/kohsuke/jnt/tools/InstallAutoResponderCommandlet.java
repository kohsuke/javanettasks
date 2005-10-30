package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNMailingList;
import org.kohsuke.jnt.SubscriptionMode;

import java.io.PrintStream;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class InstallAutoResponderCommandlet extends Commandlet {
    public String getShortDescription() {
        return "Installs auto-responder for people who send e-mails without subscription";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: InstallAutoResponder <project> <mailing list> ...");
        out.println("");
        out.println("Installs the autoresponder daemon as the mailing list moderator");
        out.println("to the specified list.");
        out.println("");
        out.println("See http://weblogs.java.net/blog/kohsuke/archive/2005/08/handling_modera.html");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length<2) {
            printUsage(System.err);
            return -1;
        }

        JNProject p = connection.connect().getProject(args[0]);

        for( int i=1; i<args.length; i++ ) {
            JNMailingList ml = p.getMailingLists().get(args[i]);
            if(ml==null) {
                System.err.println("No such mailing list : "+args[i]);
                return -1;
            }

            System.err.println("Processing "+args[i]);
            // replace the moderator list
            List<String> subscribers = ml.getSubscribers(SubscriptionMode.MODERATOR);
            ml.massUnsubscribe(subscribers, SubscriptionMode.MODERATOR, null);
            ml.massSubscribe("autoresponder@dev.java.net", SubscriptionMode.MODERATOR);
        }

        return 0;
    }
}
