package org.kohsuke.jnt;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebForm;

/**
 * java&#x2E;net discussion forum.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNForum {
    private final JNProject project;
    private final String name;
    private final int id;
    private final int messageCount;
    private final String description;

    public JNForum(JNProject project, String name, int id, int messageCount, String description) {
        this.project = project;
        this.name = name;
        this.id = id;
        this.messageCount = messageCount;
        this.description = description;
    }

    /**
     * Gets the project to which this forum belongs.
     *
     * @return
     *      always non-null.
     */
    public JNProject getProject() {
        return project;
    }

    /**
     * Gets the name of this forum, such as "Wish List".
     *
     * @return
     *      always non-null.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the unique ID of this forum.
     *
     * <p>
     * The forum ID is internally used by the java.net system to
     * identify a forum.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the total number of messages in this forum.
     */
    public int getMessageCount() {
        return messageCount;
    }

    /**
     * Gets the one-line description of this forum.
     *
     * @return
     *      always non-null.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the URL that points to the top page of this forum,
     * such as <tt>https://java-net.dev.java.net/servlets/ForumMessageList?forumID=93</tt>.
     *
     * @return
     *      always non-null valid URL.
     */
    public URL getURL() {
        try {
            return new URL(project.getURL()+"/servlets/ForumMessageList?forumID="+id);
        } catch (MalformedURLException e) {
            // this shall never happen
            throw new IllegalStateException();
        }
    }

    /**
     * Deletes this mailing list.
     *
     * <p>
     * This is a priviledged operation.
     */
    public void delete() throws ProcessingException {
        new Scraper("Unable to delete forum "+name) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = project.wc.getResponse(project.getURL()+"/servlets/ProjectForumDelete?forumID="+id);
                WebForm form = Util.getFormWithAction(response,"ProjectForumDelete");
                form.submit();
                return null;
            }
        }.run();

        project.getForums().reset();
    }

    /**
     * Subscribes yourself.
     */ 
    public void subscribe() throws ProcessingException {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
