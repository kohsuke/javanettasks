package org.kohsuke.jnt;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

/**
 * information about the current user in java&#2E;net.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNMyself {
    protected final WebConversation wc;

    private final JavaNet net;
    
    /**
     * Set of {@link JNProject} to which the current user belongs.
     * Never null.
     * 
     * This set is read-only.
     */
    public final Set myProjects;
    
    /**
     * Current user id logged in.
     */
    public final String userName;
    
    protected JNMyself(JavaNet net) throws ProcessingException {
        this.net = net;
        this.wc = net.wc;
        
        try {
            // obtain current user information
            WebResponse wr = wc.getResponse("https://www.dev.java.net/servlets/StartPage");
            Document dom = new DOMReader().read(wr.getDOM());
            
            // parse the user name
            userName = dom.selectSingleNode("//STRONG[@class='username']").getText();
            
            // parse my projects
            Set myProjects = new HashSet(); 
            List projects = dom.selectNodes("//DIV[@id='myprojects']//TR/TD[1]/A");
            for( int i=0; i<projects.size(); i++) {
                Element e = (Element)projects.get(i);
                myProjects.add(net.getProject(e.getText()));
            }
            this.myProjects = Collections.unmodifiableSet(myProjects);
        } catch( IOException e ) {
            throw new ProcessingException("unable to parse 'My start page'",e);
        } catch( SAXException e ) {
            throw new ProcessingException("unable to parse 'My start page'",e);
        }
    }
}
