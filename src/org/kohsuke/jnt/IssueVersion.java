package org.kohsuke.jnt;

/**
 * The version against which issues can be reported.
 *
 * @author Kohsuke Kawaguchi
 */
public final class IssueVersion {
    private final String name;

    IssueVersion(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    /**
     * Returns the human-readable name of the version, such as "current"
     * (which is the one automatically set when a new project is created.)
     */
    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueVersion)) return false;

        final IssueVersion issueVersion = (IssueVersion) o;

        if (name != null ? !name.equals(issueVersion.name) : issueVersion.name != null) return false;

        return true;
    }

    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }
}
