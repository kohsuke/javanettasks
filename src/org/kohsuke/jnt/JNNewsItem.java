/*
 * $Id: JNNewsItem.java 56 2003-12-17 05:40:20Z ryan_shoemaker $
 * 
 */
package org.kohsuke.jnt;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import org.xml.sax.SAXException;

import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;

/**
 * java&#x2#;net project news item.
 * 
 * TODO: add support for advanced news item options
 * 
 * @author Ryan Shoemaker
 * @version $Revision: 56 $
 */
public class JNNewsItem {

    // parent project reference
    private final JNProject project;

    // web conversation objects used to interact with the news item form
    private final WebConversation wc;

    // url for this news item
    private final String url;

    // data for the form
    private Calendar date;
    private String headline;
    private String body;
    private String imageUrl;
    private String articleUrl;

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

    protected JNNewsItem(JNProject project, String url) {
        this.wc = project.wc;
        this.project = project;
        this.url = url;
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
    public void submitNewsItemData(
        Calendar date,
        String headline,
        String body,
        String imageUrl,
        String articleUrl)
        throws ProcessingException {

        try {
            setCurrentPage();

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
    public void submitNewItemData(String headline) throws ProcessingException {
        submitNewsItemData(null, headline, null, null, null);
    }

    /**
     * Moves to the URL of the news item if necessary
     */
    private void setCurrentPage() throws IOException, SAXException {
        if (wc.getCurrentPage().getURL().toExternalForm().equals(url))
            return;
        wc.getResponse(url);
    }

}
