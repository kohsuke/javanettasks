package org.kohsuke.jnt;

import junit.textui.TestRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public class JNFileFolderTest extends TestCaseBase {
    public static void main(String[] args) {
        TestRunner.run(JNFileFolderTest.class);
    }

    /**
     * Create
     */
    public void test1() throws ProcessingException {
        JNProject project = con.getProject("javanettasks-test");
        JNFileFolder utf = project.getRootFolder().getSubFolder("unit-test");
        assertNotNull(utf);

        // test folder navigation
        assertEquals(3,utf.getSubFolders().size());
        assertEquals(0,utf.getFiles().size());
        assertTrue(utf.getSubFolders().containsKey("aaa"));
        assertTrue(utf.getSubFolders().containsKey("bbb"));
        assertTrue(utf.getSubFolders().containsKey("ccc"));

        JNFileFolder aaa = utf.getSubFolder("aaa");
        assertEquals(1,aaa.getSubFolders().size());
        assertEquals(0,aaa.getFiles().size());
        assertTrue(aaa.getSubFolders().containsKey("aaa"));

        JNFileFolder nested = aaa.getSubFolder("aaa");
        assertEquals(0,nested.getSubFolders().size());
        assertEquals(3,nested.getFiles().size());

        assertSame(project.getRootFolder(),nested.getSubFolder("/"));
        assertSame(utf,nested.getSubFolder("/unit-test"));

        // test properties of files
        JNFile x = nested.getFile("x");
        assertNotNull(x);
        JNFile y = nested.getFile("y");
        assertNotNull(y);
        JNFile z = nested.getFile("z");
        assertNotNull(z);

        assertSame(FileStatus.DRAFT,x.getStatus());
        assertSame(FileStatus.OBSOLETE,y.getStatus());
        assertSame(FileStatus.STABLE,z.getStatus());

        assertSame(con.getUser("kohsuke"),x.getModifiedBy());
        assertSame(con.getUser("kohsuke"),y.getModifiedBy());
        assertSame(con.getUser("kohsuke"),z.getModifiedBy());

        assertTrue(new Date().compareTo(x.getLastModified())>0);
        assertTrue(new Date().compareTo(y.getLastModified())>0);
        assertTrue(new Date().compareTo(z.getLastModified())>0);
        assertTrue(x.getLastModified().compareTo(y.getLastModified())<0);
        assertTrue(y.getLastModified().compareTo(z.getLastModified())<0);

        assertEquals("x",x.getDescription());
        assertEquals(9881,x.getId());
        assertEquals("https://javanettasks-test.dev.java.net/servlets/ProjectDocumentView?documentID=9881&noNav=true",
                x.getURL().toExternalForm());
    }

    /**
     * Create and delete a folder
     */
    public void test2() throws ProcessingException {
        JNProject project = con.getProject("javanettasks-test");
        JNFileFolder root = project.getRootFolder();

        JNFileFolder sub = root.createFolder("unittest2","no description");
        assertNotNull(sub);

        assertEquals(root.getSubFolder("unittest2"),sub);

        sub.delete();

        assertNull(root.getSubFolder("unittest2"));
    }

    /**
     * Create and delete a file
     */
    public void test3() throws ProcessingException, IOException {
        JNProject project = con.getProject("javanettasks-test");
        JNFileFolder root = project.getRootFolder();

        File f = File.createTempFile("jnt","test");
        f.deleteOnExit();
        FileWriter w = new FileWriter(f);
        w.write("abc");
        w.close();

        JNFile file = root.uploadFile("bravo","gamma",FileStatus.OBSOLETE,f);
        assertNotNull(file);
        assertSame(file,root.getFile("bravo"));

        assertEquals("bravo",file.getName());
        assertEquals("gamma",file.getDescription());
        assertSame(FileStatus.OBSOLETE,file.getStatus());
        assertSame(con.getMyself(),file.getModifiedBy());
        long timeDiff = Math.abs(new Date().getTime()-file.getLastModified().getTime());
        System.out.println("diff: "+timeDiff);  // just to make sure we are in the same ballpark
        assertTrue(5*60*1000>timeDiff);

//        BufferedReader in = new BufferedReader(new InputStreamReader(file.getURL().openStream()));
//        assertEquals("abc",in.readLine());
//        in.close();

        file.delete();

        assertNull(root.getFile("bravo"));
    }
}
