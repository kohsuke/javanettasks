package org.kohsuke.jnt;

/**
 * Mailing list operation mode enum.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ListType {
    /**
     * Only subscribers can post.
     */
    public static final ListType DISCUSS = new ListType("discuss");
    /**
     * Only moderaters can post.
     */
    public static final ListType MODERATED = new ListType("moderated");
    /**
     * Anyone can post.
     */
    public static final ListType UNMODERATED = new ListType("unmoderated");

    private ListType(String name) {
        this.name = name;
    }

    /*package*/ final String name;

    /**
     * Returns a human-readable name of the type.
     */
    public String toString() {
        return name;
    }
}
