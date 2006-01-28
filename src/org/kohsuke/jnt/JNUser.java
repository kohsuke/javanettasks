package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebForm;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

/**
 * An user of java&#x2E;net.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNUser extends JNObject implements Comparable {
    /**
     * Login name of the user.
     */
    private final String name;

    /**
     * Lazily computed user id.
     */
    private Integer id;

    /**
     * Projects to which this user belongs to.
     */
    private List<JNProject> projects;

    protected JNUser( JavaNet net, String name ) {
        super(net);
        this.name = name;
    }

    /**
     * Obtains the user name.
     * 
     * @return
     *      non-null valid string.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the e-mail address of the user.
     */
    public final String getEmailAddress() {
        return name+"@dev.java.net";
    }

    /**
     * Gets the user id, which is what the system uses to uniquely identify each user.
     *
     * <p>
     * Accessing this property requires a domain admin privilege.
     */
    public int getId() throws ProcessingException {
        if(id!=null)
            return id;

        id = new Scraper<Integer>("error parsing user id for "+name) {
            protected Integer scrape() throws IOException, SAXException, ProcessingException {
                // parse ID
                WebResponse r = goTo("https://www.dev.java.net/servlets/UserList");
                WebForm form = r.getFormWithName("UserListFilterForm");
                form.setParameter("field","LoginName");
                form.setParameter("matchType","equals");
                form.setParameter("matchValue",name);
                r = checkError(form.submit());

                Document dom = Util.getDom4j(r);
                Element a = (Element)dom.selectSingleNode("//DIV[@id='userlist']/TABLE//TR[2]//A");
                String anchor = a.attributeValue("href");
                Matcher m = USERID_PATTERN.matcher(anchor);
                if(!m.matches())
                    throw new ProcessingException("unexpected href "+anchor);

                // make sure that the user name matches
                if(!a.getTextTrim().equals(name))
                    throw new ProcessingException("Unexpected name "+a.getTextTrim()+" (expected:"+name+")");

                return Integer.parseInt(m.group(1));
            }
        }.run();

        return id;
    }

    private static final Pattern USERID_PATTERN = Pattern.compile("https://www.dev.java.net/servlets/UserEdit\\?userID=([0-9]+)");

    /**
     * Gets the projects to which the user belongs.
     *
     * <p>
     * Accessing this property requires a domain admin privilege.
     */
    public Collection<JNProject> getProjects() throws ProcessingException {
        if(projects!=null)
            return projects;

        projects = new Scraper<List<JNProject>>("error parsing projects for user "+name) {
            protected List<JNProject> scrape() throws IOException, SAXException, ProcessingException {
                List<JNProject> projects = new ArrayList<JNProject>();

                WebResponse r = goTo("https://www.dev.java.net/servlets/UserEdit?userID=" + getId());
                Document dom = Util.getDom4j(r);
                List<Element> anchors = (List<Element>)dom.selectNodes("//DIV[@id='projectroles']/TABLE//TR/TD[1]/A");
                for (Element a : anchors) {
                    projects.add(root.getProject(a.getTextTrim()));
                }

                return Collections.unmodifiableList(projects);
            }
        }.run();

        return projects;
    }


    public int compareTo(Object o) {
        JNUser that = (JNUser)o;
        return this.name.compareTo(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof JNUser))
            return false;
        JNUser that = (JNUser)obj;
        return this.name.equals(that.name);
    }

    public String toString() {
        return name;
    }
}
