import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNIssue;
import org.kohsuke.jnt.IssueResolution;

/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    public static void main(String[] args) throws Exception {
        JNProject p = JavaNet.connect().getProject("javanettasks-test");
        JNIssue issue = p.getIssueTracker().get(2);

        issue.beginEdit().resolve(IssueResolution.FIXED).commit("Testing auto-update");
    }
}
