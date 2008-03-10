package org.kohsuke.jnt;

import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * One announcement, such as <a href="https://jwsdp.dev.java.net/servlets/NewsItemView?newsItemID=1495">this</a>
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNNewsItem extends JNObject {
    private final Date timestamp;
    private final String headline;
    private final JNProject project;
    private final int id;

    JNNewsItem(JNProject owner, int id, Date timestamp, String headline) {
        super(owner);
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
        new Scraper("Unable to delete the announcement") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/servlets/ProjectNewsDelete?newsItemID="+id);
                WebForm form = Util.getFormWithAction(response,"ProjectNewsDelete");
                if(form==null)
                    throw new ProcessingException("missing form");
                SubmitButton submit = form.getSubmitButton("Button","Confirm delete");
                if(submit==null)
                    throw new ProcessingException("no submit button");
                checkError(form.submit(submit));

                return null;
            }
        }.run();

        project.getNewsItems().resetNewsItems();
    }

    /**
     * Approves this announcement.
     */
    public void approve() throws ProcessingException {
        moderate("Approve");
    }

    /**
     * Rejects this announcement.
     */
    public void disapprove() throws ProcessingException {
        moderate("Disapprove");
    }

    private void moderate(final String action) throws ProcessingException {
        new Scraper("Unable to moderate the announcement") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                String url = project._getURL() + "/servlets/ProjectNewsApproval";
                WebResponse response = goTo(url);
                WebForm form = Util.getFormWithAction(response, url);
                if(form==null)
                    throw new ProcessingException("missing form");
                form.setParameter("operation "+id, action);
                checkError(form.submit());

                return null;
            }
        }.run();

        project.getNewsItems().resetNewsItems();
    }

    /**
     * Gets the URL to thew news item page
     */
    public URL getURL() {
        try {
            return new URL(project._getURL()+"/servlets/NewsItemView?newsItemID="+id);
        } catch (MalformedURLException e) {
            // can't happen normally
            throw new IllegalStateException();
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JNNewsItem)) return false;

        JNNewsItem that = (JNNewsItem) o;
        return this.id == that.id && this.project==that.project;
    }

    public int hashCode() {
        return id;
    }
}
