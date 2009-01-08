package org.kohsuke.jnt;

import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Object for making updates to an issue.
 *
 * <p>
 * The procedure to update an issue will be as follows:
 *
 * <pre>
 * // first obtain the {@link JNIssue} for the issue you'd like to update
 * JNIssue issue = ...;
 *
 * // then begin an edit session
 * IssueEditor e = issue.beginEdit();
 *
 * // call methods on IssueEditor to update as many fields as you need
 * ...
 *
 * // then finally commit the change
 * e.update(comment);
 * </pre>
 *
 * <p>
 * The class follows the fluent API pattern so that you can write
 * multiple changes in a concise fashion.
 *
 * @author Kohsuke Kawaguchi
 */
public final class IssueEditor extends AbstractIssueEditor<IssueEditor> {
    private final JNIssue issue;
    private final int id;

    protected IssueEditor(JNIssue issue) {
        super(issue.getProject());
        this.issue = issue;
        this.id = issue.getId();
    }

    /**
     * Marks the issue as resolved, with the specified resolution.
     */
    public IssueEditor resolve(final IssueResolution resolution) {
        actions.add(new Action() {
            public void update(WebForm form) {
                form.setParameter("knob","resolve");
                form.setParameter("resolution",resolution.name());
            }
        });
        return this;
    }

    /**
     * Accepts an issue
     */
    public IssueEditor accept() {
        return knob("accept");
    }

    /**
     * Reopens an issue
     */
    public IssueEditor reopen() {
        return knob("reopen");
    }

    /**
     * Marks the issue as verified.
     */
    public IssueEditor verify() {
        return knob("verify");
    }

    /**
     * Marks the issue as closed.
     */
    public IssueEditor close() {
        return knob("close");
    }

    /**
     * Marks this issue as a duplicate of another issue.
     */
    public IssueEditor duplicateOf(final JNIssue issue) {
        actions.add(new Action() {
            public void update(WebForm form) {
                form.setParameter("knob","duplicate");
                form.setParameter("dup_id",String.valueOf(issue.getId()));
            }
        });
        return this;
    }

    private IssueEditor knob(final String state) {
        actions.add(new Action() {
            public void update(WebForm form) {
                form.setParameter("knob", state);
            }
        });
        return this;
    }

    /**
     * Commits the change.
     *
     * @param comment
     *      Description of this update. This is a mandatory field.
     */
    public void commit(final String comment) throws ProcessingException {
        new Scraper<Void>("Failed to post comment to "+project.getName()+" issue #"+id) {
            protected Void scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/issues/show_bug.cgi?id="+id);

                WebForm form = Util.getFormWithAction(response, "process_bug.cgi");

                for (Action action : actions)
                    action.update(form);

                form.setParameter("comment",comment);
                response = checkError(form.submit());

                // TODO: how do we check if there was an error?
                // the response code is 200 when I submit a form without any comment.
                // what happens with a collision?
                return null;
            }
        }.run();
    }
}
