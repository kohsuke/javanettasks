package org.kohsuke.jnt;

import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * java&#x2E;net discussion forums in one project.
 *
 * @author Bruno Souza
 * @author Kohsuke Kawaguchi
 */
public final class JNForums {
    private final JNProject project;

    /**
     * List of all {@link JNForum}s. Parsed lazily.
     */
    private List forums;

    JNForums(JNProject parent) {
        this.project = parent;
    }

    /**
     * Returns all the forums in this project as a {@link List} of {@link JNForum}s.
     *
     * @return
     *      always non-null. Can be empty.
     */
    public List getForums() throws ProcessingException {
        if(forums==null)
            parse();
        return Collections.unmodifiableList(forums);
    }

    /**
     * Returns the forum of the specified name.
     * <p>
     * This method returns the {@link JNForum} from {@link #getForums()}
     * such that <tt>{@link JNForum#getName()}.equals(name)</tt>.
     *
     * @return
     *      null if no such forum is found
     */
    public JNForum getForum(String name) throws ProcessingException {
        if(forums==null)
            parse();
        for( int i=0; i<forums.size(); i++ ) {
            JNForum f = (JNForum) forums.get(i);
            if(f.getName().equals(name))
                return f;
        }
        return null;
    }

    /**
     * Creats a new forum.
     *
     * <p>
     * This is a priviledged operation.
     *
     * @param name
     *      The name of the forum to be created, such as "Wish List".
     *      Must not be null, must not be empty.
     * @param description
     *      One-line description of the forum. Must not be null.
     *
     * @return
     *      always non-null valid {@link JNForum} object that
     *      represents the forum that was created.
     */
    public JNForum createForum(final String name,final String description) throws ProcessingException {
        return (JNForum)new Scraper("Failed to create forum "+name) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = project.wc.getResponse(project.getURL()+"/servlets/ProjectForumAdd");

                WebForm form = Util.getFormWithAction(response,"/servlets/ProjectForumAdd");

                form.setParameter("name", name);
                form.setParameter("description", description);

                form.submit();

                // TODO: parse the output to check errors

                forums = null;
                parse();
                for( int i=0; i<forums.size(); i++ ) {
                    JNForum f = (JNForum)forums.get(i);
                    if(f.getName().equals(name))
                        return f;
                }

                throw new ProcessingException("Unable to find the forum "+name+" that was just created");
            }
        }.run();
    }

    /**
     * Parse the HTML page
     */
    private void parse() throws ProcessingException {
        // load all information that is on the membership pages

        forums = new ArrayList();

        new Scraper("Unable to parse the forum view page") {
            protected Object scrape() throws IOException, SAXException {
                WebResponse response = project.wc.getResponse(project.getURL()+"/servlets/ProjectForumView");
                Document dom = Util.getDom4j(response);

                Element table = (Element)dom.selectSingleNode("//DIV[@id='ProjectForumView']/TABLE");

                if (table == null)
                    // theres no forum.
                    return null;

                List rows = table.selectNodes("TR");

                // we start from row 1, since row 0 is the header row.
                for (int r=1; r<rows.size(); r++) {
                    Element row = (Element) rows.get(r);

                    Element link = (Element) row.selectSingleNode("TD[1]/A");
                    String name = link.getText();
                    String href = link.attributeValue("href");
                    int idx = href.lastIndexOf('=')+1;

                    String messageCount =  ((Element)row.elements("TD").get(1)).getTextTrim();
                    int firstSpace = messageCount.indexOf(' ');
                    int n = Integer.parseInt(messageCount.substring(0,firstSpace).trim());

                    String description = ((Element)row.elements("TD").get(2)).getTextTrim();

                    forums.add(new JNForum(project,name,Integer.parseInt(href.substring(idx)),
                        n, description ));
                }

                return null;
            }
        }.run();

    }

    /**
     * Forces the forum info to be reparsed.
     */
    /*package*/ void reset() {
        forums = null;
    }
}
