package org.kohsuke.jnt;

/**
 * Mailing list operation mode enum.
 *
 * @author Kohsuke Kawaguchi
 */
public enum ListType {
    /**
     * Only subscribers can post.
     */
    DISCUSS("discuss"),
    /**
     * Only moderaters can post.
     */
    MODERATED("moderated"),
    /**
     * Anyone can post.
     */
    UNMODERATED("unmoderated");

    ListType(String name) {
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
