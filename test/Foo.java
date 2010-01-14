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
        JavaNet.connect().getProject("esb-console").getSummary();
    }
}
