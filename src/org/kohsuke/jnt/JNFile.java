package org.kohsuke.jnt;

import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;

import com.meterware.httpunit.WebResponse;

/**
 * File in the documents &amp; files section.
 *
 * @author Kohsuke Kawaguchi
 * @author Bruno Souza
 */
public final class JNFile {
    private final JNFileFolder folder;
    private final String name;
    private final URL href;
    private final FileStatus status;
    private final JNUser modifiedBy;
    private final Date lastModified;
    private final String description;

    private final int id;

    /**
     * Parses the information from the TR element.
     */
    protected JNFile( JNFileFolder folder, Element tr ) throws ProcessingException, ParseException, MalformedURLException {
        this.folder = folder;

        Element anchor = (Element)tr.selectSingleNode("TD[1]//A");  // XPath is 1-origin
        href = new URL(folder.project.getURL(),anchor.attributeValue("href"));
        name = anchor.getTextTrim();

        String statusText = ((Element) tr.elements("TD").get(1)).getTextTrim();
        status = FileStatus.parse( statusText );
        if(status==null)
            throw new ProcessingException("Unable to parse the status " + statusText);


        Element td2 = (Element) tr.elements("TD").get(2);

        modifiedBy = folder.project.net.getUser( td2.element("A").getTextTrim() );

        lastModified = LONG_FORMAT.parse(td2.getTextTrim().substring(3));   // trim off the first "on "

        description = ((Element) tr.elements("TD").get(5)).getTextTrim();

        Element infoLink = (Element)tr.selectSingleNode("TD[7]//A");  // XPath is 1-origin
        String href = infoLink.attributeValue("href");

        final String param = "?documentID=";
        int idx = href.indexOf(param);
        if(idx==-1)
            throw new ProcessingException("Unable to parse the document ID");

        href = href.substring(idx+param.length());
        idx = href.indexOf('&');
        if(idx>0)
            href = href.substring(0,idx);

        id = Integer.parseInt(href);
    }

    /**
     * Gets the folder to which this file belongs.
     *
     * @return
     *      never be null.
     */
    public JNFileFolder getFolder() {
        return folder;
    }

    /**
     * The name of the file.
     *
     * @return
     *      never be null.
     */
    public String getName() {
        return name;
    }

    /**
     * The location of the file.
     *
     * @return
     *      never be null.
     */
    public URL getURL() {
        return href;
    }

    /**
     * File status.
     *
     * @return
     *      never be null.
     */
    public FileStatus getStatus() {
        return status;
    }

    /**
     * The person who modified this file.
     *
     * @return
     *      never be null.
     */
    public JNUser getModifiedBy() {
        return modifiedBy;
    }

    /**
     * The timestamp when this file was modifieid.
     *
     * @return
     *      never be null.
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * A human-readable description of this file.
     *
     * @return
     *      never be null.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the unique ID that distinguishes this document.
     * <p>
     * It's not clear to me whether the scope of the uniqueness is within
     * a project or the whole java.net. I suspect the latter.
     */
    public int getId() {
        return id;
    }

    /**
     * Removes this file.
     */
    public void delete() throws ProcessingException {
        new Scraper("error deleting file "+name) {
            protected Object scrape() throws IOException, SAXException {
                WebResponse r = folder.wc.getResponse(
                    folder.project._getURL()+"/servlets/ProjectDocumentDelete?documentID="+id+"&maxDepth=");

                r = r.getFormWithName("ProjectDocumentDeleteForm").submit();

                folder.reset();
                return null;
            }
        }.run();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JNFile)) return false;

        final JNFile that = (JNFile) o;

        return this.id == that.id && this.folder.equals(that.folder);

    }

    public int hashCode() {
        return id + folder.hashCode()*29;
    }

    private static final DateFormat LONG_FORMAT = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm:ss aa",Locale.ENGLISH);

    static {
        LONG_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
