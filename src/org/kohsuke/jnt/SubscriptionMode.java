package org.kohsuke.jnt;

/**
 * Mode of subscription to a mailing list.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SubscriptionMode {
    /**
     * Normal subscription mode. One e-mail per a post.
     */
    public static final SubscriptionMode NORMAL = new SubscriptionMode("normal","Subscribers",0);

    /**
     * Digest subscription mode. One e-mail per a day.
     */
    public static final SubscriptionMode DIGEST = new SubscriptionMode("digest","Digest+Subscribers",1);

    private final String name;

    /**
     * Either "Subscribers" or "Digest+Subscribers". This form is used in the HTML forms.
     */
    /*package*/ final String groupName;

    /**
     * 0 and 1. Useful to map modes to an array index.
     */
    /*package*/ final int index;

    private SubscriptionMode(String name, String groupName, int index) {
        this.name = name;
        this.groupName = groupName;
        this.index = index;
    }

    /**
     * Returns human-readable mode name. Either "normal" or "digest".
     */
    public String toString() {
        return name;
    }
}
