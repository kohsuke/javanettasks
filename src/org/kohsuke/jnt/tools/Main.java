/*
 * Created on Aug 6, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt.tools;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNUser;
import org.kohsuke.jnt.JavaNet;

/**
 * Command line interface to the java.net automation tool.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        JavaNet connection = JavaNet.connect();
        
        if(args.length>0) {
            if(args[0].equals("grantRole")) {
                // grant a new role
                switch(args.length) {
                case 1:
                    new RoleRequest(new InputStreamReader(System.in)).grant(connection);
                    return;
                case 4:
                    new RoleRequest(args[1],args[2],args[3]).grant(connection);
                    System.out.println("done");
                    return;
                }
            }
            
            if(args[0].equals("declineRole")) {
                new RoleRequest(args[1],args[2],args[3]).decline(connection,args[4]);
                System.out.println("done");
                return;
            }
            
            if(args[0].equals("processRole")) {
                if(args.length!=2) {
                    usage("processRole <policyFile>");
                    return;
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
                return;
            }
            
            if(args[0].equals("grantRoleForBugReporter")) {
                if(args.length!=3) {
                    System.out.println(args.length);
                    usage("grantRoleForBugReporter <projectName> <roleName>");
                    return;
                }
                NewBugEmail nbe = new NewBugEmail(connection,new InputStreamReader(System.in));
                if(args[1].equals(nbe.project.getName())) {
                    System.out.println("granting "+args[2]+" to "+nbe.reportedBy);
                    nbe.project.getMembership().grantRole(nbe.reportedBy,args[2]);
                } else {
                    System.out.println("project name didn't match");
                }
                return;
            }
            
            if(args[0].equals("listMyProjects")) {
                Iterator itr = connection.getMyself().getMyProjects().iterator();
                System.err.println("user name "+connection.getMyself().getName());
                while(itr.hasNext()) {
                    System.out.println(((JNProject)itr.next()).getName());
                }
                return;
            }
            
            if(args[0].equals("projectInfo")) {
                JNProject proj = connection.getProject(args[1]);
                System.out.println( "parent is "+proj.getParent().getName() );
                JNProject com = proj.getOwnerCommunity();
                System.out.println( com!=null?("belong to "+com.getName()+" community"):"no parent community" );
                System.out.println( proj.isCommunity()?"a community":"not a community" );
                for (Iterator itr = proj.getOwners().iterator(); itr.hasNext(); ) {
                    JNUser u = (JNUser) itr.next();
                    System.out.println(u.getName());
                }
                return;
            }
        }
        
        usage(
                "<command> ....\n" +
                "where commands are:\n" +
                "  processRole\n" +
                "      process 'new role requested' e-mails\n" +
                "  grantRoleForBugReporter\n" +
                "      process new bug report and grant a role to the submitter.\n");
    }
    
    private static void usage(String msg) {
        System.err.println("Usage: java -jar javanettasks.jar "+msg);
        System.exit(1);
    }
}
