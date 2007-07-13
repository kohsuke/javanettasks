package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNMailingList;
import org.kohsuke.jnt.JNMailingList.ProgressCallback;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.jnt.SubscriptionMode;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class CopySubscribersCommandlet extends Commandlet {
    public String getShortDescription() {
        return "copy the subscribers of one list to another";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: copySubscribers <src-project> <src-list> <dst-project> <dst-list>");
        out.println("Copy all the digest/normal subscribers into new list. Need admin access on both");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=4) {
            printUsage(System.err);
            return -1;
        }

        JNMailingList src = getList(connection, args[0], args[1]);
        JNMailingList dst = getList(connection, args[2], args[3]);

        copy(src,dst,SubscriptionMode.DIGEST);
        copy(src,dst,SubscriptionMode.NORMAL);

        return 0;
    }

    private void copy(JNMailingList src, JNMailingList dst, SubscriptionMode mode) throws ProcessingException {
        dst.massSubscribe( src.getSubscribers(mode), mode, new ProgressCallback() {
            public void onProgress(int current, int total) {
                System.out.println(current+"/"+total);
            }
        });
    }

    private JNMailingList getList(ConnectionFactory connection, String p, String l) throws ProcessingException {
        JNProject project = connection.connect().getProject(p);
        return project.getMailingLists().get(l);
    }
}
