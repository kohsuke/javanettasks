/*
 * $Id: JNNewsItems.java 272 2004-12-19 20:08:23Z kohsuke $
 * 
 */
package org.kohsuke.jnt;

import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * java&#x2E;net project news item section.
 * 
 * TODO: add support for advanced news item options
 * 
 * @author Ryan Shoemaker
 * @author Bruno Souza
 * @version $Revision: 272 $
 */
public final class JNNewsItems {

    // parent project reference
    private final JNProject project;

    // web conversation objects used to interact with the news item form
    private final WebConversation wc;

    /**
     * List of {@link JNNewsItem}s.
     * Lazily parsed.
     */
    private List<JNNewsItem> newsItems;

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
        final Calendar date,
        final String headline,
        final String body,
        final String imageUrl,
        final String articleUrl)
        throws ProcessingException {

        new Scraper("Unable to submit news "+headline) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                // move to the submission page
                String url = project._getURL()+"/servlets/ProjectNewsAdd";
                WebResponse resp = wc.getResponse(url);

                WebForm form = resp.getFormWithName(FORM_NAME);
                SubmitButton submitButton =
                    form.getSubmitButton(FORM_BUTTON, "Add new announcement");
                if(submitButton==null)
                    throw new ProcessingException();

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

                return null;
            }
        }.run();

        // purge the news item list. we just added one, so we need to reload
        newsItems = null;
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
     * Gets all announcements in this project as a {@link List} of {@link JNNewsItem}s.
     *
     * <p>
     * The returned list is sorted in the order of date; the latest announcement is the first entry.
     *
     * @return
     *      can be empty but never be null. Read-only.
     */
    public List<JNNewsItem> getNewsItems() throws ProcessingException {
        if (newsItems == null)
            loadNewsInfo();
        return Collections.unmodifiableList(newsItems);
    }


    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM,Locale.ENGLISH);

    private void loadNewsInfo() throws ProcessingException {
        // load all information that is on the membership pages
        newsItems = new ArrayList<JNNewsItem>();

        new Scraper("Unable to parse the announcement list") {
            protected Object scrape() throws IOException, SAXException, ParseException {
                WebResponse response = project.wc.getResponse(project._getURL()+"/servlets/ProjectNewsList");
                Document dom = Util.getDom4j(response);

                Element table = (Element)dom.selectSingleNode("//FORM[@name='ProjectNewsListForm']/TABLE");

                if (table== null) {
                    // theres no news table.
                    return null;
                }

                List rows = table.selectNodes("TR");

                // we start from row 1, since row 0 is the header row.


                for (int r=1; r<rows.size(); r++) {
                    Element row = (Element) rows.get(r);
                    String date =  ((Element)row.elements("TD").get(0)).getTextTrim();

                    Element link = (Element) row.selectSingleNode("TD[2]/A");
                    String summary = link.getText();
                    String href = link.attributeValue("href");
                    int idx = href.lastIndexOf('=')+1;

                    newsItems.add(new JNNewsItem(project, Integer.parseInt(href.substring(idx)),
                        dateFormat.parse(date), summary));
                }

                // this code may be needed for pagination in the future...
//        WebLink nextPage = response.getLinkWith("Next");
//
//        if (nextPage != null) {
//            // load next page
//            parseNewsPageInfo(nextPage.click());
//        }
                return null;
            }
        }.run();
    }

    /**
     * Resest the list of news items and force reloading.
     */
    void resetNewsItems() {
        newsItems = null;
    }
}
