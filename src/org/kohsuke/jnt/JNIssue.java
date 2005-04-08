package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * An issue.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNIssue {
    private final JNProject project;
    private final int id;

    private final Element rawData;

    /**
     * Lazily created list of {@link Description}s.
     */
    private List<Description> descriptions;

    /**
     * A comment added to an issue.
     */
    public final class Description {
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
            return project.net.getUser(e.elementText("who"));
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

    JNIssue(JNProject project, int id) throws ProcessingException {
        this(project,id,null);
    }

    JNIssue(JNProject _project, int _id, Element rawData) throws ProcessingException {
        this.project = _project;
        this.id = _id;

        if(rawData==null) {
            // fetch now
            Document doc = bulkFetch(project,Collections.singletonList(id) );
            rawData = doc.getRootElement().element("issue");
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
        return project.net.getUser(rawData.elementText("reporter"));
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
     * Gets the component to which this issue belongs to.
     */
    public IssueVersion getComponent() {
        return new IssueVersion(rawData.elementText("version"));
    }

    public String _getPlatform() {
        return rawData.elementText("platform");
    }

    /**
     * Gets the type of the issue.
     */
    public IssueType getType() {
        return IssueType.valueOf(rawData.elementText("issue_type"));
    }

    private static final SimpleDateFormat lastModifiedFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final SimpleDateFormat creationDateFormat = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss");
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    static {
        // assume GMT. We have no clue what the time zone of java.net is, actually.
        lastModifiedFormat.setTimeZone(GMT);
        creationDateFormat.setTimeZone(GMT);
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
     *      can be an empty list but never null.
     */
    public List<Description> getDescriptions() {
        if(descriptions==null) {
            descriptions = new ArrayList<Description>();
            for( Element e : (List<Element>)rawData.elements("long_desc") )
                descriptions.add(new Description(e));
        }
        return descriptions;
    }

    private Calendar formatDate(SimpleDateFormat f, String text) {
        try {
            long t = f.parse(text).getTime();
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
            if(!issue.attributeValue("status_code").equals("200"))
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
            public Document scrape() throws IOException, SAXException {
                WebResponse rsp = project.wc.getResponse(project.getURL()+"/issues/xml.cgi?id="+idList);
                return Util.getDom4j(rsp);
            }
        }.run();
    }

    /**
     * Fetches the XML for issues updated during the specified time span.
     * See https://jaxb.dev.java.net/issues/xmlupdate.cgi
     */
    static Document bulkUpdateFetch(final JNProject project,final String queryParam) throws ProcessingException {
        return new Scraper<Document>("fetching the details of the issue xmlupdate.cgi "+queryParam) {
            public Document scrape() throws IOException, SAXException {
                WebResponse rsp = project.wc.getResponse(project.getURL()+"/issues/xmlupdate.cgi?"+queryParam);
                return Util.getDom4j(rsp);
            }
        }.run();
    }
}
