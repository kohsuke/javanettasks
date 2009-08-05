package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.WeakHashMap;

/**
 * java&#x2E;net issue tracker (IssueZilla) in one project.
 *
 * @author Kohsuke Kawaguchi
 * @author Tomas Knappek
 */
public final class JNIssueTracker extends JNObject {

    private final JNProject project;

    /**
     * Cached instance of issues.
     */
    private final Map<Integer,JNIssue> issues = new WeakHashMap<Integer, JNIssue>();

    /**
     * Lazily parsed list of components, by their {@link JNIssueComponent#getName() names}.
     */
    private Map<String,JNIssueComponent> components;

//
// metadata parsed from "issues/xml.cgi?metadata=xml" lazily.
//
    /**
     * Metadata XML root element.
     */
    private Element rawMetadata;
    private List<String> issueTypes;
    private List<String> priorities;
    private List<String> status;
    private List<String> resolutions;
    private List<String> platforms;
    private List<String> opSystems;
    private List<String> keywords;

    JNIssueTracker(JNProject project) {
        super(project);
        this.project = project;
    }

    /**
     * Gets the issue of the specified id.
     *
     * For the performance reason, if you access multiple issues, it will be faster
     * to use {@link #get(int...)}
     */
    public JNIssue get(int id) throws ProcessingException {
        return getOrCreate(id,null);
    }

    final JNIssue getOrCreate(int id,Element rawData) throws ProcessingException {
        JNIssue r = issues.get(id);
        if(r==null) {
            r = new JNIssue(project,id,rawData);
            issues.put(id,r);
        }
        return r;
    }

    /**
     * Gets multiple issues at once.
     *
     * @return
     *      the map is from the id to {@link JNIssue}. It contains entries for
     *      all the specified IDs that actually exist. Non-existent issue ID
     *      in the argument will not be an error, but the returned map won't have
     *      entries for such IDs.
     */
    public Map<Integer,JNIssue> get(int... ids) throws ProcessingException {
        // list of IDs to fetch
        List<Integer> fetch = new ArrayList<Integer>();

        // check the cache first
        Map<Integer,JNIssue> r = new TreeMap<Integer, JNIssue>();
        for( int id : ids ) {
            JNIssue v = issues.get(id);
            if(v!=null)     r.put(id,v);
            else            fetch.add(id);
        }

        if(!fetch.isEmpty())
            // fetch the rest
            r.putAll(JNIssue.bulkCreate(project,JNIssue.bulkFetch(project,fetch)));

        return r;
    }

    /**
     * Gets multiple issues at once, by spcifying a range [start,end)
     */
    public Map<Integer,JNIssue> getRange(int start, int end) throws ProcessingException {
        return get(createRange(start,end));
    }

    /**
     * Gets all the issues in this issue tracker.
     */
    public Map<Integer,JNIssue> getAll() throws ProcessingException {
        Map<Integer,JNIssue> allIssues = new TreeMap<Integer,JNIssue>();

        for( int i=0; ; i+= BULK_SIZE) {
            Map<Integer,JNIssue> batch = get(createRange(i, i + BULK_SIZE));
            allIssues.putAll(batch);
            if(batch.isEmpty())
                return allIssues;
        }
    }

    private static final int BULK_SIZE = 100;

    private int[] createRange(int start,int end) {
        int[] ids = new int[end-start];
        for( int x=0; x<ids.length; x++ )
            ids[x] = start+x;
        return ids;
    }

    /**
     * Loads the issue persisted by {@link JNIssue#save(OutputStream)}.
     */
    public JNIssue load(int id, InputStream in) throws ProcessingException {
        try {
            Document dom = new SAXReader().read(in);
            Element root = dom.getRootElement();

            return new JNIssue(project, id, root);
        } catch (DocumentException e) {
            throw new ProcessingException(e);
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss");

    /**
     * Gets all the issues updated during the specified time span.
     *
     * <p>
     * This wraps
     * http://www.jini.org/nonav/docs/ProjectOwnerIssues.html#xml
     *
     * @return
     *      the map is from the id to {@link JNIssue}. It contains entries for
     *      all the specified IDs.
     */
    public Map<Integer,JNIssue> getUpdatedIssues(Calendar start,Calendar end) throws ProcessingException {
        return getUpdatedIssues(start.getTime(), end.getTime());
    }

    public Map<Integer,JNIssue> getUpdatedIssues(Date start,Date end) throws ProcessingException {
        return JNIssue.bulkCreate(project,JNIssue.bulkUpdateFetch(project,
                "include_attachments=false&ts="+dateFormat.format(start)+"&ts_end="+dateFormat.format(end)));
    }

    /**
     * Gets all the issues updated since the given time stamp.
     */
    public Map<Integer,JNIssue> getUpdatedIssues(Date start) throws ProcessingException {
        // if the time span is too long, fetch them in a group to avoid facing OutOfMemoryError
        long sec = 1000;
        long min = 60*sec;
        long hour = 60*min;
        long day = 24*hour;
        long month = 30*day;

        Map<Integer,JNIssue> r = new TreeMap<Integer, JNIssue>();
        for( long t=start.getTime(); t<System.currentTimeMillis(); t+=month)
            r.putAll(getUpdatedIssues(new Date(t),new Date(t+month)));
        return r;
    }

    /**
     * Gets the issue tracker metadata
     *
     * @return
     * @throws org.kohsuke.jnt.ProcessingException
     */
    public Element getMetadata() throws ProcessingException {
        if (rawMetadata == null) {
            // fetch now
            rawMetadata = new Scraper<Element>("fetching the metadata info") {
                public Element scrape() throws IOException, SAXException, ProcessingException {
                    WebResponse rsp = project.goTo(project.getURL() + "issues/"+JNIssue.XML_CGI+"?metadata=xml");
                    return Util.getDom4j(rsp).getRootElement().element("HEAD").element("ISSUEZILLA_METADATA");
                }
            }.run();

            if (rawMetadata == null)
                throw new ProcessingException("No metadata available!");
        }
        return rawMetadata;
    }

    public List<String> getIssueTypes() throws ProcessingException {
        if (issueTypes == null) {
            Element types = getMetadata().element("ATTRIBUTES").element("ISSUE_TYPE");
            issueTypes = getValuesAsList(types);
        }
        return issueTypes;
    }

    public List<String> getPriorities() throws ProcessingException {
        if (priorities == null) {
            Element prios = getMetadata().element("ATTRIBUTES").element("PRIORITIES");
            priorities = getValuesAsList(prios);
        }
        return priorities;
    }

    public List<String> getStatus() throws ProcessingException {
        if (status == null) {
            Element st = getMetadata().element("ATTRIBUTES").element("ISSUE_STATUS").element("STATUS_ALL");
            status = getValuesAsList(st);
        }
        return status;
    }

    public List<String> getResolutions() throws ProcessingException {
        if (resolutions == null) {
            Element res = getMetadata().element("ATTRIBUTES").element("RESOLUTIONS");
            resolutions = getValuesAsList(res);
        }
        return resolutions;
    }

    public List<String> getOpSystems() throws ProcessingException {
        if (opSystems == null) {
            Element os = getMetadata().element("AFFECT").element("OP_SYS");
            opSystems = getValuesAsList(os);
        }
        return opSystems;
    }

    public List<String> getPlatforms() throws ProcessingException {
        if (platforms == null) {
            Element plafs = getMetadata().element("AFFECT").element("REP_PLATFORM");
            platforms = getValuesAsList(plafs);
        }
        return platforms;
    }

    public List<String> getKeywords() throws ProcessingException {
        if (keywords == null) {
            Element keys = getMetadata().element("AFFECT").element("KEYWORDS");
            keywords = getValuesAsList(keys);
        }
        return keywords;
    }

    private List<String> getValuesAsList(Element element) {
        List<String> list = new Vector<String>();
        if (element != null) {
            for (Element e : children(element)) {
                list.add(e.getTextTrim());
            }
        }
        return list;
    }

    public List<Action> getActions(String fromState) throws ProcessingException {
        Element actions = getMetadata().element("STATE_TRANSITIONS");
        List<Action> actionList = new Vector<Action>();
        for (Element state : children(actions)) {
            if (state.attributeValue("from_status").equals(fromState)) {
                for (Element action : children(state)) {
                    String label = action.attributeValue("label");
                    String value = action.attributeValue("value");
                    actionList.add(new Action(label, value));
                }
                break;
            }
        }
        return actionList;
    }

    /**
     * Returns the components in this issue tracker.
     *
     * @return
     *      can be empty but never null.
     */
    public Map<String,JNIssueComponent> getComponents() throws ProcessingException {
        if (components == null) {
            components = new HashMap<String, JNIssueComponent>();
            Element coms = getMetadata().element("AFFECT").element("COMPONENTS");
            for (Element com : children(coms)) {
                String name = com.element("COMPONENT_NAME").getTextTrim();
                List<String> subs = getValuesAsList(com.element("SUBCOMPONENTS"));
                List<String> mils = getValuesAsList(com.element("TARGET_MILESTONES"));
                List<String> vers = getValuesAsList(com.element("VERSIONS"));
                components.put(name,new JNIssueComponent(project, name, subs, mils, vers));
            }
        }
        return components;
    }

    /**
    * Gets the {@link IssueCreator} object for creating new issue
     */
    public IssueCreator createIssue() throws ProcessingException {
        return new IssueCreator(project);
    }
    
    /**
     * Runs the requested stored query
     */
    public Map<Integer,JNIssue> getIssuesByQuery(String queryName) throws ProcessingException {
        return JNIssue.bulkCreate(project,JNIssue.bulkQueryFetch(project,queryName));
    }

    /**
     * Runs the default query - My issues
     */
    public Map<Integer,JNIssue> getMyIssues() throws ProcessingException {
        return getIssuesByQuery(null);
    }

    /**
     * Gets an issue tracker component by its name.
     *
     * @return
     *      null if not found.
     */
    public JNIssueComponent getComponent(String name) throws ProcessingException {
        return getComponents().get(name);
    }

    /**
     * Action metadata
     */
    public class Action {
        private final String label;
        private final String value;

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        Action(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    // this seems like another way to get updates.
    // TODO: to query updates https://hudson.dev.java.net/issues/buglist.cgi?field0-0-0=delta_ts&type0-0-0=greaterthan&value0-0-0=2008-07-04&format=xml

//    private static String zeroPad(int value,int width) {
//        // efficiency? what's that?
//        String r = Integer.toString(value);
//        while(r.length()<width) {
//            r = '0'+r;
//        }
//        return r;
//    }

}
