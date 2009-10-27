package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JavaNet;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class DumpVotesCommandlet extends Commandlet {
    public String getShortDescription() {
        return "dump all votes of the issue tracker";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: dump-votes <project>");
        out.println("Dump all votes in the issue tracker as CSV");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=1) {
            printUsage(System.err);
            return -1;
        }

        String project = args[0];
        JavaNet jn = connection.connect();
        String id = jn.runAdHocQuery("SELECT PROJECT_ID from HELM_PROJECT WHERE NAME='"+ project +"'")[0][0];
        String[][] votes = jn.runAdHocQuery(String.format(
        "SELECT u.LOGIN_NAME AS user, v.issue_id AS issue, v.count AS count FROM issues_%1$s.votes AS v, issues_%1$s.profiles AS p, HELM_USER AS u WHERE v.who=p.userid AND p.helm_user_id=u.USER_ID;",id));

        for (String[] vote : votes) {
            System.out.printf("%s,%s,%s\n",vote[0],vote[1],vote[2]);
        }

        return 0;
    }
}

