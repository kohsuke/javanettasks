package org.kohsuke.jnt;

import com.meterware.httpunit.WebConversation;

/**
 * An user of java&#x2E;net.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNUser {
    protected final JavaNet net;

    protected final WebConversation wc;
    
    
    private final String name;
    
    protected JNUser( JavaNet net, String name ) {
        this.net = net;
        this.wc = net.wc;
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
}
