package org.kohsuke.jnt;

import org.dom4j.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

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

    /**
     * Parses the information from the TR element.
     */
    protected JNFile( JNFileFolder folder, Element tr ) throws ProcessingException, ParseException, MalformedURLException {
        this.folder = folder;

        Element anchor = (Element)tr.selectSingleNode("TD[1]//A");  // XPath is 1-origin
        href = new URL(anchor.attributeValue("href"));
        name = anchor.getTextTrim();

        String statusText = ((Element) tr.elements("TD").get(1)).getTextTrim();
        status = FileStatus.parse( statusText );
        if(status==null)
            throw new ProcessingException("Unable to parse the status " + statusText);

        String modifiedByCell = ((Element) tr.elements("TD").get(2)).getTextTrim();

        // temp is a string in the format:
        //    brunos on Sunday, December 21, 2003
        //    <user> on <date------------------->
        // we are not even treating errors here... no idea of what will happen if this changes...

        int firstSpace = modifiedByCell.indexOf(' ');

        modifiedBy = folder.project.net.getUser( modifiedByCell.substring(0, firstSpace) );

        lastModified = LONG_FORMAT.parse(modifiedByCell.substring(firstSpace + 4));

        description = ((Element) tr.elements("TD").get(5)).getTextTrim();
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


    private static final DateFormat LONG_FORMAT = DateFormat.getDateInstance(DateFormat.FULL,Locale.ENGLISH);
}
