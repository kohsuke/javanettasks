/*
 * Created on Aug 6, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt.tools;

import java.io.File;
import java.io.InputStreamReader;

import org.kohsuke.jnt.JavaNet;

/**
 * Command line interface to the java.net automation tool.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        JavaNet connection = JavaNet.connect();
        
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
        
        System.out.println("Unknown command "+args[0]);
    }
}
