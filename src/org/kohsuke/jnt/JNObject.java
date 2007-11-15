package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;
import org.dom4j.Document;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class JNObject {
    protected final JavaNet root;


    protected JNObject(JavaNet root) {
        if(this instanceof JavaNet) root = (JavaNet)this; // because we can't do "super(this)" 
        this.root = root;
    }

    protected JNObject(JNObject parent) {
        this(parent.root);
    }

    protected WebResponse getCurrentPage() {
        return root.wc.getCurrentPage();
    }

    /**
     * Jumps to the specified page.
     */
    protected WebResponse goTo(String url) throws IOException, SAXException, ProcessingException {
        return checkError(root.wc.getResponse(url));
    }

    /**
     * Checks if the response contains any error message.
     */
    protected final WebResponse checkError(WebResponse resp) throws SAXException, ProcessingException, IOException {
        if(resp.getResponseCode()!=200)
            throw new ProcessingException("request failed "+resp.getResponseMessage());

        Document dom = Util.getDom4j(resp);
        org.dom4j.Node errorNode = dom.selectSingleNode("//DIV[@class='errormessage']");
        if(errorNode!=null) {
            // this happens for example when you request "http://nosuchproject.dev.java.net/"
            throw new ProcessingException(errorNode.getStringValue().trim());
        }

        return resp;
    }
}
