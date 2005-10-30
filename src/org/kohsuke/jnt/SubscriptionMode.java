package org.kohsuke.jnt;

/**
 * Mode of subscription to a mailing list.
 *
 * @author Kohsuke Kawaguchi
 */
public enum SubscriptionMode {
    /**
     * Normal subscription mode. One e-mail per a post.
     */
    NORMAL("normal","Subscribers",0),

    /**
     * Digest subscription mode. One e-mail per a day.
     */
    DIGEST("digest","Digest+Subscribers",1),

    /**
     * List moderators.
     */
    MODERATOR("moderator","Moderators",2),

    /**
     * Allowed posters.
     *
     * Those who can post messages but don't receive e-mails.
     */
    ALLOWED_POSTERS("allowed posters","Allowed+Posters",3);

    private final String name;

    /**
     * Either "Subscribers" or "Digest+Subscribers". This form is used in the HTML forms.
     */
    /*package*/ final String groupName;

    /**
     * 0 and 1. Useful to map modes to an array index.
     */
    /*package*/ final int index;

    SubscriptionMode(String name, String groupName, int index) {
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

    /**
     * Gets either "Normal" or "Digest". Used in the form.
     */
    public String getNameAsWord() {
        return Character.toUpperCase(name.indexOf(0))+name.substring(1);
    }
}
