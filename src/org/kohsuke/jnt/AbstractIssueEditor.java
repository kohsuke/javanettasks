package org.kohsuke.jnt;

import com.meterware.httpunit.WebForm;

import java.util.List;
import java.util.ArrayList;

/**
 * Common part between {@link IssueEditor} and {@link IssueCreator}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractIssueEditor<T extends AbstractIssueEditor<T>> extends JNObject {
    protected final JNProject project;
    protected final List<Action> actions = new ArrayList<Action>();

    protected static interface Action {
        void update(WebForm form);
    }

    /*package*/ AbstractIssueEditor(JNProject project) {
        super(project);
        this.project = project;
    }

    public T setField(final IssueField field, final String value) {
        actions.add(new Action() {
            public void update(WebForm form) {
                form.setParameter(field.value, value);
            }
        });

        return self();
    }

    @SuppressWarnings({"unchecked"})
    private T self() {
        return (T)this;
    }

    /**
     * Sets the priority.
     */
    public T setPriority(final Priority p) {
        actions.add(new Action() {
            public void update(WebForm form) {
                form.setParameter("priority",p.name());
            }
        });
        return self();
    }

    /**
     * Sets the issue type.
     */
    public T setType(final IssueType type) {
        actions.add(new Action() {
            public void update(WebForm form) {
                form.setParameter("issue_type",type.name());
            }
        });
        return self();
    }

    /**
     * Append new words to the status whiteboard
     */
    public T appendToWhiteBoard(final String words) {
        actions.add(new Action() {
            public void update(WebForm form) {
                final String field = "status_whiteboard";
                String value = form.getParameterValue(field);
                form.setParameter(field,
                    (value == null ? words : value + " " + words));
            }
        });
        return self();
    }
}
