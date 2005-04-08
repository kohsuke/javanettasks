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

/**
 * An issue.
 *
 * @author Kohsuke Kawaguchi
 */
public class JNIssue {
    private final JNProject project;
    private final int id;

    private final Element rawData;

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
