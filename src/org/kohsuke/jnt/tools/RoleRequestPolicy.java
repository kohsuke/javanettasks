/*
 * Created on Aug 7, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

/**
 * @author kk122374
 */
public class RoleRequestPolicy {
    
    private final Policy[] lines;
    
    /** Used to resolve relative file names inside the policy file. */
    private final File baseDir;
    
    /**
     * Parses a policy file from a file.
     */
    public RoleRequestPolicy(File policyFile) throws IOException, ParseException {
        baseDir = policyFile.getParentFile();
        List lines = new ArrayList();
        
        BufferedReader reader = new BufferedReader(new FileReader(policyFile));
        String line;
        while((line=reader.readLine())!=null) {
            if(line.startsWith("#"))    continue;   // comment
            lines.add(new Policy(line));
        }
        
        this.lines = (Policy[]) lines.toArray(new Policy[lines.size()]);
    }
    
    /**
     * Computes the action that should be taken against this request.
     */
    public Action determineAction(RoleRequest request) {
        for( int i=0; i<lines.length; i++ ) {
            Action a = lines[i].matches(request);
            if(a!=null) return a;
        }
        return null;
    }
    
    /**
     * Represents one filter condition.
     */
    private class Policy {
        private final String projectName;
        private final String userName;
        private final String roleName;
        private final Action action;
        
        public Policy( String line ) throws ParseException, IOException {
            String[] parts = line.split(":");
            if(parts.length!=4) throw new ParseException("incorrect format "+line,-1);
            
            projectName = parts[0].equals("*")?null:parts[0];
            userName = parts[1].equals("*")?null:parts[1];
            roleName = parts[2].equals("*")?null:parts[2];
            
            if( parts[3].equals("APPROVE") )
                action = approveAction;
            else
            if( parts[3].startsWith("DECLINE ") )
                action = new DeclineAction(new File(baseDir,parts[3].substring(8)));
            else
            if( parts[3].equals("DEFER"))
                action = null;
            else
                throw new ParseException("undefined action "+parts[3],-1);
        }
        
        /**
         * Checks if the request matches this policy,
         * and if so return the action to be taken.
         */
        public Action matches( RoleRequest req ) {
            if( projectName!=null && !req.projectName.equals(projectName) )
                return null;
            if( userName!=null && !req.userName.equals(userName) )
                return null;
            if( roleName!=null && !req.roleName.equals(roleName) )
                return null;
            
            // match
            return action;
        }
    }
    
    /**
     * Action is a task that can be run on a request.
     */
    public interface Action {
        void process( RoleRequest request ) throws ProcessingException;
    }
    
    private static final Action approveAction = new Action() {
        public void process(RoleRequest request) throws ProcessingException {
            request.grant(JavaNet.connect());
        }
        public String toString() {
            return "approve";
        }
    };
    
    private static class DeclineAction implements Action {
        private final String message;
        
        /** Obtains the message from a file. */
        public DeclineAction( File msg ) throws IOException {
            StringBuffer buf = new StringBuffer();
            BufferedReader in = new BufferedReader(new FileReader(msg));
            String line;
            while((line=in.readLine())!=null) {
                buf.append(line);
                buf.append('\n');
            }
            message = buf.toString();
        }
        
        public void process(RoleRequest request) throws ProcessingException {
            request.decline(JavaNet.connect(),message);
        }
        
        public String toString() {
            return "decline";
        }
    }
}
