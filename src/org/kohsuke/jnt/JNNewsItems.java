/*
 * $Id: JNNewsItems.java 79 2003-12-18 17:16:53Z kohsuke $
 * 
 */
package org.kohsuke.jnt;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.xml.sax.SAXException;

import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;

/**
 * java&#x2#;net project news item section.
 * 
 * TODO: add support for advanced news item options
 * 
 * @author Ryan Shoemaker
 * @version $Revision: 79 $
 */
public final class JNNewsItems {

    // parent project reference
    private final JNProject project;

    // web conversation objects used to interact with the news item form
    private final WebConversation wc;

    // constants for the form field names
    private static final String FORM_NAME = "ProjectNewsAddForm";
    private static final String FORM_YEAR = "eventYear";
    private static final String FORM_MONTH = "eventMonth";
    private static final String FORM_DAY = "eventDay";
    private static final String FORM_HEADLINE = "headline";
    private static final String FORM_BODY = "body";
    private static final String FORM_IMAGEURL = "imageUrl";
    private static final String FORM_ARTICLEURL = "articleUrl";
    private static final String FORM_BUTTON = "Button";
    private static final String FORM_SUBMIT = "Add news item";
    private static final String FORM_ADVANCED = "Advanced options";

    protected JNNewsItems(JNProject project) {
        this.wc = project.wc;
        this.project = project;
        //System.out.println("1: " + url);
    }

    /**
     * Submit a news item.
     * 
     * The only required parameter is headline.
     * 
     * @param date
     *                Optional Calendar object representing the date the news
     *                item will be posted. if this parameter is null, the
     *                current date will be used and the news item will be
     *                posted immediately.
     * @param headline
     *                The news item headline - must not be null, HTML markup
     *                allowed
     * @param body
     *                Optional content for the body of the news message (HTML
     *                markup allowed and encouraged)
     * @param imageUrl
     *                Optional image url field
     * @param articleUrl
     *                Optional original article url
     * @throws ProcessingException
     *                 if anything is wrong with the parameter data or an error
     *                 occurs during the form submission
     */
    public void createNewsItem(
        Calendar date,
        String headline,
        String body,
        String imageUrl,
        String articleUrl)
        throws ProcessingException {

        try {
            // move to the submission page
            String url = project.getURL()+"/servlets/ProjectNewsAdd";
            if (!wc.getCurrentPage().getURL().toExternalForm().equals(url))
                wc.getResponse(url);
            WebResponse resp = wc.getCurrentPage();
            
            WebForm form = resp.getFormWithName(FORM_NAME);
            SubmitButton submitButton =
                form.getSubmitButton(FORM_BUTTON, FORM_SUBMIT);

            if (headline == null)
                throw new ProcessingException("null headline");
            form.setParameter(FORM_HEADLINE, headline);

            if (date != null) {
                // TODO: error check that (date >= today) && (date <= 12/31/04)
                form.setParameter(
                    FORM_YEAR,
                    String.valueOf(date.get(Calendar.YEAR)));
                // the month field is set as an int even though the drop down
                // menu contains month names
                form.setParameter(
                    FORM_MONTH,
                    String.valueOf(date.get(Calendar.MONTH) + 1));
                form.setParameter(
                    FORM_DAY,
                    String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
            }

            if (body != null) {
                form.setParameter(FORM_BODY, body);
            }

            if (imageUrl != null) {
                form.setParameter(FORM_IMAGEURL, imageUrl);
            }

            if (articleUrl != null) {
                form.setParameter(FORM_ARTICLEURL, articleUrl);
            }

            // submit the news item
            form.submit(submitButton);
        } catch (IOException ioe) {
            throw new ProcessingException(ioe);
        } catch (SAXException se) {
            throw new ProcessingException(se);
        }

    }

    /**
     * Set the headline field of the news item.
     * 
     * The headline is the only required field on the form. In this case, the
     * news item will contain the specified headline and default to the current
     * date.
     * 
     * @param headline
     *                the headline
     */
    public void createNewsItem(String headline) throws ProcessingException {
        createNewsItem(null, headline, null, null, null);
    }
    
    /**
     * Returns all the news items. 
     */
    public List getNewsItems() {
        throw new UnsupportedOperationException();
    }
}
