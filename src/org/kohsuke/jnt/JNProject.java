package org.kohsuke.jnt;

import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Java&#x2E;net project.
 * 
 * <p>
 * Comparisons are based on their project names, so one can compare
 * two {@link JNProject}s from different {@link JavaNet}s.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Bruno Souza
 */
public final class JNProject implements Comparable {
    /** The project name. */
    protected final String projectName;
    
    protected final WebConversation wc;

    final JavaNet net;
    
    //
    // lazily created
    //
    private JNMembership membership;
    private JNForums forums;
    private JNMailingLists mailingLists;

    /**
     * News item section.s
     */
    private JNNewsItems newsItems;
    
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
    private Set<JNUser> owners;

    private Set<JNProject> subProjects;

    private String summmary;
    private JNFileFolder rootFolder;

    private String ownerMessage;

    protected JNProject(JavaNet net, String name) {
        this.net = net;
        this.wc = net.wc;
        this.projectName = name;
    }

    /**
     * Returns the {@link JavaNet} object that this project belongs to.
     *
     * @return
     *      never null.
     */
    public JavaNet getConnection() {
        return net;
    }

    /**
     * Parse the project index page.
     */
    private void parseProjectInfo() throws ProcessingException {
        if(topLevelName!=null)
            return; // already parsed.

        new Scraper("unable to parse the project page of "+projectName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                Document dom = Util.getDom4j(wc.getResponse(_getURL()+'/'));

                List as = dom.selectNodes("//DIV[@id='breadcrumbs']//A");
                if(as.size()==0)
                    throw new ProcessingException("failed to parse "+getURL());

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

                // parse summary
                summmary = dom.selectSingleNode("//DIV[@class='axial']/TABLE/TR[TH/text()='Summary']/TD").getText();

                // parse owners
                Set<JNUser> owners = new TreeSet<JNUser>();
                List os = dom.selectNodes("//DIV[@class='axial']/TABLE/TR[TH/text()='Owner(s)']/TD/A");
                for( int i=0; i<os.size(); i++ )
                    owners.add( net.getUser( ((Element)os.get(i)).getTextTrim() ) );
                JNProject.this.owners = Collections.unmodifiableSet(owners);

                // parse sub-projects
                Set<JNProject> subProjects = new TreeSet<JNProject>();
                List sp = dom.selectNodes("//H3[text()='Subprojects']/following::*[1]/TR/TD/A");
                for( int i=0; i<sp.size(); i++ )
                    subProjects.add( net.getProject( ((Element)sp.get(i)).getTextTrim() ) );
                JNProject.this.subProjects = Collections.unmodifiableSet(subProjects);

                // parse owner's message.
                // this isn't actually too reliable, because NekoHTML changes
                // tag names to upper cases for one thing.
                Node node = dom.selectSingleNode("//DIV[@id='ownermessage']");
                if(node!=null) {
                    Element e = (Element)node;
                    ownerMessage = "";
                    List children = e.elements();
                    // skip the first H3 which is used for a caption
                    for( int i=1; i<children.size(); i++ )
                        ownerMessage += ((Element)children.get(i)).asXML();
                    ownerMessage = ownerMessage.substring(3,ownerMessage.length()-4);
                }

                return null;
            }
        }.run();
    }

    /**
     * Gets the name of this project.
     *
     * @return
     *      always non-null.
     */
    public String getName() {
        return projectName;
    }

    /**
     * Gets the one-line summary of this project.
     * such as "This is the java.net community project. All are welcome."
     *
     * @return
     *      always non-null.
     */
    public String getSummary() throws ProcessingException {
        parseProjectInfo();
        return summmary;
    }

    /**
     * Gets the owner's message. This could contain HTML.
     * <p>
     * Generally speaking, this library cannot obtain the exact char-by-char
     * representation of such HTML (for example, whitespace between attributes
     * are lost.)
     *
     * @return
     *      null if the owner message is not set. Otherwise non-null string.
     *
     * @see #getOwnerMessage2()
     */
    public String getOwnerMessage() throws ProcessingException {
        parseProjectInfo();
        return ownerMessage;
    }

    /**
     * Gets the owner's message.
     * <p>
     * This is an admin-only operation, but unlike {@link #getOwnerMessage()},
     * this method returns the exact char-by-char owner's message as currently set.
     *
     * @return
     *      null if the owner message is not set. Otherwise non-null string.
     *
     * @see #getOwnerMessage()
     */
    public String getOwnerMessage2() throws ProcessingException {
        return new Scraper<String>("Failed to get the owner message") {
            protected String scrape() throws IOException, SAXException {
                WebResponse response = wc.getResponse(_getURL()+"/servlets/ProjectEdit");

                WebForm form = response.getFormWithName("ProjectEditForm");

                return form.getParameterValue("status");
            }
        }.run();
    }

    /**
     * Sets the owner's message.
     * <p>
     * This is an admin-only operation.
     * When set, the owner's message is displayed in the project top page.
     *
     * @param msg
     *      null to reset the owner message.
     *      Otherwise the string will be set as the owner's message.
     *      Can contain HTML.
     *
     * @return
     *      the old owner's message, or null if none was set.
     */
    public String setOwnerMessage(final String msg) throws ProcessingException {
        return new Scraper<String>("Failed to set the owner message") {
            protected String scrape() throws IOException, SAXException {
                WebResponse response = wc.getResponse(_getURL()+"/servlets/ProjectEdit");

                WebForm form = response.getFormWithName("ProjectEditForm");

                String old = form.getParameterValue("status");
                form.setParameter("status",msg);

                form.submit();

                return old;
            }
        }.run();
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
     * Moves this project under the specified project.
     *
     * <p>
     * This is a priviledged operation.
     */
    public void setParent(final JNProject newParent) throws ProcessingException {
        if(newParent==null)
            throw new IllegalArgumentException();

        new Scraper("Failed to reparent the project") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = wc.getResponse(_getURL()+"/servlets/ProjectEdit");

                WebForm form = response.getFormWithName("ProjectEditForm");

                form.setParameter("parent", Util.getOptionValueFor(form,"parent",newParent.getName()));

                form.submit();

                return null;
            }
        }.run();
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
     *
     * @see #getOwners()
     */
    public Set<JNUser> getOwners() throws ProcessingException {
        parseProjectInfo();
        return owners;
    }

    /**
     * Returns the e-mail alias connected to all the current owners
     * of this project.
     *
     * <p>
     * This alias is maintained by java.net. It is always "owner@PROJECT.dev.java.net"
     *
     * @see #getOwners()
     */
    public String getOwnerAlias() {
        return "owner@"+projectName+".dev.java.net";
    }

    /**
     * Returns a set of {@link JNProject} objects that represent the sub-projects
     * of this project.
     *
     * <p>
     * Note that depending on the permission of the current account used
     * to log in to the system, you might not be able to see all the sub-projects
     *
     * @return
     *      always return non-null set. If the project doesn't have
     *      any sub-project, it returns an empty set. The set is read-only.
     */
    public Set<JNProject> getSubProjects() throws ProcessingException {
        parseProjectInfo();
        return subProjects;
    }

    /**
     * Accesses the membership section of the project.
     *
     * @return
     *      always non-null.
     */
    public JNMembership getMembership() {
        if(membership==null)
            membership = new JNMembership(this);
        return membership;
    }

    /**
     * Accesses the membership section of the project.
     *
     * @return
     *      always non-null.
     */
    public JNMailingLists getMailingLists() {
        if(mailingLists==null)
            mailingLists = new JNMailingLists(this);
        return mailingLists;
    }

    /**
     * Returns the URL of the project top page.
     *
     * @return
     *      URL that looks like "https://PROJECTNAME.dev.java.net/".
     *      Never be null.
     */
    public URL getURL() {
        try {
            return new URL(_getURL());
        } catch (MalformedURLException e) {
            // never happen
            throw new IllegalStateException(e.getMessage());
        }
    }

    /*package*/ String _getURL() {
        return "https://"+projectName+".dev.java.net";
    }

    /**
     * Obtains the top folder in the file sharing section.
     */
    public JNFileFolder getFolder( String folderPathName ) throws ProcessingException {
        return getRootFolder().getSubFolder(folderPathName);
    }

    /**
     * Returns the top folder in the document &amp; files section.
     */
    public JNFileFolder getRootFolder() {
        if(rootFolder==null)
            rootFolder = new JNFileFolder(this,null,projectName,0);
        return rootFolder;
    }

    /**
     * Obtains the object that represents the news item section.
     */
    public JNNewsItems getNewsItems() {
        if(newsItems==null)
            newsItems = new JNNewsItems(this);
        return newsItems;
    }

    /**
     * Obtains the object that represents the discussion forums section.
     */
    public JNForums getForums() {
        if(forums==null)
            forums = new JNForums(this);
        return forums;
    }

    /**
     * Approves the project.
     * <p>
     * This is an admin-only operation.
     */
    public void approve() throws ProcessingException {
        new Scraper("failed to approve project "+projectName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = wc.getResponse(_getURL()+"/servlets/ProjectApproval");

                WebTable table = response.getTableStartingWith("Project");

                int rows = table.getRowCount();

                boolean found = false;

                String approveElementName = null;

                for (int r = 1; r < rows && !found; r++ ) {
                    TableCell c1 = table.getTableCell(r, 0);

                    if (c1.getLinks().length > 0) {
                        String link = c1.getLinks()[0].getURLString();

                        if (link.equals(_getURL())) {
                            TableCell c2 = table.getTableCell(r, 3);
                            String[] names = c2.getElementNames();
                            if (names.length > 0) {
                                approveElementName = names[0];
                                found = true;
                            }
                        }
                    }
                }

                if (!found)
                    throw new ProcessingException("Unable to find project "+projectName+" in the approval page");

                // we already have the info we need to approve the project, now, do it

                WebForm form = response.getFormWithName("ProjectApprovalForm");

                form.setParameter(approveElementName, "Approve");

                response = form.submit();

                // TODO: need to treat errors here, if something goes wrong.
                return null;
            }
        }.run();
    }

    public int hashCode() {
        return projectName.hashCode();
    }
    
    public boolean equals(Object o) {
        if( o.getClass()!=this.getClass() )
            return false;
        return this.projectName.equals( ((JNProject)o).projectName );
    }

    public int compareTo(Object o) {
        JNProject that = (JNProject) o;
        return this.projectName.compareTo(that.projectName);
    }
}
