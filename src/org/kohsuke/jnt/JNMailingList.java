package org.kohsuke.jnt;

/**
 * A mailing list of a project on java&#x2E;net.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNMailingList {
    private final JNProject project;
    private final String name;

    JNMailingList(JNProject project, String name) {
        this.project = project;
        this.name = name;
    }

    /**
     * Gets the project to which this mailing list belongs.
     */
    public JNProject getProject() {
        return project;
    }

    /**
     * Gets the name of ths mailing list, such as "issues" or "cvs".
     *
     * @return never null.
     */
    public String getName() {
        return name;
    }
}
