package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.DOMReader;
import org.xml.sax.SAXException;

import java.io.IOException;
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
                throw new IllegalArgumentException("No such issue. Id="+id);
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
     * Gets the current status of this issue.
     */
    public IssueStatus getStatus() {
        return IssueStatus.valueOf(rawData.elementText("issue_status"));
    }

    /**
     * Gets the resolution of this issue.
     */
    public IssueResolution getResolution() {
        return IssueResolution.valueOf(rawData.elementText("resolution"));
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
     * @return
     *      can be an empty list but never null. Older changes first.
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
                WebResponse rsp = project.goTo(project.getURL()+"issues/xml.cgi?include_empty_issues=false&id="+idList);
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
}
