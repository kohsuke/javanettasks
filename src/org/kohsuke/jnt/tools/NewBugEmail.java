package org.kohsuke.jnt.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

/**
 * Represents the notification e-mail from BugZilla about a new bug.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NewBugEmail {
    
    private static Pattern PATTERN_REPORTED_BY = Pattern.compile(" *Reported By: (.*)");
    private static Pattern PATTERN_COMPONENT   = Pattern.compile(" *Component: (.*)");
    
    /**
     * User name who reported the problem.
     */
    public final String reportedBy; 
    
    /**
     * Project to which this bug is reported.
     */
    public final JNProject project;
    
    public NewBugEmail( JavaNet connection, Reader mail ) throws IOException, ParseException, ProcessingException {
        BufferedReader r = new BufferedReader(mail);
        
        String rb=null;
        String prj=null;
        
        String line;
        while((line=r.readLine())!=null) {
            Matcher m = PATTERN_REPORTED_BY.matcher(line);
            if(m.matches() && rb==null) {
                rb = m.group(1);
            }
            m = PATTERN_COMPONENT.matcher(line);
            if(m.matches() && prj==null) {
                prj = m.group(1);
            }
        }
        
        if(rb==null)
            throw new ParseException("unable to find the reported-by field",0);
        this.reportedBy = rb;
        
        if(prj==null)
            throw new ParseException("unable to find the component field",0);
        this.project = connection.getProject(prj);
    }
}
