package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * An issue.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNIssue extends JNObject {
    // TODO: do attachments. see https://hudson.dev.java.net/issues/xml.cgi?id=1120&include_attachments=false
    private final JNProject project;
    private final int id;

    private final Element rawData;

    /**
     * Lazily created list of {@link Description}s.
     */
    private List<Description> descriptions;

    /**
     * Lazily created list of {@link Activity}s.
     */
    private List<Activity> activities;

    public abstract class Item<T extends Item> implements Comparable<Item<T>> {
        T prev,next;

        public T getPrev() {
            return prev;
        }

        public T getNext() {
            return next;
        }

        public JNIssue getParent() {
            return JNIssue.this;
        }

        /**
         * Gets the user who created this item.
         */
        public abstract JNUser getAuthor();

        /**
         * Gets the timestamp when this item was created.
         */
        public abstract Calendar getTimestamp();

        /**
         * <tt>(date of this event)-(date of the original issue report)</tt>
         * in milliseconds.
         */
        public long getAge() {
            return getTimestamp().getTimeInMillis()-getParent().getCreationDate().getTimeInMillis();
        }

        /**
         * Compare its timestamps, in ascending order.
         */
        public int compareTo(Item<T> that) {
            return this.getTimestamp().compareTo(that.getTimestamp());
        }
    }

    /**
     * A comment added to an issue.
     */
    public final class Description extends Item<Description> {
        /**
         * The 'long_desc' element.
         */
        private final Element e;

        Description(Element e) {
            this.e = e;
        }

        /**
         * Gets the user who added this comment.
         */
        public JNUser getAuthor() {
            return root.getUser(e.elementText("who"));
        }

        /**
         * Gets the timestamp when this comment was added.
         */
        public Calendar getTimestamp() {
            return formatDate(creationDateFormat, e.elementText("issue_when"));
        }

        /**
         * Gets the actual comment text.
         */
        public String getText() {
            return e.elementText("thetext");
        }
    }

    public abstract class Activity extends Item<Activity> {
        /**
         * Gets the user who performed this activity.
         */
        public abstract JNUser getAuthor();

        public abstract Calendar getTimestamp();

        /**
         * Gets the field that has changed.
         *
         * @return
         *      Null if this is the creation activity.
         */
        public abstract IssueField getField();

        /**
         * Old value before the change.
         *
         * @return
         *      Can be empty string but never null for update activity.
         *      For creation activity, null.
         */
        public abstract String getOldValue();

        /**
         * New value after the change.
         *
         * Value can be parsed with methods like {@link IssueStatus#valueOf(String)},
         * {@link IssueResolution#valueOf(String)}, {@link IssueType#valueOf(String)},
         * and {@link Priority#valueOf(String)}.
         *
         * @return
         *      Can be empty string but never null for update activity.
         *      For creation activity, null.
         */
        public abstract String getNewValue();

        /**
         * Returns true if this activity is an update activity
         * and not the creation activity.
         */
        public boolean isUpdate() {
            return getField()!=null;
        }

        /**
         * Gets the status of the issue right after this activity.
         * <p>
         * That is, if this activity updates the status, this method
         * returns that value, and otherwise it returns the status
         * of the issue when the activity happened.
         */
        public IssueStatus getCurrentStatus() {
            Activity a=this;
            for(; a!=null; a=a.getNext())
                if(a.getField()== IssueField.STATUS)
                    return IssueStatus.valueOf(a.getOldValue());
            return getParent().getStatus();
        }
    }

    public final class UpdateActivity extends Activity {
        /**
         * The 'activity' element.
         */
        private final Element e;

        UpdateActivity(Element e) {
            this.e = e;
        }

        public JNUser getAuthor() {
            return root.getUser(e.elementText("user"));
        }

        public Calendar getTimestamp() {
            return formatDate(creationDateFormat, e.elementText("when"));
        }

        public IssueField getField() {
            return IssueField.find(e.elementText("field_name"));
        }

        public String getOldValue() {
            return e.elementText("oldvalue");
        }

        public String getNewValue() {
            return e.elementText("newvalue");
        }
    }

    public final class CreationActivity extends Activity {
        public JNUser getAuthor() {
            return getReporter();
        }

        public Calendar getTimestamp() {
            return getCreationDate();
        }

        public IssueField getField() {
            return null;
        }

        public String getOldValue() {
            return null;
        }

        public String getNewValue() {
            return null;
        }
    }

    JNIssue(JNProject project, int id) throws ProcessingException {
        this(project,id,null);
    }

    JNIssue(JNProject _project, int _id, Element rawData) throws ProcessingException {
        super(_project);
        this.project = _project;
        this.id = _id;

        if(rawData==null) {
            // fetch now
            Document doc = bulkFetch(project,Collections.singletonList(id) );
            rawData = doc.getRootElement().element("issue");
            if(rawData==null)
                throw new ProcessingException("No such issue. Id="+id);
        }

        this.rawData = rawData;
    }

    /**
     * Returns the raw XML data that describes this issue.
     *
     * <p>
     * See https://javanettasks.dev.java.net/issues/issuezilla.dtd for the format.
     *
     * @return
     *      the "issue" element.
     */
    public Element getRawData() {
        return rawData;
    }

    /**
     * Gets the issue ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the project that this issue is filed for.
     */
    public JNProject getProject() {
        return project;
    }

    /**
     * Gets the priority of this issue.
     */
    public Priority getPriority() {
        return Priority.valueOf(rawData.elementTextTrim("priority"));
    }

    /**
     * Gets the short description of this issue.
     *
     * A "short description" is a summary of the issue. For example,
     * the short description of https://java-ws-xml-incubator.dev.java.net/issues/show_bug.cgi?id=1
     * is "Please graduate me... I have grown up now..."
     *
     */
    public String getShortDescription() {
        return rawData.elementText("short_desc");
    }

    /**
     * Gets the reporter of this issue.
     */
    public JNUser getReporter() {
        return root.getUser(rawData.elementText("reporter"));
    }

    /**
     * Gets the current person assigned to the issue.
     *
     * <p>
     * This can be an user name or an alias name, like "issues@hudson".
     * It's not clear exactly what's allowed and what's not.
     */
    public String getAssignedTo() {
        return rawData.elementText("assigned_to");
    }

    /**
     * Gets the current status of this issue.
     */
    public IssueStatus getStatus() {
        return IssueStatus.valueOf(rawData.elementText("issue_status"));
    }

    /**
     * Gets the 'status whiteboard' text, or null if not set.
     */
    public String getStatusWhiteboard() {
        String s = rawData.elementText("status_whiteboard");
        if(s.length()==0)   return null;
        return s;
    }

    /**
     * Gets the resolution of this issue.
     */
    public IssueResolution getResolution() {
        String s = rawData.elementText("resolution");
        if(s.length()==0) return null;
        return IssueResolution.valueOf(s);
    }

    /**
     * Gets the version to which this issue belongs to.
     */
    public IssueVersion getVersion() {
        return new IssueVersion(rawData.elementText("version"));
    }

    public String _getPlatform() {
        return rawData.elementText("rep_platform");
    }

    public String getComponent() {
        return rawData.elementText("component");
    }

    public String getSubComponent() {
        return rawData.elementText("subcomponent");
    }

    /**
     * Gets the number of votes on this issue.
     */
    public int getVotes() {
        try {
            return Integer.parseInt(rawData.elementText("votes"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Gets the type of the issue.
     */
    public IssueType getType() {
        return IssueType.valueOf(rawData.elementText("issue_type"));
    }

    private static final SimpleDateFormat lastModifiedFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final SimpleDateFormat creationDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final TimeZone PST = TimeZone.getTimeZone("PST");
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    static {
        // apparently java.net servers are in PST,
        // and when they send time stamps they send local time
        lastModifiedFormat.setTimeZone(PST);
        creationDateFormat.setTimeZone(PST);
    }

    /**
     * Gets the last modified date of this issue.
     */
    public Calendar getLastModified() {
        return formatDate(lastModifiedFormat,rawData.elementText("delta_ts"));
    }

    /**
     * Gets the timestamp when this issue was created.
     */
    public Calendar getCreationDate() {
        return formatDate(creationDateFormat,rawData.elementText("creation_ts"));
    }

    /**
     * Gets the description of issues (AKA "additional comments")
     *
     * <p>
     * This also includes the initial bug report, which is the first element.
     *
     * @return
     *      Never empty, never null. Older changes first.
     */
    public List<Description> getDescriptions() {
        if(descriptions==null) {
            descriptions = new ArrayList<Description>();
            for( Element e : (List<Element>)rawData.elements("long_desc") )
                descriptions.add(new Description(e));
            makeDoubeLinkedList(descriptions);
        }
        return descriptions;
    }

    /**
     * Gets the activities of issues.
     * This list represents status changes made to the issue.
     *
     * <p>
     * The first activity is always {@link CreationActivity}
     * indicating the creation of this issue.
     *
     * @return
     *      always non-empty list. Older changes first.
     */
    public List<Activity> getActivities() {
        if(activities==null) {
            activities = new ArrayList<Activity>();
            activities.add(new CreationActivity());
            for( Element e : (List<Element>)rawData.elements("activity") )
                activities.add(new UpdateActivity(e));
            makeDoubeLinkedList(activities);
        }
        return activities;
    }

    /**
     * Persists this issue to the disk.
     *
     * @see JNIssueTracker#load(int, InputStream)  
     */
    public void save(OutputStream out) throws IOException {
        new XMLWriter(out).write(rawData);
    }

    private <T extends Item<T>> void makeDoubeLinkedList(List<T> items) {
        T prev = null;
        for (T t : items) {
            t.prev = prev;
            if(prev!=null)
                prev.next = t;
            prev = t;
        }
        if(prev!=null)
            prev.next = null;
    }

    private Calendar formatDate(SimpleDateFormat f, String text) {
        try {
            long t = f.parse(text).getTime();
            // when returning a calendar, set the time zone to GMT
            // so that user applications won't be affected by the server's timezone.
            GregorianCalendar c = new GregorianCalendar(GMT);
            c.setTimeInMillis(t);
            return c;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Update the issue by just posting a comment.
     */
    public void update(final String comment) throws ProcessingException {
        beginEdit().commit(comment);
    }

    /**
     * Begins updating an issue.
     *
     * See {@link IssueEditor} for more details. You make a bunch of edits through
     * {@link IssueEditor}, then submit it via {@link IssueEditor#commit(String)}.
     */
    public IssueEditor beginEdit() {
        return new IssueEditor(this);
    }

    static Map<Integer,JNIssue> bulkCreate(JNProject project, Document doc) throws ProcessingException {
        Map<Integer,JNIssue> r = new TreeMap<Integer, JNIssue>();

        for( Element issue : (List<Element>)doc.getRootElement().elements("issue") ) {
            // make sure that the issue id is correct
            int id = Integer.parseInt(issue.elementTextTrim("issue_id"));
            String status = issue.attributeValue("status_code");
            if(status.equals("404"))
                continue;   // not found
            if(!status.equals("200"))
                throw new ProcessingException("bad status code for "+id+" : "+issue.attributeValue("status_message"));
            r.put(id,project.getIssueTracker().getOrCreate(id,issue));
        }
        return r;
    }

    /**
     * Fetchs the XML for all the specified issues.
     */
    static Document bulkFetch(final JNProject project, List<Integer> ids) throws ProcessingException {
        StringBuffer buf = new StringBuffer();
        for( int i : ids ) {
            if(buf.length()>0)
                buf.append(',');
            buf.append(i);
        }

        final String idList = buf.toString();

        return new Scraper<Document>("fetching the details of the issue "+idList) {
            public Document scrape() throws IOException, SAXException, ProcessingException {
                WebResponse rsp = project.goTo(project.getURL()+"issues/"+ XML_CGI +"?include_empty_issues=false&include_attachments=false&id="+idList);
                return new DOMReader().read(rsp.getDOM());
            }
        }.run();
    }

    /**
     * Fetches the XML for issues updated during the specified time span.
     * See https://jaxb.dev.java.net/issues/xmlupdate.cgi
     */
    static Document bulkUpdateFetch(final JNProject project,final String queryParam) throws ProcessingException {
        return new Scraper<Document>("fetching the details of the issue xmlupdate.cgi "+queryParam) {
            public Document scrape() throws IOException, SAXException, ProcessingException {
                WebResponse rsp = project.goTo(project.getURL()+"issues/xmlupdate.cgi?"+queryParam);
                return new DOMReader().read(rsp.getDOM());
            }
        }.run();
    }

     /**
     * Fetches the XML for issues by running stored query, runs default query if the
      * queryName parameter is null
     */
    static Document bulkQueryFetch(final JNProject project,final String queryName) throws ProcessingException {
        return new Scraper<Document>("fetching the details of the issue buglist.cgi "+queryName) {
            public Document scrape() throws IOException, SAXException, ProcessingException {
                String query = (queryName == null ? "runuserdefault" : "runnamed&namedcmd="+queryName);
                WebResponse rsp = project.goTo(project.getURL()+"issues/buglist.cgi?&cmdtype="+query+"&format=xml");
                return new DOMReader().read(rsp.getDOM());
            }
        }.run();
    }

    /*package*/ static final String XML_CGI = System.getProperty("java.net.xml.cgi","xml.cgi");
}
