package org.kohsuke.jnt;

/**
 * Status of an issue.
 *
 * @author Kohsuke Kawaguchi
 */
public enum IssueStatus {
    /**
     * This issue has recently been added to the database. No one has validated that
     * this issue is true. Users who have the "Project Issue Tracking - Change"
     * permission set may confirm this issue, changing its state to NEW. Or,
     * it may be directly resolved and marked RESOLVED.
     */
    UNCONFIRMED(true),
    /**
     * This issue has recently been added to the assignee's list of issues and must
     * be processed. Issues in this state may be accepted, and become STARTED,
     * passed on to someone else, and remain NEW, or resolved and marked RESOLVED.
     */
    NEW(true),
    /**
     * This issue is not yet resolved, but is assigned to the proper person. From here
     * issues can be given to another person and become NEW, or resolved and become
     * RESOLVED.
     */
    STARTED(true),
    /**
     * This issue was once resolved, but the resolution was deemed incorrect.
     * For example, a WORKSFORME issue is REOPENED when more information shows up
     * and the issue is now reproducible. From here issues are either marked STARTED
     * or RESOLVED.
     */
    REOPENED(true),
    /**
     * A resolution has been taken, and it is awaiting verification by QA. From here
     * issues are either re-opened and become REOPENED, are marked VERIFIED, or are
     * closed for good and marked CLOSED.
     */
    RESOLVED(false),
    /**
     * QA has looked at the issue and the resolution and agrees that the appropriate
     * resolution has been taken. Issues remain in this state until the product they
     * were reported against is actually released, at which point they become CLOSED.
     */
    VERIFIED(false),
    /**
     * The issue is considered dead, the resolution is correct. A closed issue can be
     * reactivated by marking it REOPENED.
     */
    CLOSED(false);

    /**
     * True for {@link #UNCONFIRMED}, {@link #NEW}, {@link #STARTED},
     * and {@link #REOPENED}. This indicates that the issue needs to be
     * acted on by the developers.
     *
     * False for other status, indicating that the issue needs to be
     * acted on by the user, or the work has been completed.
     */
    public final boolean needsWork;

    IssueStatus(boolean needsWork) {
        this.needsWork = needsWork;
    }
}
