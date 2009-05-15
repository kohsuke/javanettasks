/*
 * Use is subject to the license terms.
 */
package org.kohsuke.jnt;

import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.cookies.CookieProperties;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Root of java&#x2E;net.
 *
 * <p>
 * Start with one of the {@link #connect() connect} method, which gives you
 * an instance of {@link JavaNet}. The rest of the java.net object model
 * can be traversed from this object.
 * 
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class JavaNet extends JNObject {
    protected final WebConversation wc;

    private final Map<String,JNProject> projects = new HashMap<String,JNProject>();
    private final Map<String,JNUser> users = new HashMap<String,JNUser>();
    private final Map<String,JNRole> roles = new HashMap<String,JNRole>();

    /**
     * A special {@link JNUser} object that represents the current user.
     */
    private JNMyself myself;

    private JavaNet() {
        this(new WebConversation());
    }

    private JavaNet(WebConversation wc) {
        super(null);
        this.wc = wc;

        // disable scripting support
        HttpUnitOptions.setScriptingEnabled(false);

        // java.net security certificate cause a problem. So avoid it by disabling certificate validation.
        SSLTrustAllManager.install();

        // we need this to work around the broken cookies in java.net
        CookieProperties.setDomainMatchingStrict(false);
    }

    private void setProxyServer( String hostName, int port ) {
        wc.setProxyServer(hostName,port);
        System.setProperty( "https.proxyHost", hostName );
        System.setProperty( "https.proxyPort", Integer.toString( port ) );
    }
    
    /**
     * Obtains the session being used.
     * 
     * If you need to use the underlying HTTPUnit directly, use this method.
     */
    public final WebConversation getConversation() {
        return wc;
    }
    
    /**
     * Logs in to the java.net. This method has to be called first.
     */
    public void login( final String userName, final String password ) throws ProcessingException {
        new Scraper("unable to log in as user "+userName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse r = wc.getResponse("https://www.dev.java.net/servlets/TLogin");
                WebForm form = r.getFormWithID("loginform");

                form.setParameter("loginID",userName);
                form.setParameter("password",password);
                checkError(form.submit(form.getSubmitButton("Login")));

                // check if the login was successful
                if( wc.getCurrentPage().getURL().toExternalForm().indexOf("TLogin")!=-1)
                    throw new ProcessingException("authentication failed. invalid username/password");

                // create a special myself object.
                myself = new JNMyself(JavaNet.this,userName);
                users.put(userName,myself);

                return null;
            }
        }.run();
    }
    
    /**
     * obtains the connection info from ~/.java.net and returns the connected {@link JavaNet} object.
     */
    public static JavaNet connect() throws ProcessingException {
        return connect(getDefaultAccountFile());
    }

    /**
     * obtains the connection info from the given file and returns the connected {@link JavaNet} object.
     *
     * @param accountFile
     *      The property file that stores the connection information.
     */
    public static JavaNet connect(File accountFile) throws ProcessingException {
        Properties accountInfo = new Properties();
        
        try {
            accountInfo.load( new FileInputStream(accountFile) );
        } catch( IOException e ) {
            throw new ProcessingException("Unable to locate "+accountFile.getPath()+" : See https://javanettasks.dev.java.net/nonav/maven/config.html",e);
        }
            
        JavaNet session = new JavaNet();
        
        if(accountInfo.containsKey("proxyServer")) {
            String host = accountInfo.getProperty("proxyServer");
            String port = accountInfo.getProperty("proxyPort");
//            System.out.println("Using the proxy server "+host+":"+port);
            session.setProxyServer( host, Integer.parseInt(port) );
        }
        
        String userName = accountInfo.getProperty("userName");
        if(userName==null)
            throw new ProcessingException("userName property is missing");
        String password = accountInfo.getProperty("password");
        if(password==null)
            throw new ProcessingException("password property is missing");
        
        session.login(userName,password);
        
        return session;
    }

    /**
     * Gets the default ".java.net" config property file.
     */
    private static File getDefaultAccountFile() {
        // look for the system property that points to the file first
        String p = System.getProperty(".java.net");
        if(p!=null)
            return new File(p);

        // otherwise default to ~/.java.net
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir,".java.net");
    }

    /**
     * Connects anonymously.
     */
    public static JavaNet connectAnonymously() {
        return new JavaNet();
    }

    /**
     * Connects to java&#x2E;net directly by using the given account info.
     */
    public static JavaNet connect( String userName, String password ) throws ProcessingException {
        JavaNet session = new JavaNet();
        session.login(userName,password);
        return session;
    }
    
    /**
     * Connects to java&#x2E;net through HTTP proxy by using the given account info.
     */
    public static JavaNet connect( String userName, String password, String proxyServer, int proxyPort ) throws ProcessingException {
        JavaNet session = new JavaNet();
        session.setProxyServer(proxyServer,proxyPort);
        session.login(userName,password);
        return session;
    }

    /**
     * Connects to java&#x2E;net by using a {@link WebConversation} instance that has already logged in.
     * <p>
     * Don't use this method unless you know what you are doing.
     */
    public static JavaNet connect( WebConversation conversation ) {
        return new JavaNet(conversation);
    }

    /**
     * Obtains a {@link JNProject} object from its name.
     *
     * <p>
     * A successful completion of this method does not
     * guarantee that the project actually exists.
     *
     * @return
     *      always non-null, even if no such project exists.
     */
    public JNProject getProject(String projectName) throws ProcessingException {
        projectName = projectName.toLowerCase();
        JNProject p = projects.get(projectName);
        if(p==null){
            p = new JNProject(this,projectName);
            projects.put(projectName,p);
        }
        return p; 
    }
    
    /**
     * Obtains a special {@link JNUser} object that represents
     * the currently logged-in user.
     * 
     * @return
     *      always return non-null object.
     */
    public JNMyself getMyself() throws ProcessingException {
        return myself;
    }

    /**
     * Obtains a {@link JNUser} object from an user name.
     *
     * <p>
     * A successful completion of this method does not
     * guarantee that the user exists.
     *
     * @return
     *      always return a non-null object.
     */
    public JNUser getUser(String userName) {
        JNUser u = users.get(userName);
        if(u==null) {
            u = new JNUser(this,userName);
            users.put(userName,u);
        }
        return u;
    }

    /**
     * Obtains a {@link JNRole} object from a role name.
     *
     * <p>
     * A successful completion of this method does not
     * guarantee that the role actually exists.
     */
    public JNRole getRole(String roleName) throws ProcessingException {
        JNRole r = roles.get(roleName);
        if(r==null) {
            r = new JNRole(this,roleName);
            roles.put(roleName,r);
        }
        return r;
    }

    /**
     * Runs ad-hoc query against java.net database.
     * Requires domain admin privilege.
     */
    public String[][] runAdHocQuery(final String sql) throws ProcessingException {
        return new Scraper<String[][]>("unable to run ad-hoc query") {
            protected String[][] scrape() throws IOException, SAXException, ProcessingException {
                Document dom = Util.getDom4j(goTo(
                        "https://www.dev.java.net/servlets/AdHocQuery?query="
                                + URLEncoder.encode(sql, "UTF-8") + "&Button=Run+query"));
                List<Element> trs = (List<Element>) dom.selectNodes("//DIV[@id='adhocqueryresults']//TR");
                if(trs.size()<=1)   // no data
                    return new String[0][];

                trs = trs.subList(1,trs.size()); // skip the first header row
                String[][] data = new String[trs.size()][];

                for (int i=0; i < trs.size(); i++) {
                    List<Element> tds = trs.get(i).elements("TD");
                    data[i] = new String[tds.size()];
                    for (int j = 0; j < tds.size(); j++) {
                        Element td = tds.get(j);
                        data[i][j] = td.getText();
                    }
                }

                return data;
            }
        }.run();
    }

    static {
        // disable scripting support
        HttpUnitOptions.setScriptingEnabled(false);
    }
}
