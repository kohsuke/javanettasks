package org.kohsuke.jnt;

import org.dom4j.Element;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

/**
 * java&#x2E;net issue tracker (IssueZilla) in one project.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNIssueTracker {

    private final JNProject project;

    /**
     * Cached instance of issues.
     */
    private final Map<Integer,JNIssue> issues = new WeakHashMap<Integer, JNIssue>();

    JNIssueTracker(JNProject project) {
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
     *      all the specified IDs.
     */
    public Map<Integer,JNIssue> get(int... ids) throws ProcessingException {
        // list of IDs to fetch
        List<Integer> fetch = new ArrayList<Integer>();

        // check the cache first
        Map<Integer,JNIssue> r = new TreeMap<Integer, JNIssue>();
        for( int id : ids ) {
            JNIssue v = issues.get(id);
            if(v!=null)     r.put(id,v);
        }

        if(!fetch.isEmpty())
            // fetch the rest
            r.putAll(JNIssue.bulkCreate(project,JNIssue.bulkFetch(project,fetch)));

        return r;
    }

    /**
     * Gets all the issues updated during the specified month.
     *
     * <p>
     * This wraps
     *
     * @return
     *      the map is from the id to {@link JNIssue}. It contains entries for
     *      all the specified IDs.
     */
    public Map<Integer,JNIssue> getUpdatedIssues(int year, int month) throws ProcessingException {
        return JNIssue.bulkCreate(project,JNIssue.bulkUpdateFetch(project,
                zeroPad(year,4)+'-'+zeroPad(month,2)));
    }

    /**
     * Gets all the issues updated during the specified day.
     *
     * <p>
     * This wraps
     *
     * @return
     *      the map is from the id to {@link JNIssue}. It contains entries for
     *      all the specified IDs.
     */
    public Map<Integer,JNIssue> getUpdatedIssues(int year, int month, int dayOfMonth) throws ProcessingException {
        return JNIssue.bulkCreate(project,JNIssue.bulkUpdateFetch(project,
                zeroPad(year,4)+'-'+zeroPad(month,2)));
    }

    private static String zeroPad(int value,int width) {
        // efficiency? what's that?
        String r = Integer.toString(value);
        while(r.length()<width) {
            r = '0'+r;
        }
        return r;
    }
}
