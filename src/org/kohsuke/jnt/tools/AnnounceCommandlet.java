package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JavaNet;

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.GregorianCalendar;

/**
 * Posts a new announcement.
 *
 * @author Kohsuke Kawaguchi
 */
public class AnnounceCommandlet extends Commandlet {
    public String getShortDescription() {
        return "posts a new announcement";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage : announce <project> <headline>");
        out.print("Post a new announcement to a project.");
        out.print("Stdin is read as announcement body text");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        JavaNet conn = connection.connect();

        if(args.length!=2) {
            printUsage(System.err);
            return -1;
        }

        StringBuffer text = new StringBuffer();
        Reader in = new InputStreamReader(System.in);
        int sz; char[] buf = new char[1024];
        while((sz=in.read(buf))>=0)
            text.append(buf,0,sz);

        JNProject project = conn.getProject(args[0]);
        project.getNewsItems().createNewsItem(new GregorianCalendar(),
            args[1], text.toString(), null, null);
        return 0;
    }
}
