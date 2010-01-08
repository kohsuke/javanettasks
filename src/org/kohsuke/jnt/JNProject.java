package org.kohsuke.jnt;

import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
public final class JNProject extends JNObject implements Comparable {
    /** The project name. */
    protected final String projectName;

    //
    // lazily created
    //
    private JNMembership membership;
    private JNForums forums;
    private JNMailingLists mailingLists;
    private JNIssueTracker issueTracker;
    private Boolean exists;

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
    private JNVCS vcs;

    protected JNProject(JavaNet net, String name) {
        super(net);
        this.projectName = name;
    }

    /**
     * Returns the {@link JavaNet} object that this project belongs to.
     *
     * @return
     *      never null.
     */
    public JavaNet getConnection() {
        return root;
    }

    /**
     * Parse the project index page.
     */
    private void parseProjectInfo() throws ProcessingException {
        if(topLevelName!=null)
            return; // already parsed.

        exists = false;

        new Scraper("unable to parse the project page of "+projectName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                Document dom = Util.getDom4j(goTo(_getURL()+'/'));

                List as = dom.selectNodes("//DIV[@id='breadcrumbs']//A");
                if(as.size()==0)
                    throw new ProcessingException("failed to obtain the breadcrumb in "+getURL());

                if(as.size()>2) {
                    topLevelName = ((Element)as.get(1)).getTextTrim();
                    parentProject = ((Element)as.get(as.size()-2)).getTextTrim();
                } else {
                    topLevelName = projectName;
                    parentProject = null;
                }

                if(( dom.selectSingleNode("//TABLE[@class='axial']/TR[normalize-space(TH)='Project group'][normalize-space(TD)='communities']")!=null ) ||
                    ( projectName.equalsIgnoreCase("glassfish")) )  // hack for glassfish community
                    isCommunity=Boolean.TRUE;
                else
                    isCommunity=Boolean.FALSE;

                // parse summary
                Node summaryNode = dom.selectSingleNode("//TABLE[@class='axial']/TR[TH/text()='Summary']/TD");
                if(summaryNode!=null)
                    summmary = summaryNode.getText();

                // parse owners
                Set<JNUser> owners = new TreeSet<JNUser>();
                List<Element> os = dom.selectNodes("//TABLE[@class='axial']/TR[TH/text()='Owner(s)']/TD/A");
                for (Element o : os)
                    owners.add(root.getUser(o.getTextTrim()));
                JNProject.this.owners = Collections.unmodifiableSet(owners);

                // parse sub-projects
                Set<JNProject> subProjects = new TreeSet<JNProject>();  
                List<Element> sps = dom.selectNodes("//H2[text()='Subprojects']/following::*[1]/TR/TD/A");
                for (Element sp : sps)
                    subProjects.add(root.getProject(sp.getTextTrim()));
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

                // is this CVS, or Subversion based?
                Node vcslink = dom.selectSingleNode("//DL[@id='projecttools']//A[@href='https://" + projectName + ".dev.java.net/source/browse/" + projectName + "/']");
                if(vcslink==null)
                    throw new ProcessingException("Version control link not found");
                if(vcslink.getText().indexOf("CVS")>=0)
                    vcs = JNVCS.CVS;
                else
                    vcs = JNVCS.SVN;

                exists = true;

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
     * Gets the version control system this project uses.
     *
     * @return
     *      always non-null.
     */
    public JNVCS getVersionControl() throws ProcessingException {
        parseProjectInfo();
        return vcs;
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
            protected String scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(_getURL()+"/servlets/ProjectEdit");

                WebForm form = Util.getFormWithAction(response,"ProjectEdit");

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
            protected String scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(_getURL()+"/servlets/ProjectEdit");

                WebForm form = Util.getFormWithAction(response,"ProjectEdit");

                String old = form.getParameterValue("status");
                form.setParameter("status",msg);

                checkError(form.submit());

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
        return root.getProject(parentProject);
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
                WebResponse response = goTo(_getURL()+"/servlets/ProjectEdit");

                WebForm form = Util.getFormWithAction(response,"ProjectEdit");
                String parentName = newParent.getName();
                boolean found = false;

                String[] options = form.getOptions("parent");
                for (int i = 0; i < options.length; i++) {
                    String optionText = options[i].replace((char) 0xA0, ' ');
                    if( optionText.equals(parentName) || optionText.startsWith(parentName+' ')) {
                        form.setParameter("parent",form.getOptionValues("parent")[i]);
                        found = true;
                        break;
                    }
                }

                if(!found)
                    throw new ProcessingException("No such projcet " + parentName);

                checkError(form.submit());

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
        JNProject p = root.getProject(topLevelName);
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
       return isCommunity;
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
        if (owners.isEmpty())
            // do it the slow way, since projects can now delete the default summary table
            owners = getMembership().getUserOf(root.getRole("Project Owner"));
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

    public boolean exists() throws ProcessingException {
        if(exists==null)
            try {
                parseProjectInfo();
            } catch (ProcessingException e) {
                if(e.getMessage().contains("The dev.java.net domain contains no project named"))
                    return false;
                throw e;
            }
        return exists;
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
            return new URL(_getURL()+'/');
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
     *
     * @param folderPathName
     *      String like "/folder1/subfolder"
     * @return
     *      if the folder is not found, return null.
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
     * Obtains the object that represents the issue tracker section.
     */
    public JNIssueTracker getIssueTracker() {
        if(issueTracker==null)
            issueTracker = new JNIssueTracker(this);
        return issueTracker;
    }

    /**
     * Approves the project.
     * <p>
     * This is an admin-only operation.
     */
    public void approve() throws ProcessingException {
        new ProjectApprover(true,null).run();
    }

    /**
     * Disapproves the project.
     * <p>
     * This is an admin-only operation.
     */
    public void disapprove(String reason) throws ProcessingException {
        if(reason==null)
            throw new IllegalArgumentException();
        new ProjectApprover(false,reason).run();
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

    public String toString() {
        return projectName+" project";
    }

    private class ProjectApprover extends Scraper {
        private boolean approve;
        private String reason;

        public ProjectApprover(boolean approve, String reason) {
            super(
                approve ?
                "failed to approve project " + projectName :
                "failed to disapprove project "+projectName);

            this.approve = approve;
            this.reason = reason;
        }

        protected Object scrape() throws IOException, SAXException, ProcessingException {
            WebResponse response = goTo(_getURL()+"/servlets/ProjectApproval");

            WebTable table = response.getTableStartingWith("Project");

            int rows = table.getRowCount();

            boolean found = false;

            String approveElementName = null;

            for (int r = 1; r < rows && !found; r++ ) {
                TableCell c1 = table.getTableCell(r, 0);

                if (c1.getLinks().length > 0) {
                    String link = c1.getLinks()[0].getURLString();

                    if (link.equals(_getURL()+'/')) {
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
            WebForm form = Util.getFormWithAction(response,"ProjectApproval");

            form.setParameter(approveElementName, approve ? "Approve" : "Disapprove");
            form.setParameter("disapprovalReason",reason);

            checkError(form.submit());

            return null;
        }
    }
}
