package org.kohsuke.jnt;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
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

    private final JavaNet net;
    
    //
    // lazily created
    //
    private JNMembership membership;
    
    /**
     * URL (as String) to {@link JNFileFolder} map.
     */
    private final Map folders = new HashMap();

    /**
     * News item section.s
     */
    private JNNewsItems newsItems = new JNNewsItems(this);
    
    /**
     * Parent project name.
     * Lazily retrieved by the {@link #parseProjectInfo()}method. 
     */
    private String parentProject;
    
    /**
     * Name of the community to which this project belongs to.
     * It will be the same as the project name if the project
     * is top level (which means it's a community or it's a top-level
     * project.)
     * Lazily retrieved by the {@link #parseProjectInfo()}method. 
     */
    private String communityName;
    
    /**
     * A set of {@link JNUser} objects that represent the project owners. 
     * Lazily retrieved by the {@link #parseProjectInfo()}method. 
     */
    private Set owners;
    
    protected JNProject(JavaNet net, String name) {
        this.net = net;
        this.wc = net.wc;
        this.projectName = name;
    }
    
    /**
     * Parse the project index page.
     */
    private void parseProjectInfo() throws ProcessingException {
        if(communityName!=null)
            return; // already parsed.
        
        try {
            Document dom = Util.getDom4j(wc.getResponse(getURL()+"/"));
            List as = dom.selectNodes("//DIV[@id='breadcrumbs']/TABLE[1]/TR[1]/TD[1]/A");
            if(as.size()<2)
                throw new IllegalStateException("failed to parse "+getURL()+"/");
            
            communityName = ((Element)as.get(1)).getTextTrim();
            
            if(as.size()>2)
                parentProject = ((Element)as.get(2)).getTextTrim();
            
            Set owners = new HashSet();
            List os = dom.selectNodes("//DIV[@class='axial']/TABLE/TR[TH/text()='Owner(s)']/TD/A");
            for( int i=0; i<os.size(); i++ )
                owners.add( net.getUser( ((Element)os.get(i)).getTextTrim() ) );
            this.owners = Collections.unmodifiableSet(owners);
        } catch( SAXException e ) {
            throw new ProcessingException(e);
        } catch( IOException e ) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Gets the name of this project.
     */
    public String getName() {
        return projectName;
    }
    
    /**
     * Gets the parent project.
     * 
     * @return null
     *      if this is a top-level project. For example,
     *      all communities are top-level projects.
     */
    public JNProject getParent() throws ProcessingException {
        parseProjectInfo();
        if( parentProject==null )   return null;
        return net.getProject(parentProject); 
    }
    
    /**
     * Gets the community project to which this project
     * directly/indirectly belongs.
     * 
     * @return
     *      always return non-null project object.
     *      If this project is a community, this method
     *      returns <tt>this</tt>.
     */
    public JNProject getOwnerCommunity() throws ProcessingException {
        parseProjectInfo();
        return net.getProject(communityName);
    }
    
    /**
     * Returns a set of {@link JNUser}s that represent project owners.
     * 
     * @return
     *      always return non-null set. If the project doesn't have
     *      any owner, it returns an empty set. The set is read-only.
     */
    public Set getOwners() throws ProcessingException {
        parseProjectInfo();
        return owners;
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
     * Obtains the object that represents the news item section.
     */
    public JNNewsItems getNewsItems() {
        return newsItems;
    }

}
