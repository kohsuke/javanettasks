/*
 * Use is subject to the license terms.
 */
package org.kohsuke.jnt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.cookies.CookieProperties;

/**
 * Root of java&#x2E;net.
 * 
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class JavaNet {
    protected final WebConversation wc;
    
    private final Map projects = new HashMap();
    private final Map users = new HashMap();
    private final Map roles = new HashMap();

    /**
     * A special {@link JNUser} object that represents the current user.
     */
    private JNMyself myself;
    
    private JavaNet() {
        this(new WebConversation());
    }

    private JavaNet(WebConversation wc) {
        this.wc = wc;
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
    private void login( String userName, String password ) throws ProcessingException {
        try {
            WebResponse r = wc.getResponse("https://www.dev.java.net/servlets/TLogin");
            WebForm form = r.getFormWithName("loginform");
        
            form.setParameter("loginID",userName);
            form.setParameter("password",password);
            form.submit(form.getSubmitButton("Login"));

            // check if the login was successful
            if( wc.getCurrentPage().getURL().toExternalForm().indexOf("TLogin")!=-1)
                throw new ProcessingException("authentication failed. invalid username/password");
            
            // create a special myself object.
            myself = new JNMyself(this,userName);
            users.put(userName,myself);
            
        } catch( IOException e ) {
            throw new ProcessingException("unable to log in for user "+userName+" : "+e.getMessage(),e);
        } catch( SAXException e ) {
            throw new ProcessingException("unable to log in for user "+userName+" : "+e.getMessage(),e);
        }
    }
    
    

    /**
     * obtains the connection info from ~/.java.net and returns the connected {@link JavaNet} object.
     */
    public static JavaNet connect() throws ProcessingException {
        File accountFile = getPropertyFile();

        Properties accountInfo = new Properties();
        
        try {
            accountInfo.load( new FileInputStream(accountFile) );
        } catch( IOException e ) {
            throw new ProcessingException("Unable to locate "+accountFile.getPath(),e);
        }
            
        JavaNet session = new JavaNet();
        
        if(accountInfo.containsKey("proxyServer")) {
            System.out.println("Using the specified proxy");
            session.setProxyServer(
                accountInfo.getProperty("proxyServer"),
                Integer.parseInt(accountInfo.getProperty("proxyPort")) );
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
     * Gets the ".java.net" config property file.
     */
    private static File getPropertyFile() {
        // look for the system property that points to the file first
        String p = System.getProperty(".java.net");
        if(p!=null)
            return new File(p);

        // otherwise default to ~/.java.net
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir,".java.net");
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
        session.login(userName,password);
        session.setProxyServer(proxyServer,proxyPort);
        return session;
    }

    /**
     * Connects to java&#x2E;net by using a {@link WebConversation} instance that has already logged in.
     * <p>
     * Don't use this method unless you know what you are doing.
     */
    public static JavaNet connect( WebConversation conversation ) throws ProcessingException {
        return new JavaNet(conversation);
    }

    /**
     * Obtains a {@link JNProject} object from its name.
     *
     * <p>
     * A successful completion of this method does not
     * guarantee that the project actually exists.
     */
    public JNProject getProject(String projectName) throws ProcessingException {
        JNProject p = (JNProject)projects.get(projectName);
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
    public JNUser getUser(String userName) throws ProcessingException {
        JNUser u = (JNUser)users.get(userName);
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
        JNRole r = (JNRole)roles.get(roleName);
        if(r==null) {
            r = new JNRole(this,roleName);
            users.put(roleName,r);
        }
        return r;
    }
}
