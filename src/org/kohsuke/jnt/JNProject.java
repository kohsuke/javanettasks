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
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;

/**
 * Java&#2E;net project.
 * 
 * <p>
 * Comparisons are based on their project names, so one can compare
 * two {@link JNProject}s from different {@link JavaNet}s.
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
     * Name of the top-level project to which this project belongs to.
     * 
     * <p>
     * It will be the same as the project name if the project
     * is top level (which means it's a community or it's a top-level
     * project.)
     * 
     * Lazily retrieved by the {@link #parseProjectInfo()}method. 
     */
    private String topLevelName;
    
    /**
     * If this project is a community, true. If not, false.
     * 
     * Lazily retrieved by the {@link #parseProjectInfo()}method,
     * and until then it's null. 
     */
    private Boolean isCommunity;
    
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
        if(topLevelName!=null)
            return; // already parsed.
        
        try {
            Document dom = Util.getDom4j(wc.getResponse(getURL()+"/"));
            List as = dom.selectNodes("//DIV[@id='breadcrumbs']//A");
            if(as.size()==0)
                throw new ProcessingException("failed to parse "+getURL()+"/");
            
            if(as.size()>2) {
                topLevelName = ((Element)as.get(1)).getTextTrim();
                parentProject = ((Element)as.get(as.size()-2)).getTextTrim();
            } else {
                topLevelName = projectName;
                parentProject = null;
            }
            
            if( dom.selectSingleNode("//DIV[@class='axial']/TABLE/TR[normalize-space(TH)='Project group'][normalize-space(TD)='communities']")!=null )
                isCommunity=Boolean.TRUE;
            else
                isCommunity=Boolean.FALSE;
                
            Set owners = new HashSet();
            List os = dom.selectNodes("//DIV[@class='axial']/TABLE/TR[TH/text()='Owner(s)']/TD/A");
            for( int i=0; i<os.size(); i++ )
                owners.add( net.getUser( ((Element)os.get(i)).getTextTrim() ) );
            this.owners = Collections.unmodifiableSet(owners);
        } catch( SAXException e ) {
            throw new ProcessingException(e);
        } catch( IOException e ) {
            throw new ProcessingException(e);
        } catch( DOMException e ) {
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
     *      If this project is a community, this method
     *      returns <tt>this</tt>.
     *      If this project doesn't belong to any community
     *      (which means it's a free standing project),
     *      this method returns <tt>null</tt>.
     */
    public JNProject getOwnerCommunity() throws ProcessingException {
        parseProjectInfo();
        JNProject p = net.getProject(topLevelName);
        if(p.isCommunity())
            return p;
        else
            return null;
    }
    
    /**
     * Returns true if this project is a community project.
     * 
     * <p>
     * Certain java.net projects are designated as "community"
     * projects (e.g., games.dev.java.net, java-ws-xml.dev.java.net)
     */
    public boolean isCommunity() throws ProcessingException {
       if( isCommunity==null )
           parseProjectInfo();
       return isCommunity.booleanValue();
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

    public int hashCode() {
        return projectName.hashCode();
    }
    
    public boolean equals(Object o) {
        if( o.getClass()!=this.getClass() )
            return false;
        return this.projectName.equals( ((JNProject)o).projectName );
    }
}
