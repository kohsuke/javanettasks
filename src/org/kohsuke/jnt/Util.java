package org.kohsuke.jnt;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;

/**
 * Utility code.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Util {
    /** Finds the first element child, or null if not found. */
    static Element getFirstElementChild( Element parent ) {
        for( Node n = parent.getFirstChild(); n!=null; n=n.getNextSibling() ) {
            if(n.getNodeType()==Node.ELEMENT_NODE)
                return (Element)n;
        }
        return null;
    }

    /**
     * Gets the value for an item in a combo box.
     */
    static String getOptionValueFor( WebForm form, String parameter, String displayString ) {
        String[] options = form.getOptions(parameter);
        for (int i = 0; i < options.length; i++) {
            if( options[i].equals(displayString) ) {
                return form.getOptionValues(parameter)[i];
            }
        }
        throw new IllegalStateException("No such option:"+displayString);
    }
    
    /**
     * Finds a hyper-link that has the specified text and whose
     * target URL starts with the given prefix.
     */
    static WebLink findLink( WebConversation wc, String text, String urlPrefix ) throws ProcessingException, SAXException {
        WebLink[] links = wc.getCurrentPage().getLinks();
        for( int i=0; i<links.length; i++ ) {
            if( links[i].asText().indexOf(text)==-1 )
                continue;
            if( !links[i].getURLString().startsWith(urlPrefix))
                continue;
                
            return links[i];
        }
        
        throw new ProcessingException("no link found for '"+text+"'");
    }
}
