package org.kohsuke.jnt;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * java&#x2E;net issue tracker (IssueZilla) in one project.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNIssueTracker extends JNObject {

    private final JNProject project;

    /**
     * Cached instance of issues.
     */
    private final Map<Integer,JNIssue> issues = new WeakHashMap<Integer, JNIssue>();

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
        return JNIssue.bulkCreate(project,JNIssue.bulkUpdateFetch(project,
                "ts="+dateFormat.format(start.getTime())+"&ts_end="+dateFormat.format(end.getTime())));
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
