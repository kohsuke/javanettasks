package org.kohsuke.jnt;

import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * java&#x2E;net project mailing list section.
 *
 * @author Kohsuke Kawaguchi
 * @author Bruno Souza
 */
public final class JNMailingLists {
    private final JNProject project;
    private final WebConversation wc;

    /**
     * List of {@link JNMailingList}. Lazily parsed.
     */
    private List lists;

    protected JNMailingLists(JNProject project) {
        this.wc = project.wc;
        this.project = project;
    }

    /**
     * Returns all the mailing lists as a {@link List} of {@link JNMailingList}.
     *
     * <p>
     * This list will only contain mailing lists you can see.
     * The {@link JNMailingList}s are returned in the order you see in the HTML page.
     *
     * @return
     *      can be empty but never be null. read-only.
     */
    public List getLists() throws ProcessingException {
        if(lists==null)     parse();
        return Collections.unmodifiableList(lists);
    }

    private void parse() throws ProcessingException {
        lists = new ArrayList();

        try {
            WebResponse response = wc.getResponse(project.getURL()+"/servlets/ProjectMailingListList");

            WebLink[] links = response.getLinks();

            for (int i = 0; i < links.length; i++) {
                WebLink link = links[i];
                String linkTxt = link.getURLString();
                if (linkTxt.startsWith(project.getURL() + "/servlets/SummarizeList?listName=")) {
                    String listName = linkTxt.substring(linkTxt.lastIndexOf('=')+1, linkTxt.length());
                    lists.add(new JNMailingList(project,listName));
                }
            }
        } catch( SAXException e ) {
            throw new ProcessingException(e);
        } catch( IOException e ) {
            throw new ProcessingException(e);
        } catch( DOMException e ) {
            throw new ProcessingException(e);
        } catch(HttpException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Creates a new mailing list.
     */
    public JNMailingList create(String listName, String description, String ownerAddress ) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
