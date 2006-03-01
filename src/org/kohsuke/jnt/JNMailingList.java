package org.kohsuke.jnt;

import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A mailing list of a project on java&#x2E;net.
 *
 * @author Bruno Souza
 * @author Kohsuke Kawaguchi
 */
public final class JNMailingList extends JNObject {
    private final JNProject project;
    private final String name;

    /**
     * Description of this list. Lazily parsed.
     */
    private String description;
    /**
     * Number of total messages. Lazily parsed.
     */
    private Integer totalMessages;

    /**
     * Subscribers as {@link String}s. Lazily parsed.
     * When non-null, eachc {@link List} is unmodifiable.
     */
    private final List<String>[] subscribers = new List[SubscriptionMode.values().length];

    /**
     * The number of messages posted per month.
     */
    public static class MessagePerMonth {
        public final String month;
        public final int count;

        public MessagePerMonth(String month, int count) {
            this.month = month;
            this.count = count;
        }
    }

    /**
     * List of {@link MessagePerMonth}. Lazily parsed.
     */
    private List<MessagePerMonth> messagesPerMonth;


    JNMailingList(JNProject project, String name) {
        super(project);
        this.project = project;
        this.name = name;
    }

    /**
     * Gets the project to which this mailing list belongs.
     */
    public JNProject getProject() {
        return project;
    }

    /**
     * Gets the name of ths mailing list, such as "issues" or "cvs".
     *
     * @return never null.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the e-mail address to post to this list.
     *
     * @return
     *      a string that looks like 'cvs@jaxb.dev.java.net' for example.
     */
    public String getPostAddress() {
        return name+'@'+project.getName()+".dev.java.net";
    }

    /**
     * Gets the description of this list.
     *
     * <p>
     * In java.net, every mailing list can have a human-readable description associated with it.
     * This method returns it.
     *
     * <p>
     * TODO: check if the description can contain HTML.
     *
     * @return
     *      non-null valid string.
     */
    public String getDescription() throws ProcessingException {
        if (description == null)
            parseListInfo();

        return description;
    }

    /**
     * Gets the total number of messages this list holds.
     */
    public int getTotalMessages() throws ProcessingException {
        if(totalMessages==null)
            parseListInfo();
        return totalMessages;
    }

    /**
     * Gets the list of subscriber e-mail addresses.
     *
     * <p>
     * This is an admin operation that requires a priviledge.
     *
     * @return
     *      read-only non-null (but possibly empty) list.
     */
    public List<String> getSubscribers( SubscriptionMode mode ) throws ProcessingException {
        if(subscribers[mode.index]==null)
            parseSubscribers(mode);
        return subscribers[mode.index];
    }

    /**
     * Subscribes yourself.
     */
    public void subscribe( final SubscriptionMode mode ) throws ProcessingException {
        new Scraper<Void>("Unable to subscribe to "+name) {
            protected Void scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/servlets/ProjectMailingListList");
                for (WebForm form : response.getForms()) {
                    if(!form.getName().equals("ProjectMailingListListForm"))
                        continue;
                    if(!form.getParameterValue("listName").equals(name))
                        continue;

                    // found the form
                    SubmitButton sb = form.getSubmitButtons()[0];
                    if(!sb.getValue().equals("Subscribe"))
                        throw new ProcessingException("Found "+sb.getValue()+" but expected Subscribe");

                    // set the mode
                    form.setParameter("subtype",mode.getNameAsWord());

                    checkError(form.submit(sb));
                    return null;
                }

                throw new ProcessingException("no subscription form found");
            }
        }.run();
    }

    /**
     * Subscribes the specified e-mail address to this mailing list.
     *
     * <p>
     * Doing this requires that you have a moderator permission on this mailing list.
     *
     * @param mode
     *      the subscription mode that the specified e-mail address uses.
     */
    public int massSubscribe(String address, SubscriptionMode mode ) throws ProcessingException {
        return massSubscribe(new String[]{address},mode,null);
    }

    /**
     * Subscribes the specified e-mail addresses to this mailing list.
     *
     * <p>
     * Convenience method to invoke {@link #massSubscribe(Collection,SubscriptionMode,ProgressCallback)}
     */
    public int massSubscribe(String[] addresses, SubscriptionMode mode, ProgressCallback callback ) throws ProcessingException {
        return doMassSubscribe(Arrays.asList(addresses), mode, callback);
    }

    /**
     * Subscribes the specified e-mail addresses to this mailing list.
     *
     * <p>
     * Doing this requires that you have a moderator permission on this mailing list.
     *
     * @param addresses
     *      collection of e-mail address of a {@link Collection} of {@link String}s.
     * @param mode
     *      the subscription mode that the specified e-mail address uses.
     * @param callback
     *      If not null, this interface receives progress indication.
     */
    public int massSubscribe(Collection<String> addresses, SubscriptionMode mode, ProgressCallback callback ) throws ProcessingException {
        return doMassSubscribe(addresses, mode, callback);
    }

    /**
     * Unsubscribes the specified e-mail addresses from this mailing list.
     *
     * <p>
     * Convenience method to invoke {@link #massUnsubscribe(Collection,SubscriptionMode,ProgressCallback)}
     */
    public int massUnsubscribe(String[] addresses, SubscriptionMode mode, ProgressCallback callback ) throws ProcessingException {
        return massUnsubscribe(Arrays.asList(addresses),mode,callback);
    }

    /**
     * Unsubscribes the specified e-mail addresses from this mailing list.
     *
     * <p>
     * Doing this requires that you have a moderator permission on this mailing list.
     *
     * @param addresses
     *      collection of e-mail address of a {@link Collection} of {@link String}s.
     * @param mode
     *      the subscription mode that the specified e-mail addresses use.
     * @param callback
     *      If not null, this interface receives progress indication.
     */
    public int massUnsubscribe(Collection<String> addresses, SubscriptionMode mode, ProgressCallback callback ) throws ProcessingException {
        return doMassUnsubscribe(addresses,mode);
    }

    /**
     * Deletes this mailing list.
     *
     * <p>
     * This is a priviledged operation.
     */
    public void delete() throws ProcessingException {
        new Scraper("Unable to delete mailing list "+name) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/servlets/MailingListDelete?list="+name);
                WebForm form = Util.getFormWithAction(response,"MailingListDelete");
                if(form==null)
                    throw new ProcessingException("form not found");
                checkError(form.submit());
                return null;
            }
        }.run();

        project.getMailingLists().reset();
    }


    /**
     * Returns a list of {@link MessagePerMonth}.
     *
     * <p>
     * Becaus of the way java.net is designed, this information is cheap to obtain.
     *
     * @return
     *      always non-null (but possibly empty) list of {@link MessagePerMonth}.
     */
    public List<MessagePerMonth> getMessagesPerMonth() throws ProcessingException {
        if(messagesPerMonth==null)  parseListInfo();
        return Collections.unmodifiableList(messagesPerMonth);
    }


    private void parseListInfo() throws ProcessingException {
        new Scraper("Unable to parse the mailing list summary page") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/servlets/SummarizeList?listName="+name);

                WebTable listInfo = response.getTableStartingWith("List address");

                if (listInfo == null)
                    throw new ProcessingException("Unable to find the list table");

                description = listInfo.getCellAsText(1,1);
                totalMessages = new Integer(listInfo.getCellAsText(2,1));

                WebTable monthInfo = response.getTableStartingWith("Month");

                messagesPerMonth = new ArrayList<MessagePerMonth>();

                if(monthInfo==null) {
                    if(totalMessages==0)
                        return null; //this is to be expected
                    else
                        throw new ProcessingException("month table not found, even though the total message count isn't 0");
                }

                int numRows = monthInfo.getRowCount();

                // we start at row 1, since the row 0 is the header
                for (int r = 1; r < numRows; r++) {
                    String month = monthInfo.getCellAsText(r,0).trim();
                    String messages = monthInfo.getCellAsText(r,1).trim();
                    messagesPerMonth.add( new MessagePerMonth(month,Integer.parseInt(messages)) );
                }

                return null;
            }
        }.run();
    }

    private int doMassSubscribe(Collection<String> addresses, SubscriptionMode mode, ProgressCallback callback ) throws ProcessingException {

//        if (!validateEmails(addresses)) {
//            System.out.println("There are errors in the e-mails, aborting subscriptions.");
//            return 0;
//        }

        /* ATENTION:

        There's a bug in the form on java.net. It has been reported more then
        once, and no action was taken so far. The form is submitted by methot
        GET, that has a limitation on the server on the amount of bytes
        that can be sent. That makes the handling of large mailing lists
        a pain, since you can't massSubscribe/unsubscribe more then +/- 300
        e-mails at once. This is a problem, specially when you need to import
        large lists.

        Experiences show that in the java.net server, you can send a URL with
        7895 characters, and the URL takes about 116 of those.

        That amonts to 7152 characters of _emails_ only.

        Since each @ is trasformed into "%40", if we have 300 e-mails sent,
        we have 600 characters of overhead.

        So, for a bit of safety, we will assume that the total amount of
        e-mails we can send at once is such that:

        NumberOfBytes + 2* NumberOfEmais < 7000.

        The < 7000 proved correct so far. On manual imports, I was able to get up
        to 304 e-mails imported at once. With the 7000 limit, the system imported
        up to 297 e-mails at once. Good enough, while garanteeing to work.

        This calculation worked on importing a 1500+ mailing list and then
        another large list with 3800+ e-mails into java.net.

        */

        // if we have less than 250 e-mails, don't worry, it will work, unless
        // we have a very unusual list of _very_ long e-mails... I won't worry
        // with this here :-)
        if (addresses.size() < 250 ) {
            return doMassSubscribe2(addresses,mode);
        }

        // else, we will have to handle the e-mail in batches

        int totalSubscribed = 0;

        List<String> batch = new ArrayList<String>(400);
        Iterator itr = addresses.iterator();

        while (itr.hasNext()) {
            batch.clear();
            int bytes = 0;

            while( bytes + 2*batch.size() < 7000 && itr.hasNext()) {
                String address = (String) itr.next();
                batch.add(address);
                bytes += address.length();
            }

            totalSubscribed += doMassSubscribe2(batch,mode);

            if(callback!=null)
                callback.onProgress(totalSubscribed,addresses.size());
        }

        return totalSubscribed;
    }

    /**
     * Appliation can implement this interface to receive
     * progress reports on subscribing e-mail addresses to
     * a mailing list.
     */
    public static interface ProgressCallback {
        /**
         * Called every once in a while to report a progress.
         *
         * @param current
         *      The number of addresses that are already subscribed.
         * @param total
         *      The total number of addresses that need to be subscribed.
         */
        void onProgress( int current, int total );
    }


    private int doMassSubscribe2(final Collection<String> addresses, final SubscriptionMode mode) throws ProcessingException {
        // we are going to change this
        subscribers[mode.index] = null;

        return new Scraper<Integer>("Unable to mass-subscribe") {
            protected Integer scrape() throws IOException, SAXException, ProcessingException {
                WebForm form = getListMemberForm(mode);

                form.setParameter("subscribeList",Util.toList(addresses,' '));

                // we must change the method, from get to post, to be able to include
                // a large number of e-mails... Need to experiment with that.
                // There's no way in HTTP Unit to change the method, so, we had to
                // break the e-mails in groups...

                SubmitButton subscribeBt = form.getSubmitButton("Button", "Subscribe");
                if (subscribeBt == null)
                    throw new ProcessingException("Error: submit button not found! This is probably the wrong page...");

                // check the response
                WebResponse r = checkError(form.submit(subscribeBt));
                String text = r.getText();

                int start = text.indexOf("<p>New members subscribed:");
                int end = text.indexOf("</p>", start);

                if (start == -1 || end == -1)
                    throw new ProcessingException("Error: wrong answer while subscribing.");

                String numberTxt = text.substring(start+26, end).trim();

                return new Integer(numberTxt);
            }
        }.run();
    }

    /**
     * Gets the list member form to mass subscribe/unsubscribe addresses.
     */
    private WebForm getListMemberForm(SubscriptionMode mode) throws IOException, SAXException, ProcessingException {
        WebResponse response = goTo(project._getURL()+"/servlets/MailingListMembers?list="+name+"&group="+mode.groupName);

        for( WebForm form : response.getForms() ) {
            if (form.getAction().equals("MailingListMembers"))
                return form;
        }
        throw new ProcessingException("Error: this is not the Mailing List Members Form!");
    }

    private int doMassUnsubscribe(final Collection<String> addresses, final SubscriptionMode mode) throws ProcessingException {
        // we are going to change this
        subscribers[mode.index] = null;

        Integer r = (Integer)new Scraper("Unable to mass-unsubscribe") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebForm form = getListMemberForm(mode);
                form.setParameter("unsubscribeList",addresses.toArray(new String[addresses.size()]));

                // we must change the method, from get to post, to be able to include
                // a large number of e-mails... The unsubscribe has the same problem
                // of the subscribe (see method doMassSubscribe2()).
                // It is very unusual to have large unsubscibes and it is more dangerous
                // too, so, I'll leave this for a future implementation. Right now,
                // let's just handle small unsubscribes.
                SubmitButton subscribeBt = form.getSubmitButton("Button", "Unsubscribe");
                if (subscribeBt == null)
                    throw new ProcessingException("Error: submit button not found! This is probably the wrong page...");

                WebResponse r = checkError(form.submit(subscribeBt));
                String text = r.getText();

                int start = text.indexOf("<p>Members unsubscribed:");
                int end = text.indexOf("</p>", start);

                if (start == -1 || end == -1)
                    throw new ProcessingException("Error: wrong answer while subscribing.");

                String numberTxt = text.substring(start+24, end).trim();

                return new Integer(numberTxt);
            }
        }.run();

        return r.intValue();
    }

    /**
     * Parses all the subscribers.
     */
    private void parseSubscribers(final SubscriptionMode mode) throws ProcessingException {
        new Scraper("Unable to parse subscriber info") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebForm form = getListMemberForm(mode);
                subscribers[mode.index] = Collections.unmodifiableList(Arrays.asList(form.getOptions("unsubscribeList")));
                return null;
            }
        }.run();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JNMailingList)) return false;

        final JNMailingList that = (JNMailingList) o;

        return this.name.equals(that.name) && this.project==that.project;
    }

    public int hashCode() {
        return 29 * project.hashCode() + name.hashCode();
    }
}
