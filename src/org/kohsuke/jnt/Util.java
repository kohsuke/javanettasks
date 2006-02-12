package org.kohsuke.jnt;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import org.cyberneko.html.parsers.SAXParser;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

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
    static String getOptionValueFor( WebForm form, String parameter, String displayString ) throws ProcessingException {
        String[] options = form.getOptions(parameter);
        for (int i = 0; i < options.length; i++) {
            if( options[i].replace((char)0xA0,' ').equals(displayString) ) {
                return form.getOptionValues(parameter)[i];
            }
        }
        throw new ProcessingException("No such option:"+displayString);
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

        throw new ProcessingException("no link found for '"+text+'\'');
    }

    /**
     * Obtains the HTML of the response as a dom4j document.
     */
    static Document getDom4j( WebResponse wr ) throws IOException, SAXException {
        try {
            return new SAXReader(new SAXParser()).read(new StringReader(wr.getText()));
        } catch (DocumentException e) {
            SAXException se = new SAXException(e);
            se.initCause(e);
            throw se;
        }
    }

    /**
     * Format all strings in the collection by using the specified separator.
     */
    static String toList(Collection addresses, char separator) {
        StringBuffer buf = new StringBuffer();
        for (Iterator itr = addresses.iterator(); itr.hasNext();) {
            String address = (String) itr.next();
            if(buf.length()>0)  buf.append(separator);
            buf.append(address);
        }
        return buf.toString();
    }

    /**
     * Gets the form with a specified action, or throw an error
     */
    public static WebForm getFormWithAction(WebResponse response, String actionName) throws ProcessingException, SAXException {
        WebForm[] forms = response.getForms();
        for( int i=0; i<forms.length; i++ ) {
            WebForm form = forms[i];

            if(form.getAction().equals(actionName))
                return form;
        }

        throw new ProcessingException("Unable to find a form with action="+actionName);
    }

}
