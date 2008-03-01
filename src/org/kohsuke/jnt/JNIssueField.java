package org.kohsuke.jnt;

/**
 * @author Kohsuke Kawaguchi
 */
public enum JNIssueField {
    STATUS("issue_status"),
    SUBCOMPONENT("subcomponent"),
    RESOLUTION("resolution"),
    TARGET_MILESTONE("target_milestone"),
    CC("cc"),
    URL("issue_file_loc"),
    OS("op_sys"),
    PRIORITY("priority"),
    PLATFORM("platform"),
    SUMMARY("short_desc"),
    STATUS_WHITEBOARD("status_whiteboard"),
    VERSION("version"),
    DEPENDS_ON("dependson"),
    BLOCKED_BY("blocked"),
    TYPE("issue_type")
    ;

    /**
     * Value as show up in the issue tracker XML &lt;field_name> element.
     */
    public final String value;

    JNIssueField(String value) {
        this.value = value;
    }

    public static JNIssueField find(String value) {
        for( JNIssueField v : values() )
            if(v.value.equals(value))
                return v;
        throw new IllegalArgumentException("Unexpected issue field name: "+value);
    }
}
