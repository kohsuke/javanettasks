package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.FileStatus;
import org.kohsuke.jnt.JNFileFolder;

import java.io.PrintStream;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
public class LinkUrlCommandlet extends AbstractFileUploadCommandlet {
    public String getShortDescription() {
        return "Creates a URL link in java.net docs&files section";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: linkUrl <projectName> <filePath on server> <description> <status> <url>");
        out.println("Creates a link to URL in java.net document&files section");
        out.println("  projectName : the java.net project name to which the file is uploaded");
        out.println("  filePath    : the filepath in documents&files section, such as /releases/foo.zip");
        out.println("                or /bar.txt");
        out.println("  description : Human-readable file description. Can contain spaces, but no HTML.");
        out.println("  status      : one of the file status (draft/reviewed/baselined/stable/archival/obsolete)");
        out.println("  url         : the URL to link to");
    }

    protected int action(JNFileFolder folder, String fileName, String description, FileStatus status, String src) throws Exception {
        folder.linkUrl(fileName,description,status,new URL(src));
        return 0;
    }
}
