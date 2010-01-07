package org.kohsuke.jnt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the parameters needed to establish connection to java.net.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConnectionInfo {
    /**
     * Username and password.
     */
    public String userName, password;

    /**
     * If the access should go through proxy, the HTTP proxy host name.
     */
    public String proxyServer;
    /**
     * If {@link #proxyServer} is non-null, this is the HTTP proxy port number. 
     */
    public int proxyPort;

    public ConnectionInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * Creates a connection information by reading the default account file at ~/.java.net
     */
    public ConnectionInfo() throws ProcessingException {
        this(getDefaultAccountFile());
    }

    /**
     * Creates a connection information by reading the specified parameter file. 
     */
    public ConnectionInfo(File accountFile) throws ProcessingException {
        Properties accountInfo = new Properties();

        try {
            accountInfo.load( new FileInputStream(accountFile) );
        } catch( IOException e ) {
            throw new ProcessingException("Unable to locate "+accountFile.getPath()+" : See https://javanettasks.dev.java.net/nonav/maven/config.html",e);
        }

        if(accountInfo.containsKey("proxyServer")) {
            proxyServer = accountInfo.getProperty("proxyServer");
            proxyPort = Integer.parseInt(accountInfo.getProperty("proxyPort"));
        }

        userName = accountInfo.getProperty("userName");
        if(userName==null)
            throw new ProcessingException("userName property is missing");
        password = accountInfo.getProperty("password");
        if(password==null)
            throw new ProcessingException("password property is missing");
    }

    /**
     * Gets the default ".java.net" config property file.
     */
    public static File getDefaultAccountFile() {
        // look for the system property that points to the file first
        String p = System.getProperty(".java.net");
        if(p!=null)
            return new File(p);

        // otherwise default to ~/.java.net
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir,".java.net");
    }

}
