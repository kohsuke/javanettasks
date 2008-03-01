package org.kohsuke.jnt;

import org.dom4j.Element;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;

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

    private int[] createRange(int start,int end) {
        int[] ids = new int[end-start];
        for( int x=0; x<ids.length; x++ )
            ids[x] = start+x;
        return ids;
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

//    private static String zeroPad(int value,int width) {
//        // efficiency? what's that?
//        String r = Integer.toString(value);
//        while(r.length()<width) {
//            r = '0'+r;
//        }
//        return r;
//    }
}
