/*
 * Created on Aug 6, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNUser;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * Command line interface to the java.net automation tool.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }

    public static int run(String[] args) throws Exception {
        JavaNet connection = JavaNet.connect();

        if(args.length>0) {
            if(args[0].equals("uploadFile")) {
                String projectName = args[1];
                String folderPath = args[2];
                String description = args[3];
                String status = args[4];
                File src = new File(args[5]);
                if(!src.isFile() || !src.exists()) {
                    System.err.println("File "+src+" does not exist");
                    return 1;
                }

                int idx = folderPath.lastIndexOf('/');
                if(idx<0)
                    throw new IllegalArgumentException(folderPath+" doesn't have a file name");
                String fileName = folderPath.substring(idx+1);
                folderPath = folderPath.substring(0,idx);

                JNFileFolder folder = connection.getProject(projectName).getFolder(folderPath);
                JNFile file = folder.getFiles().get(fileName);

                if( file!=null ) {
                    file.delete();
                }

                folder.uploadFile(fileName,description,status,src);
                return 0;
            }
            if(args[0].equals("grantRole")) {
                // grant a new role
                switch(args.length) {
                case 1:
                    new RoleRequest(new InputStreamReader(System.in)).grant(connection);
                    return 0;
                case 4:
                    new RoleRequest(args[1],args[2],args[3]).grant(connection);
                    System.out.println("done");
                    return 0;
                }
            }

            if(args[0].equals("declineRole")) {
                new RoleRequest(args[1],args[2],args[3]).decline(connection,args[4]);
                System.out.println("done");
                return 0;
            }

            if(args[0].equals("processRole")) {
                if(args.length!=2) {
                    usage("processRole <policyFile>");
                    return 1;
                }
                RoleRequestPolicy policy = new RoleRequestPolicy(new File(args[1]));
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

            if(args[0].equals("grantRoleForBugReporter")) {
                if(args.length!=3) {
                    System.out.println(args.length);
                    usage("grantRoleForBugReporter <projectName> <roleName>");
                    return 1;
                }
                NewBugEmail nbe = new NewBugEmail(connection,new InputStreamReader(System.in));
                if(args[1].equals(nbe.project.getName())) {
                    System.out.println("granting "+args[2]+" to "+nbe.reportedBy);
                    nbe.project.getMembership().grantRole(nbe.reportedBy,args[2]);
                } else {
                    System.out.println("project name didn't match");
                }
                return 0;
            }

            if(args[0].equals("listMyProjects")) {
                Iterator itr = connection.getMyself().getMyProjects().iterator();
                System.err.println("user name "+connection.getMyself().getName());
                while(itr.hasNext()) {
                    System.out.println(((JNProject)itr.next()).getName());
                }
                return 0;
            }

            if(args[0].equals("projectInfo")) {
                JNProject proj = connection.getProject(args[1]);
                if( proj.getParent()!=null )
                    System.out.println( "parent is "+proj.getParent().getName() );
                else
                    System.out.println( "no parent (top-level)" );
                JNProject com = proj.getOwnerCommunity();
                System.out.println( com!=null?("belong to "+com.getName()+" community"):"no parent community" );
                System.out.println( proj.isCommunity()?"a community":"not a community" );
                System.out.println( "owners are " );
                for (JNUser u : proj.getOwners())
                    System.out.println(u.getName());
                return 0;
            }

            if(args[0].equals("listSubProjects")) {
                boolean recursive = false;
                int idx=1;
                if(args[1].equals("-r")) {
                    recursive = true;
                    idx++;
                }

                JNProject proj = connection.getProject(args[idx]);
                listProjects(proj,recursive);
                return 0;
            }
        }

        usage(
                "<command> ....\n" +
                "where commands are:\n" +
                "  processRole\n" +
                "      process 'new role requested' e-mails\n" +
                "  grantRoleForBugReporter\n" +
                "      process new bug report and grant a role to the submitter.\n" +
                "  listSubProjects [-r] <projectName>\n" +
                "      list all sub-projects of the given project (recursively with -r)" +
                "      to stdout\n");
        return 1;
    }

    private static void listProjects(JNProject proj, boolean recursive) throws ProcessingException {
        for (JNProject sub : proj.getSubProjects()) {
            System.out.println(sub.getName());
            if (recursive)
                listProjects(sub, recursive);
        }

    }

    private static void usage(String msg) {
        System.err.println("Usage: java -jar javanettasks.jar "+msg);
        System.exit(1);
    }
}
