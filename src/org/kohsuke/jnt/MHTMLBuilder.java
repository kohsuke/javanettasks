package org.kohsuke.jnt;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates an <a href="http://en.wikipedia.org/wiki/MHTML">MHTML package</a>
 * from a {@link URL}.
 *
 * @author Kohsuke Kawaguchi
 */
public class MHTMLBuilder {
    private final Set<URL> resources = new HashSet<URL>();
    private final List<URL> queue = new ArrayList<URL>();
    private String boundary = "==mhtml-archiver_"+System.currentTimeMillis();

    public static void produce(WebConversation cnv, URL url, OutputStream os) throws IOException, SAXException {
        new MHTMLBuilder().run(cnv,url,os);
    }

    private void addResource(URL url) {
        if(resources.add(url))
            queue.add(url);
    }

    private void run(WebConversation cnv, URL url, OutputStream os) throws IOException, SAXException {
        queue.add(url);
        PrintStream out = new PrintStream(os);

        out.println("Content-Type: multipart/related; boundary=\""+boundary+"\"; type=\"text/html\"");
        out.println();

        while(!queue.isEmpty()) {
            url = queue.remove(queue.size() - 1);
            WebResponse r = cnv.getResponse(url.toExternalForm());
            //URLConnection con = url.openConnection();
            //con.connect();
            out.println("--"+boundary);
            out.println("Content-Type: "+r.getHeaderField("Content-type"));
            out.println("Content-Location: "+r.getURL());

            String contentType = r.getContentType();
            if(contentType.equals("text/html")) {
                out.println();
                produceHTML(r,out);
                continue;
            }

            if(contentType.startsWith("text/css")) {
                out.println();
                findInCSS(r.getText(),r.getURL());
            }

            boolean binary = !contentType.startsWith("text");

            // just dump the data
            if(binary)
                out.println("Content-Transfer-Encoding: base64");
            out.println();

            if(binary)
                new BASE64Encoder().encode(r.getInputStream(),out);
            else
                copyStream(r.getInputStream(),out);

            out.println();
            out.println();
        }
        // all done
        out.println("--"+boundary+"--");
    }

    public void produceHTML(WebResponse r, PrintStream out) throws IOException, SAXException {
        out.println();

        Document tree = Util.getDom4j(r);

        find(tree, r,"//IMG|//SCRIPT", "src");
        find(tree, r,"//LINK", "href");

        // replpace @import in CSS
        for( Element style : (List<Element>)tree.selectNodes("//STYLE") ) {
            String text = style.getText();
            findInCSS(text, r.getURL());
        }
        out.print(r.getText());
        out.println();
    }

    private void findInCSS(String text, URL baseURL) throws MalformedURLException {
        Matcher matcher = IMPORT_PATTERN.matcher(text);
        while(matcher.find()) {
            addResource(new URL(baseURL, unquote(matcher.group(1))));
        }
    }

    private String unquote(String s) {
        return s.substring(1,s.length()-1);
    }

    private final Pattern IMPORT_PATTERN = Pattern.compile("@import (\"[^\"]+\"|'[^']+');");

    private void find(Document tree, WebResponse r, String xpath, String attr) throws MalformedURLException {
        for( Element e : (List<Element>)tree.selectNodes(xpath) ) {
            String src = e.attributeValue(attr);
            if(src==null)   continue;
            addResource(new URL(r.getURL(), src));
        }
    }

    private void copyStream(InputStream i, OutputStream o) throws IOException {
        byte[] buf = new byte[256];
        int sz;
        while((sz=i.read(buf))>0)
            o.write(buf,0,sz);
        i.close();
        //o.close();
    }
}
