package org.kohsuke.jnt;

import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component in the java.net issue tracker.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNIssueComponent extends JNObject {
    private final JNProject project;
    private final String name;
    private final List<String> targetMilestones;
    private final List<String> versions;
    private final Map<String,JNIssueSubcomponent> subcomponents = new HashMap<String, JNIssueSubcomponent>();

    protected JNIssueComponent(JNProject project, String name, List<String> subcomponentNames, List<String> targetMilestones, List<String> versions) {
        super(project);
        this.name = name;
        this.project = project;
        this.targetMilestones = targetMilestones;
        this.versions = versions;
        for (String sn : subcomponentNames)
            subcomponents.put(sn,new JNIssueSubcomponent(this,sn));
    }

    public String getName() {
        return name;
    }

    /**
     * Versions defined on this component.
     */
    public List<String> getVersions() {
        return versions;
    }

    /**
     * Target milestones defined on this component.
     */
    public List<String> getTargetMilestones() {
        return targetMilestones;
    }
    
    public JNIssueSubcomponent getSubcomponent(String name) throws ProcessingException {
        return getSubcomponents().get(name);
    }

    /**
     * Gets subcomponents in this component.
     *
     * <p>
     * This requires project admin provilege.
     */
    public Map<String,JNIssueSubcomponent> getSubcomponents() throws ProcessingException {
        return subcomponents;
    }

    /**
     * Addsa new subcomponent.
     */
    public JNIssueSubcomponent add(final String subComponentName, final String description, final String initialOwner, final String initialQaContact) throws ProcessingException {
        return new Scraper<JNIssueSubcomponent>("Failed to add subcomponent "+subComponentName+" in "+this.name) {
            protected JNIssueSubcomponent scrape() throws IOException, SAXException, ProcessingException {
                WebResponse rsp = goTo(project._getURL() + "/issues/editcomponents.cgi?component=" + name + "&action=add");
                WebForm form = rsp.getFormWithID("editcomponents");
                form.setParameter("subcomponent",subComponentName);
                form.setParameter("description",description);
                form.setParameter("initialowner",initialOwner);
                form.setParameter("initialqacontact",initialQaContact);
                checkError(form.submit());

                JNIssueSubcomponent comp = new JNIssueSubcomponent(JNIssueComponent.this,subComponentName);
                if(subcomponents!=null)
                    subcomponents.put(subComponentName,comp);
                return comp;
            }
        }.run();
    }
}
