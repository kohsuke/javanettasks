package org.kohsuke.jnt;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates an MHTML package from {@link WebResponse}.
 *
 * @author Kohsuke Kawaguchi
 */
public class MHTMLBuilder {
    Set<URL> resources = new HashSet<URL>();
    List<URL> queue = new ArrayList<URL>();
    String boundary = "==mhtml-archiver"; // ."+System.currentTimeMillis();

    private void addResource(URL url) {
        if(resources.add(url))
            queue.add(url);
    }

    public void produce(WebConversation cnv, URL url, OutputStream os) throws Exception {
        queue.add(url);
        PrintStream out = new PrintStream(os);
        while(!queue.isEmpty()) {
            url = queue.remove(queue.size() - 1);
            URLConnection con = url.openConnection();
            con.connect();
            out.println("--"+boundary);
            String contentType = con.getContentType();
            out.println("Content-Type: "+contentType);

            if(contentType.startsWith("text/html")) {
                con.getInputStream().close();
                WebResponse r = cnv.getResponse(url.toExternalForm());
                out.println("Content-Location: "+r.getURL());
                out.println();
                produceHTML(r,out);
                continue;
            }

            out.println("Content-Location: "+url);

            if(contentType.startsWith("text/css")) {
                out.println();
                produceCSS(con,out);
                continue;
            }

            // otherwise treat this as BLOB
            out.println("Content-Transfer-Encoding: base64");
            out.println();

            new BASE64Encoder().encode(con.getInputStream(),out);
            out.println();
            out.println();
        }
        // all done
        out.println("--"+boundary+"--");
    }

    private void produceCSS(URLConnection con, PrintStream out) throws IOException {
        // TODO: check encoding
        InputStream in = con.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(in,baos);
        byte[] image = baos.toByteArray();

        BufferedReader r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(image)));
        String line;

        while((line=r.readLine())!=null) {
            findInCSS(line, con.getURL());
        }

        out.write(image);
        out.println();
    }

    public void produceHTML(WebResponse r, PrintStream out) throws Exception {
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
        o.close();
    }
}
