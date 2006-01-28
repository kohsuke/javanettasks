package org.kohsuke.jnt;

import com.meterware.httpunit.WebResponse;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import sun.misc.BASE64Encoder;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Creates an MHTML package from {@link WebResponse}.
 *
 * @author Kohsuke Kawaguchi
 */
public class MHTMLBuilder {
    Map<URL,String> resources = new HashMap<URL,String>();
    Set<String> names = new HashSet<String>();

    private String getName(URL url) {
        String name = resources.get(url);
        if(name==null) {
            Integer retry = null;

            do {
                name = url.getPath();
                name = name.substring(name.lastIndexOf('/')+1);
                int idx = name.indexOf('.');
                if(idx<0) {
                    if(retry==null) {
                        retry = 2;
                    } else {
                        name += retry;
                        retry++;
                    }
                } else {
                    String base = name.substring(0,idx);
                    String ext = name.substring(idx);
                    if(retry==null) {
                        retry = 2;
                    } else {
                        base += retry;
                        retry++;
                    }
                    name = base+ext;
                }
            } while(names.contains(name));

            resources.put(url,name);
        }
        return "cid:"+name;
    }

    public void produce(WebResponse r, OutputStream os) throws Exception {
        String boundary = "==mhtml-archiver"; // ."+System.currentTimeMillis();

        PrintStream out = new PrintStream(os);
        out.println("--"+boundary);
        out.println("Content-Type: text/html; charset="+r.getCharacterSet());
        out.println();


        Writer w = new OutputStreamWriter(os,r.getCharacterSet());
        Document tree = Util.getDom4j(r);

        rewrite(tree, r,"//IMG|//SCRIPT", "src");
        rewrite(tree, r,"//LINK", "href");

        // replpace @import in CSS
        for( Element style : (List<Element>)tree.selectNodes("//STYLE") ) {
            String text = style.getText();
            List<Integer> range = new ArrayList<Integer>();
            Matcher matcher = IMPORT_PATTERN.matcher(text);
            while(matcher.find()) {
                range.add(matcher.start(1));
                range.add(matcher.end(1));
            }

            for( int i=range.size()-2; i>=0; i-=2 ) {
                Integer s = range.get(i);
                Integer e = range.get(i + 1);
                String newName = getName(new URL(r.getURL(), unquote(text.substring(s,e))));
                text = text.substring(0,s)+quote(newName)+text.substring(e);
            }
            style.setText(text);
        }

        tree.write(w);
        out.println();

        for (Map.Entry<URL, String> e : resources.entrySet()) {
            URL url = e.getKey();
            URLConnection con = url.openConnection();
            con.connect();
            out.println("--"+boundary);
            out.println("Content-ID: <"+e.getValue()+">");
            out.println("Content-Type: "+con.getContentType());
            out.println("Content-Transfer-Encoding: base64");
            out.println("Content-Disposition: inline; filename=\""+e.getValue()+"\"");
            out.println();

            new BASE64Encoder().encode(con.getInputStream(),out);
            out.println();
        }
        out.println("--"+boundary+"--");
    }

    private String unquote(String s) {
        return s.substring(1,s.length()-1);
    }

    private String quote(String s) {
        return '"'+s+'"';
    }

    private final Pattern IMPORT_PATTERN = Pattern.compile("@import (\"[^\"]+\"|'[^']+');");

    private void rewrite(Document tree, WebResponse r, String xpath, String attr) throws MalformedURLException {
        for( Element e : (List<Element>)tree.selectNodes(xpath) ) {
            String src = e.attributeValue(attr);
            if(src==null)   continue;

            String name = getName(new URL(r.getURL(), src));
            e.addAttribute(attr,name);
        }
    }

    // TODO: MHTML format. Outlook and IE supports it, but what else?
    // See Content-Location header. No mangling necessary.
}
