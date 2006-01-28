/*
 * Created on Aug 6, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A notification message sent from java.net when someone requests a role.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class RoleRequest {
    /** Project to which the role is requested. */
    public final String projectName;
    /** The account name of the user who requested the role. */
    public final String userName;
    /** The role that was requested. */
    public final String roleName;
    
    /** Creates a new request. */
    public RoleRequest(String _projectName, String _userName, String _roleName) {
        this.projectName = _projectName;
        this.userName = _userName;
        this.roleName = _roleName;  
    }
    
    /**
     * Parses the notification e-mail and constructs this object.
     *
     * @param message
     *      The {@link Reader} that reads an e-mail. This could be just the content
     *      of an e-mail, or the whole message including MIME headers.
     * @throws ParseException
     *      if the text isn't a role request e-mail.
     */
    public RoleRequest( Reader message ) throws IOException, ParseException {
        BufferedReader in = new BufferedReader(message);
        
        // read all the lines for easier processing.
        List<String> lineList = new ArrayList<String>();
        String line;
        while((line=in.readLine())!=null) {
            lineList.add(line);
        }
        String[] lines = lineList.toArray(new String[lineList.size()]);

        // look for the meat
        String interest = "";
        for(int i=0; i<lines.length; i++) {
            if(lines[i].startsWith("User ") && lines[i].indexOf(':')==-1) {
                // accumulate lines until we see another empty line.
                for( ; lines[i].length()!=0 && i<lines.length; i++ )
                    interest += lines[i] + ' ';
            }
        }
        if( interest.length()==0 )
            throw new ParseException("unable to find the meat of the message",-1);
        
        // parse the important pieces
        Matcher m = regexp.matcher(interest);
        if(!m.matches())
            throw new ParseException("unable to parse the meat of the message against the template",-1);
        
        // the notification e-mail from java.net
        // main contains HTML escapes. So normalize them here
        
        this.projectName = m.group(3);
        this.userName = m.group(1);
        this.roleName = m.group(2).replaceAll("&nbsp;"," ").replaceAll("&gt;",">");
    }
    
    /** Grants this request. */
    public void grant( JavaNet javanet ) throws ProcessingException {
        javanet.getProject(projectName).getMembership().grantRole(javanet.getUser(userName),roleName);
    }
    
    /** Declines this request. */
    public void decline( JavaNet javanet, String reason ) throws ProcessingException {
        javanet.getProject(projectName).getMembership().declineRole(javanet.getUser(userName),roleName,reason);
    }
    
    public String toString() {
        return "role:"+roleName+",user:"+userName+",project:"+projectName;
    }
    
    
    
    private static final Pattern regexp = Pattern.compile(
        "User (\\S+) has requested the (.+) role in the (\\S+) project\\. ");

    
    public static void main(String[] args) throws IOException, ParseException {
        // test
        System.out.println(new RoleRequest(new InputStreamReader(System.in)));
    }
}
