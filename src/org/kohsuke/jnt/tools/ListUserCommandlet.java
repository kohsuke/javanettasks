package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNRole;
import org.kohsuke.jnt.JNUser;
import org.kohsuke.jnt.ProcessingException;

import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;

/**
 * List up all the members of the project by role.
 *
 * @author Kohsuke Kawaguchi
 */
public class ListUserCommandlet extends Commandlet {
    public String getShortDescription() {
        return "list all the users of a given project(s)";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: listUser [-r <role>] <project> ... ");
        out.println("List all the members of the project(s).");
        out.println("  -r : only list members that have the given role");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length==0) {
            printUsage(System.err);
            return -1;            
        }

        JNRole role = null;
        int idx=0;
        if(args[0].equals("-r")) {
            idx++;
            role = connection.connect().getRole(args[idx++]);
        }

        Set<JNUser> users = new TreeSet<JNUser>();
        while(idx<args.length) {
            JNProject proj = connection.connect().getProject(args[idx++]);
            listMembers(proj,role,users);
        }

        for (JNUser user : users) {
            System.out.println(user.getName());
        }

        return 0;
    }

    private void listMembers(JNProject proj, JNRole role, Set<JNUser> users) throws ProcessingException {
        if(role!=null)
            users.addAll(proj.getMembership().getUserOf(role));
        else
            users.addAll(proj.getMembership().getMembers());
    }
}
