package org.kohsuke.jnt;

import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The current logged-in user in java&#x2E;net.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNMyself extends JNUser {
    /**
     * Set of {@link JNProject} to which the current user belongs.
     * Never null.
     */
    private Set<JNProject> myProjects;
    
    protected JNMyself(JavaNet net,String userName) {
        super(net,userName);
    }
    
    private void parseStartPage() throws ProcessingException {
        if( myProjects!=null )
            return;     // already parsed

        new Scraper("failed to parse the personal info page") {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                // obtain current user information
                Document dom = Util.getDom4j(goTo("https://www.dev.java.net/servlets/StartPage"));

                // parse my projects
                Set<JNProject> myProjects = new HashSet<JNProject>();
                while(true) {// repeat while we have next pages

                    List<Element> projects = (List<Element>)dom.selectNodes("//DIV[@id='myprojects']//TR/TD[1]/A");
                    for (Element project : projects) {
                        myProjects.add(root.getProject(project.getText()));
                    }

                    Element nextLink = (Element)dom.selectSingleNode("//DIV[@id='startpage']//P[@class='paginate']/A[text()='Next']");
                    if(nextLink==null)
                        break;

                    dom = Util.getDom4j(goTo(nextLink.attributeValue("href")));
                }

                JNMyself.this.myProjects = Collections.unmodifiableSet(myProjects);

                return null;
            }
        }.run();
    }
    
    /**
     * Set of {@link JNProject} to which the current user belongs.
     * 
     * @return
     *      non-null (but possibly empty) set. The set is read-only.
     */
    public final Set<JNProject> getMyProjects() throws ProcessingException {
        parseStartPage();
        return myProjects;
    }
}
