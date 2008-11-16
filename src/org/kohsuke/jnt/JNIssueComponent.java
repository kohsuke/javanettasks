package org.kohsuke.jnt;

import org.xml.sax.SAXException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.io.IOException;

import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebForm;

/**
 * Component in the java.net issue tracker.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JNIssueComponent extends JNObject {
    private final JNProject project;
    private final String name;
    private Map<String,JNIssueSubcomponent> subcomponents;

    protected JNIssueComponent(JNProject project, String name) {
        super(project);
        this.name = name;
        this.project = project;
    }

    public String getName() {
        return name;
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
        if(subcomponents!=null)
            return subcomponents;

        subcomponents = new TreeMap<String,JNIssueSubcomponent>();

        new Scraper("unable to parse the list of subcomponents for "+name) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                Document dom = Util.getDom4j(goTo(project._getURL()+"/issues/editcomponents.cgi?component="+name));
                List<Element> trs = dom.selectNodes(".//DIV[@id='issuezilla']//TR");
                for (Element tr : trs) {
                    Node a = tr.selectSingleNode("./TD/A");
                    if(a==null) continue;   // not a component line
                    String name = a.getText();
                    subcomponents.put(name,new JNIssueSubcomponent(JNIssueComponent.this,name));
                }
                return null;
            }
        }.run();

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
