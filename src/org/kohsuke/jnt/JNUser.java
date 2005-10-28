package org.kohsuke.jnt;

/**
 * An user of java&#x2E;net.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNUser extends JNObject implements Comparable {
    private final String name;

    protected JNUser( JavaNet net, String name ) {
        super(net);
        this.name = name;
    }

    /**
     * Obtains the user name.
     * 
     * @return
     *      non-null valid string.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the e-mail address of the user.
     */
    public final String getEmailAddress() {
        return name+"@dev.java.net";
    }

    public int compareTo(Object o) {
        JNUser that = (JNUser)o;
        return this.name.compareTo(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof JNUser))
            return false;
        JNUser that = (JNUser)obj;
        return this.name.equals(that.name);
    }

    public String toString() {
        return name;
    }
}
