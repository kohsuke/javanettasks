package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.HttpException;

import java.util.Date;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.text.ParseException;

import org.dom4j.Document;
import org.xml.sax.SAXException;
import org.w3c.dom.DOMException;

/**
 * One announcement, such as <a href="https://jwsdp.dev.java.net/servlets/NewsItemView?newsItemID=1495">this</a>
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNNewsItem {
    private final Date timestamp;
    private final String headline;
    private final JNProject project;
    private final int id;

    JNNewsItem(JNProject owner, int id, Date timestamp, String headline) {
        this.project = owner;
        this.id = id;
        this.timestamp = timestamp;
        this.headline = headline;
    }

    /**
     * The time when this announcement was made.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * The one-line head line of this announcement.
     */
    public String getHeadline() {
        return headline;
    }

    /**
     * Gets the project to which this announcement belongs.
     */
    public JNProject getProject() {
        return project;
    }

    /**
     * Gets the news item ID.
     *
     * The news item ID uniquely identifies a news item within java.net
     */
    public int getId() {
        return id;
    }

    /**
     * Delete this announcement.
     */
    public void delete() throws ProcessingException {
        try {
            WebResponse response = project.wc.getResponse(project.getURL()+"/servlets/ProjectNewsDelete?newsItemID="+id);
            WebForm form = response.getFormWithName("projectnewsdeleteform");
            if(form==null)
                throw new IllegalStateException("missing form");
            SubmitButton submit = form.getSubmitButton("Button","Confirm delete");
            if(submit==null)
                throw new IllegalStateException("no submit button");
            form.submit(submit);
        } catch( SAXException e ) {
            throw new ProcessingException(e);
        } catch( IOException e ) {
            throw new ProcessingException(e);
        } catch( DOMException e ) {
            throw new ProcessingException(e);
        } catch(HttpException e) {
            throw new ProcessingException(e);
        }

        project.getNewsItems().resetNewsItems();
    }

    /**
     * Gets the URL to thew news item page
     */
    public URL getURL() {
        try {
            return new URL(project.getURL()+"/servlets/NewsItemView?newsItemID="+id);
        } catch (MalformedURLException e) {
            // can't happen normally
            throw new IllegalStateException();
        }
    }
}
