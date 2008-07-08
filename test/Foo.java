import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNIssue;
import org.kohsuke.jnt.IssueResolution;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.FileStatus;

import java.io.ByteArrayInputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    public static void main(String[] args) throws Exception {
        JNProject p = JavaNet.connect("vijayj","java.net").getProject("javafx-sdk");
        JNFileFolder f = p.getFolder("/continuous_builds");

        f.uploadFile("test","description", FileStatus.STABLE,
            new ByteArrayInputStream("test".getBytes()),"text/plain");
    }
}
