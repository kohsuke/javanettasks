package org.kohsuke.jnt;



/**
 * An role on java&#x2E;net.
 *
 * @author Kohsuke Kawaguchi
 */
public class JNRole implements Comparable {
    protected final JavaNet net;

    private final String name;

    protected JNRole( JavaNet net, String name ) {
        this.net = net;
        this.name = name;
    }

    /**
     * Obtains the full role name.
     *
     * @return
     *      non-null valid string.
     */
    public final String getName() {
        return name;
    }

    public int compareTo(Object o) {
        JNRole that = (JNRole)o;
        return this.name.compareTo(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof JNRole))
            return false;
        JNRole that = (JNRole)obj;
        return this.name.equals(that.name);
    }
}
