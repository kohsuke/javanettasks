/*
 * $Id: NewsItemTask.java 77 2003-12-18 16:53:55Z kohsuke $
 *  
 */
package org.kohsuke.jnt.ant;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.ProcessingException;

/**
 * Ant task for adding a news item to a java&#x2#;net project.
 * 
 * @author Ryan Shoemaker
 * @version $Revision: 77 $
 */
public class NewsItemTask extends AbstractJavaNetTaskForProject {

    private Calendar date;
    private String headline;
    private String body;
    private String imageUrl;
    private String articleUrl;

    public void setDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        this.date = Calendar.getInstance();
        Date d = sdf.parse(date, new ParsePosition(0));
        if (d == null)
            throw new BuildException("date format is invalid - must be MM/dd/yyyy");
        this.date.setTime(d);
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }

    public void addConfiguredHeadline(NestedHeadlineOrBody headline) {
        this.headline = headline.getContent();
    }

    public void addConfiguredBody(NestedHeadlineOrBody body) {
        this.body = body.getContent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.kohsuke.jnt.ant.AbstractJavaNetTaskForProject#run(org.kohsuke.jnt.JNProject)
     */
    protected void run(JNProject project)
        throws ProcessingException, BuildException {

        // headline is the only required data
        if (headline == null)
            throw new BuildException("Required headline missing - please add @headline or nested <headline>");

        logHeader();
        
        // submit the news item
        project.createNewsItem().submitNewsItemData(
            date,
            headline,
            body,
            imageUrl,
            articleUrl);
    }

    // print a status message in the log, truncating if it is too long
    private void logHeader() {
        int len = headline.length();
        if (len >= 25) {
            log(
                "submitting news item: '" + headline.substring(0, 24) + "...'",
                Project.MSG_INFO);
        } else {
            log("submitting news item: '" + headline + "'");
        }
    }

}
