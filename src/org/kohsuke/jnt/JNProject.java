package org.kohsuke.jnt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;

/**
 * Java&#2E;net project.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class JNProject {
    /** The project name. */
    protected final String projectName;
    
    protected final WebConversation wc;
    
    //
    // lazily created
    //
    private JNMembership membership;
    
    /**
     * URL (as String) to {@link JNFileFolder} map.
     */
    private final Map folders = new HashMap();
    
    
    protected JNProject(JavaNet net, String name) throws IOException, SAXException {
        this.wc = net.wc;
        this.projectName = name;
        
        // check if the project really exists
        wc.getResponse(getURL()+"/");
    }

    /**
     * Gets the name of this project.
     */
    public String getName() {
        return projectName;
    }
    
    /**
     * Accesses the membership section of the project.
     */
    public JNMembership getMembership() {
        if(membership==null)
            membership = new JNMembership(this);
        return membership;
    }

    
    /**
     * @return "http://PROJECTNAME.dev.java.net/"
     */
    protected String getURL() {
        return "https://"+projectName+".dev.java.net";
    }

    /**
     * Obtains a file folder object for the given URL.
     */
    protected JNFileFolder getFolderFromURL(String url) {
        JNFileFolder r = (JNFileFolder)folders.get(url);
        if(r==null) {
            folders.put(url, r=new JNFileFolder(this,url) );
        }
        return r;
    }
    
    /**
     * Obtains the top folder in the file sharing section.
     */
    public JNFileFolder getFolder( String folderPathName ) throws ProcessingException {
        JNFileFolder top = getFolderFromURL(getURL()+"/servlets/ProjectDocumentList");
        return top.getSubFolder(folderPathName);
    }
    
    /**
     * Create an empty news item for this project.
     */
    public JNNewsItem createNewsItem() {
        return new JNNewsItem(this, getURL()+"/servlets/ProjectNewsAdd");
    }

}
