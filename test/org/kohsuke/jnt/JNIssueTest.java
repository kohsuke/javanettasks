package org.kohsuke.jnt;

import junit.textui.TestRunner;

import java.util.Calendar;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNIssueTest  extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNIssueTest.class);
    }

    public void test1() throws ProcessingException {
        JNProject project = con.getProject("java-ws-xml-incubator");
        JNIssueTracker it = project.getIssueTracker();

        JNIssue i = it.get(1);

        assertEquals("All",i._getPlatform());
        calendarTest(2004,12,28,14,24,8,i.getCreationDate());
        assertEquals(1,i.getId());
        calendarTest(2005,1,10,15,40,17,i.getLastModified());
        assertEquals(Priority.P3,i.getPriority());
        assertEquals(con.getUser("harshatcse"),i.getReporter());
        assertEquals(IssueResolution.FIXED, i.getResolution());
        assertEquals(IssueStatus.RESOLVED, i.getStatus());
        assertEquals("Please graduate me... I have grown up now...",i.getShortDescription());
        assertEquals(IssueType.TASK,i.getType());
        assertEquals("current",i.getVersion().getName());

        List<JNIssue.Description> d = i.getDescriptions();
        assertEquals(4,d.size());

        assertEquals("started working on it.",d.get(1).getText());
        assertEquals(con.getUser("kohsuke"),d.get(1).getAuthor());
        calendarTest(2005,1,10,15,16,49,d.get(1).getTimestamp());
    }

    private void calendarTest(int yy, int mm, int dd, int HH, int MM, int SS, Calendar cal) {
        assertEquals(yy,cal.get(Calendar.YEAR));
        assertEquals(mm,cal.get(Calendar.MONTH)+1);
        assertEquals(dd,cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HH,cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(MM,cal.get(Calendar.MINUTE));
        assertEquals(SS,cal.get(Calendar.SECOND));
    }

}
