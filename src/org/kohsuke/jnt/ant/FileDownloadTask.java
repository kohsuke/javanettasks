package org.kohsuke.jnt.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebResponse;

/**
 * Log in to java.net then download a file.
 * 
 * This can be used to download a protected resource.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class FileDownloadTask extends AbstractJavaNetTask {
    
    /** URL to be retrieved */
    private URL url;
    
    /** File will be written to this file. */
    private File localFile;
    
    public void setURL( URL url) {
        this.url = url;
    }
    
    public void setToFile( File localFile ) {
        this.localFile = localFile;
    }
    
    protected void run(JavaNet cmd) throws ProcessingException, BuildException {
        
        // mandatory attributes check
        if( localFile==null )
            throw new BuildException("file name is not set");
        if( url==null )
            throw new BuildException("url is not set");
        
        log("downloading "+url,Project.MSG_VERBOSE);
        try {
            WebResponse r = cmd.getConversation().getResponse(url.toExternalForm());
            copyStream( r.getInputStream(), new FileOutputStream(localFile) );
        } catch( IOException e ) {
            throw new BuildException(e);
        } catch( SAXException e ) {
            throw new BuildException(e);
        }
        
        log("done",Project.MSG_VERBOSE);
    }
    
    private void copyStream(InputStream i, OutputStream o) throws IOException {
        byte[] buf = new byte[256];
        int sz;
        while((sz=i.read(buf))>0)
            o.write(buf,0,sz);
        i.close();
        o.close();
    }
}

