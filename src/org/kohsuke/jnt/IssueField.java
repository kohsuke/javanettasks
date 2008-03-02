package org.kohsuke.jnt;

/**
 * @author Kohsuke Kawaguchi
 */
public enum IssueField {
    STATUS("issue_status"),
    SUBCOMPONENT("subcomponent"),
    RESOLUTION("resolution"),
    TARGET_MILESTONE("target_milestone"),
    CC("cc"),
    URL("issue_file_loc"),
    OS("op_sys"),
    PRIORITY("priority"),
    PLATFORM("rep_platform"),
    SUMMARY("short_desc"),
    STATUS_WHITEBOARD("status_whiteboard"),
    VERSION("version"),
    DEPENDS_ON("dependson"),
    BLOCKED_BY("blocked"),
    TYPE("issue_type"),
    /**
     * old value is null, and the new value is the description of the attachment.
     */
    ATTACHMENT("attachments.ispatch"),
    ASSIGNED("assigned_to"),
    QA_CONTACT("qa_contact"),
    KEYWORDS("keywords"),
    // ???
    CONFIRMED("everconfirmed"),
    /**
     * It's not clearly exactly when this happens,
     * but I noticed this in https://jax-ws.dev.java.net/issues/xml.cgi?id=30
     *
     * Most likely this is a bug in IssueZilla, and should be ignored
     * by any applications.
     */
    COMMENT("longdesc")
    ;

    /**
     * Value as show up in the issue tracker XML &lt;field_name> element.
     */
    public final String value;

    IssueField(String value) {
        this.value = value;
    }

    public static IssueField find(String value) {
        for( IssueField v : values() )
            if(v.value.equals(value))
                return v;
        throw new IllegalArgumentException("Unexpected issue field name: "+value);
    }
}
