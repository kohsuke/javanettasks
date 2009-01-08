package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Issue tracker metadata info.
 * 
 * @author Tomas Knappek
 */
public class JNIssueTrackerMetadata extends JNObject {

    private final JNProject project;
    private Document rawDocument;
    private final Element rawData;
    private List<String> issueTypes;
    private List<String> priorities;
    private List<String> status;
    private List<String> resolutions;
    private List<JNIssueComponent> components;
    private List<String> platforms;
    private List<String> opSystems;
    private List<String> keywords;

    JNIssueTrackerMetadata(JNProject project) throws ProcessingException {
        this(project, null);
    }

    JNIssueTrackerMetadata(JNProject project, Element rawData) throws ProcessingException {
        super(project);
        this.project = project;

        if (rawData == null) {
            // fetch now
            rawDocument = fetch(project);
            rawData = rawDocument.getRootElement().element("HEAD").element("ISSUEZILLA_METADATA");

            if (rawData == null) {
                throw new ProcessingException("No metadata available!");
            }
        }

        this.rawData = rawData;

    }

    /**
     * Fetches the XML for metadata
     */
    static Document fetch(final JNProject project) throws ProcessingException {
        return new Scraper<Document>("fetching the metadata info") {

            public Document scrape() throws IOException, SAXException, ProcessingException {
                WebResponse rsp = project.goTo(project.getURL() + "issues/xml.cgi?metadata=xml");
                return Util.getDom4j(rsp);
            }
        }.run();
    }

    public List<String> getIssueTypes() {
        if (issueTypes == null) {
            Element types = rawData.element("ATTRIBUTES").element("ISSUE_TYPE");
            issueTypes = getValuesAsList(types);
        }
        return issueTypes;
    }

    public List<String> getPriorities() {
        if (priorities == null) {
            Element prios = rawData.element("ATTRIBUTES").element("PRIORITIES");
            priorities = getValuesAsList(prios);
        }
        return priorities;
    }

    public List<String> getStatus() {
        if (status == null) {
            Element st = rawData.element("ATTRIBUTES").element("ISSUE_STATUS").element("STATUS_ALL");
            status = getValuesAsList(st);
        }
        return status;
    }

    public List<String> getResolutions() {
        if (resolutions == null) {
            Element res = rawData.element("ATTRIBUTES").element("RESOLUTIONS");
            resolutions = getValuesAsList(res);
        }
        return resolutions;
    }

    public List<String> getOpSystems() {
        if (opSystems == null) {
            Element os = rawData.element("AFFECT").element("OP_SYS");
            opSystems = getValuesAsList(os);
        }
        return opSystems;
    }

    public List<String> getPlatforms() {
        if (platforms == null) {
            Element plafs = rawData.element("AFFECT").element("REP_PLATFORM");
            platforms = getValuesAsList(plafs);
        }
        return platforms;
    }

    public List<String> getKeywords() {
        if (keywords == null) {
            Element keys = rawData.element("AFFECT").element("KEYWORDS");
            keywords = getValuesAsList(keys);
        }
        return keywords;
    }


    public List<JNIssueComponent> getComponents() {
        if (components == null) {
            components = new Vector<JNIssueComponent>();
            Element coms = rawData.element("AFFECT").element("COMPONENTS");
            for (Element com : children(coms)) {
                String name = com.element("COMPONENT_NAME").getTextTrim();
                List<String> subs = getValuesAsList(com.element("SUBCOMPONENTS"));
                List<String> mils = getValuesAsList(com.element("TARGET_MILESTONES"));
                List<String> vers = getValuesAsList(com.element("VERSIONS"));
                components.add(new JNIssueComponent(project, name, subs, mils, vers));
            }
        }
        return components;
    }

    public List<Action> getActions(String fromState) {
        Element actions = rawData.element("STATE_TRANSITIONS");
        List<Action> actionList = new Vector<Action>();
        for (Element state : children(actions)) {
            if (state.attributeValue("from_status").equals(fromState)) {
                for (Element action : children(state)) {
                    String label = action.attributeValue("label");
                    String value = action.attributeValue("value");
                    actionList.add(new Action(label, value));
                }
                break;
            }
        }
        return actionList;
    }

    public Element getRawData() {
        return rawData;
    }

    private static List<String> getValuesAsList(Element element) {
        List<String> list = new Vector<String>();
        if (element != null) {
            for (Element e : children(element)) {
                list.add(e.getTextTrim());
            }
        }
        return list;
    }

    @SuppressWarnings({"unchecked"})
    private static List<Element> children(Element e) {
        return (List<Element>)e.elements();
    }

    /**
     * Action metadata
     */
    public class Action {

        private String label;
        private String value;

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public Action(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}