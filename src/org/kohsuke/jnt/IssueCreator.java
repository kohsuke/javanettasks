package org.kohsuke.jnt;

import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Very simple issue creator (doesn't check any field requirements... etc)
 *
 * <p>
 * The procedure to create a new issue will be as follows:
 *
 * <pre>
 * // first obtain the {@link JNIssueTracker} for the issue tracker you'd like to create a new issue.
 * JNIssueTracker it = ...;
 *
 * // then begin an edit session
 * IssueCreator c = issue.createIssue();
 *
 * // call methods on IssueCreator to fill in fields.
 * c.setField(...).setField(...)
 * ...
 *
 * // then finally commit the change
 * c.commit();
 * </pre>
 *
 * <p>
 * The class follows the fluent API pattern so that you can write
 * multiple changes in a concise fashion.
 *
 * @author Tomas Knappek
 */
public class IssueCreator extends AbstractIssueEditor<IssueCreator> {
    IssueCreator(JNProject project) throws ProcessingException {
        super(project);
    }

    public void commit() throws ProcessingException {
        new Scraper<Void>("Failed create new issue in "+project.getName()) {
            protected Void scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/issues/enter_bug.cgi");

                WebForm form = Util.getFormWithAction(response, "post_bug.cgi");

                for (Action action : actions)
                    action.update(form);

                response = checkError(form.submit());

                return null;
            }
        }.run();
    }


}