package org.kohsuke.jnt;

import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
    private List<JNMailingList> lists;

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
    public List<JNMailingList> getLists() throws ProcessingException {
        if(lists==null)     parse();
        return Collections.unmodifiableList(lists);
    }

    /**
     * Returns the mailing list of the specified name.
     * <p>
     * This method returns the {@link JNMailingList} from {@link #getLists()}
     * such that <tt>{@link JNMailingList#getName()}.equals(name)</tt>.
     *
     * @return
     *      null if no such list is found
     */
    public JNMailingList get(String name) throws ProcessingException {
        if(lists==null)
            parse();
        for( int i=0; i<lists.size(); i++ ) {
            JNMailingList f = lists.get(i);
            if(f.getName().equals(name))
                return f;
        }
        return null;
    }

    private void parse() throws ProcessingException {
        lists = new ArrayList<JNMailingList>();

        try {
            WebResponse response = wc.getResponse(project._getURL()+"/servlets/ProjectMailingListList");

            for( WebLink link : response.getLinks() ) {
                String linkTxt = link.getURLString();
                if (linkTxt.startsWith(project._getURL() + "/servlets/SummarizeList?listName=")) {
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
     *
     * This is an admin operation that requires a priviledge.
     *
     * @param listName
     *      The name of the new list. For example, "mylist". Must not be null.
     * @param description
     *      A one-line description of this list. Must not be null, must not be empty.
     *      For example, "list to discuss problems of the project management".
     * @param ownerAddress
     *      The e-mail address of the mailing list owner. This address will
     *      receive various moderation messages and etc. Must be a valid e-mail address.
     * @param prefix
     *      This prefix will be appended to all the e-mails. Can be null
     *      if you don't want the subject to be kept unmodified.
     * @param hasTrailer
     *      Provides instructions on how to unsubscribe as a footer in every message.
     *      Strongly recommended for lists with more than 100 members.
     * @param isPrivate
     *      Marking a mail list as private prevents non-project members from
     *      joining the list or viewing the archives.
     * @param type
     *      The type of the mailing list operation mode. See {@link ListType}
     *      for more details.
     * @param subscribers
     *      The list of initial subscribers. Can be null.
     * @param moderators
     *      The list of moderators, if you choose {@link ListType#MODERATED}.
     *      Must be null if the list type is other types, but must be non-null
     *      if the list type is {@link ListType#MODERATED}.
     *
     * @return
     *      return a newly created mailing list. this method never returns null.
     */
    public JNMailingList create(String listName, String description, String ownerAddress,
                                String prefix, boolean hasTrailer, boolean isPrivate,
                                ListType type, Collection subscribers,
                                Collection moderators ) throws ProcessingException {

        try {
            WebResponse response = wc.getResponse(project._getURL()+"/servlets/MailingListAdd");

            WebForm form = response.getFormWithName("MailingListAddForm");

            form.setParameter("listName", listName);
            form.setParameter("description", description);
            form.setParameter("owner", ownerAddress);
            if(prefix!=null)
                form.setParameter("prefix", prefix);
            form.setCheckbox("trailer", hasTrailer);
            form.setCheckbox("private", isPrivate);
            form.setParameter("type", type.name);
            if(subscribers!=null)
                form.setParameter("subscribers", Util.toList(subscribers,'\n'));
            if(moderators!=null)
                form.setParameter("moderators", Util.toList(moderators,'\n'));

            String result = form.submit().getText();

            // TODO: need to handle list creation erros here.
            // the errors we should handle are:
            // - missing information (we should not have that, we must garantee that in our code here!)
            // - duplicated mailing list (in this case, well get a "Mailing list already exists" on the return page

            if (result.indexOf("Mailing list already exists") != -1)  {
                throw new ProcessingException("Unable to create list: Mailing list "+listName+ " already exists.");
            }
            else if(result.indexOf("Missing field") != -1 ) {
                throw new ProcessingException("Unable to create list: missing required information.");
            }

            JNMailingList list = new JNMailingList(project,listName);
            if(lists!=null)
                lists.add(list);
            return list;
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
     * Resets the current list of {@link JNMailingList}.
     */
    /*package*/ void reset() {
        lists = null;
    }
}
