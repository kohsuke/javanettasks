package org.kohsuke.jnt;

import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * folder in the java&#x2E;net file sharing section.
 *
 * <p>
 * In java.net, the "documents &amp; files" section serves as
 * a simle per-project file system.
 *
 * This object represents one folder in such a file system.
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class JNFileFolder extends JNObject {
    
    /*package*/ final JNProject project;
    private final String url;

    private final int id;

    private final String name;

    /**
     * Lazily parsed subfolders. From {@link String} to {@link JNFileFolder}.
     */
    private Map<String,JNFileFolder> subFolders;

    /**
     * Lazily parsed files in this folder. From {@link String} to {@link JNFile}.
     */
    private Map<String,JNFile> files;

    /**
     * Parent folder, or null if this is the root.
     */
    private final JNFileFolder parent;

    protected JNFileFolder(JNProject project, JNFileFolder parent, String name, int id) {
        super(project);
        this.project = project;
        this.parent = parent;
        this.name = name;
        this.id = id;
        this.url = project._getURL()+"/servlets/ProjectDocumentList?folderID="+id+"&expandFolder="+id;
    }
    
    /**
     * Returns a sub-folder.
     * 
     * @param folderName
     *      '/'-separated folder name. such as "abc", "abc/def", or "/abc/def".
     *
     * @return
     *      if the folder is not found, return null.
     */
    public JNFileFolder getSubFolder( String folderName ) throws ProcessingException {
        JNFileFolder f = this;
        // chdir to root
        if( folderName.startsWith("/"))
            f = project.getRootFolder();

        StringTokenizer tokens = new StringTokenizer(folderName,"/");
        while( tokens.hasMoreTokens() && f!=null ) {
            String dirName = tokens.nextToken();
//          System.out.println("cd "+dirName);
            f = f.getSubFolders().get(dirName);
        }

        return f;
    }

    private void parse() throws ProcessingException {
        new Scraper("Failed to parse the documents&files section") {
            protected Object scrape() throws IOException, SAXException, ProcessingException, ParseException {
                WebResponse response = goTo(url);
                Document dom = Util.getDom4j(response);

                // find the current folder
                Element current = (Element)dom.selectSingleNode("//DIV[@id='projectdocumentlist']//LI[@class='selection']");

                // parse sub folders
                subFolders = new TreeMap<String,JNFileFolder>();
                for( Element anchor : (List<Element>)current.selectNodes("UL/LI/A[SPAN]") ) {
                    // https://jaxb.dev.java.net/servlets/ProjectDocumentList?folderID=1747&expandFolder=1747
                    String name = anchor.getTextTrim();
                    // trim the trailing (n)
                    name = name.substring(0,name.lastIndexOf(0xA0/*NBSP*/));
                    String href = anchor.attributeValue("href");
                    int sidx = href.indexOf("?folderID=");
                    int eidx = href.indexOf("&expandFolder=");
                    if(sidx==-1 || eidx==-1)
                        throw new ProcessingException("Failed to parse the link "+href);

                    int id = Integer.parseInt( href.substring(sidx+"?folderID=".length(),eidx) );

                    subFolders.put(name,new JNFileFolder(project,JNFileFolder.this,name,id));
                }

                // find the current folder

                files = new TreeMap<String,JNFile>();


                List trs = dom.selectNodes("//DIV[@id='projectdocumentlist']//TD[@class='filebrowse']/DIV/TABLE/TR");
                for( int i=1; i<trs.size(); i++ ) { // row 0 == header
                    Element tr = (Element)trs.get(i);

                    JNFile file = new JNFile(JNFileFolder.this,tr);
                    files.put(file.getName(),file);
                }

                return null;
            }
        }.run();
    }

    /**
     * Returns the parent folder, or null if this folder is the root.
     */
    public JNFileFolder getParent() {
        return parent;
    }

    /**
     * Gets the name of this folder.
     * <p>
     * For the root folder, this method returns the name of the project.
     *
     * @return
     *      always non-null valid string.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the unique ID that distinguishes this folder.
     * <p>
     * The uniqueness is within a project.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns true if this is the root folder of a project.
     */
    public boolean isRoot() {
        return parent==null;
    }

    /**
     * Returns the subfolders of this folder.
     *
     * @return
     *      can be empty but never be null. read-only.
     */
    public Map<String, JNFileFolder> getSubFolders() throws ProcessingException {
        if(subFolders==null)  parse();
        return Collections.unmodifiableMap(subFolders);
    }

    /**
     * Returns the files in this folder.
     *
     * @return
     *      can be empty but never be null. read-only.
     */
    public Map<String, JNFile> getFiles() throws ProcessingException {
        if(files==null)  parse();
        return Collections.unmodifiableMap(files);
    }

    /**
     * Gets a file in this folder.
     *
     * <p>
     * Convenience method for <code>getFiles().get(fileName)</code>.
     */
    public JNFile getFile(String fileName) throws ProcessingException {
        return getFiles().get(fileName);
    }

    /**
     * Uploads a file to the folder.
     *
     * @param fileStatus
     *      can be null.
     * @deprecated
     *      use {@link #uploadFile(String, String, FileStatus, File)}
     */
    public void uploadFile( String fileName, String description, String fileStatus, File fileToUpload ) throws ProcessingException {
        uploadFile(fileName,description,FileStatus.parse(fileStatus),fileToUpload);
    }

    private interface DocumentAddFormActor {
        void act(WebForm form) throws IOException;
    }

    private JNFile _upload( final String errorMessage, final String fileName, final String description, final FileStatus fileStatus, final DocumentAddFormActor actor ) throws ProcessingException {
        return new Scraper<JNFile>(errorMessage) {
            protected JNFile scrape() throws IOException, SAXException, ProcessingException {
                setCurrentPage();

                WebResponse r = getCurrentPage();

                WebLink addFileLink = r.getLinkWith("Add new file");
                if(addFileLink==null) {
                    throw new ProcessingException("Unable to find 'add new file' link. Does this account have a permission to post a file?");
                }
                r = addFileLink.click();

                WebForm form = r.getFormWithID("ProjectDocumentAddForm");
                form.setParameter("name",fileName);
                if( fileStatus!=null )
                    form.setParameter("status",fileStatus.toString());
                form.setParameter("description",description);

                actor.act(form);
                r = checkError(form.submit());

                if( r.getImageWithAltText("Alert notification")!=null )
                    // TODO: obtain the error message
                    throw new ProcessingException(errorMessage);

                reset();
                parse();
                JNFile file = getFile(fileName);
                if(file==null)
                    throw new ProcessingException(errorMessage);
                return file;
            }
        }.run();
    }

    /**
     * Uploads a file to the folder.
     * 
     * @param fileStatus
     *      can be null.
     */
    public JNFile uploadFile( String fileName, String description, FileStatus fileStatus, final File fileToUpload ) throws ProcessingException {
        return _upload("error uploading a file "+fileToUpload,fileName,description,fileStatus,new DocumentAddFormActor() {
            public void act(WebForm form) throws IOException {
                if(!fileToUpload.exists() || !fileToUpload.isFile())
                    throw new IOException(fileToUpload+" is not a file");

                form.setParameter("type","file");
                form.setParameter("file",new UploadFileSpec[]{
                    new UploadFileSpec(fileToUpload.getName(),new FileInputStream(fileToUpload),guessContentType(fileToUpload))});
                    // this version somehow posts the full file name to the server, which often confuses it.
                    // new UploadFileSpec(fileToUpload)});
            }
        });
    }

    /**
     * Creates a link to an URL.
     */
    public JNFile linkUrl(String fileName, String description, FileStatus status, final URL link) throws ProcessingException {
        return _upload("error creating a link to "+link,fileName,description,status,new DocumentAddFormActor() {
            public void act(WebForm form) throws IOException {
                form.setParameter("type","link");
                form.setParameter("url",link.toString());
            }
        });
    }

    private static final String[] MIME_TYPE_TABLE = new String[] {
        "application/x-zip-compressed", ".zip", null
    };

    private static String guessContentType(File f) {
        String name = f.getName().toLowerCase();

        String type = null;
        for( String s : MIME_TYPE_TABLE ) {
            if(type==null) {
                type = s;
                continue;
            }
            if(s==null) {
                type = null;
                continue;
            }
            if(name.endsWith(s))
                return type;
        }

        return "text/plain";
    }

    /**
     * Creates a new sub-folder in this folder.
     * <p>
     * This is an admin only operation
     *
     * @return
     *      newly created folder.
     */
    public JNFileFolder createFolder(final String name, final String description) throws ProcessingException {
        return new Scraper<JNFileFolder>("failed to create a new folder "+name+" in "+name) {
            protected JNFileFolder scrape() throws IOException, SAXException, ProcessingException {
                WebResponse response = goTo(project._getURL()+"/servlets/ProjectFolderAdd?folderID="+id);

                WebForm form = Util.getFormWithAction(response, "ProjectFolderAdd");
                form.setParameter("name",name);
                form.setParameter("description",description);
                response = checkError(form.submit());

                // make sure there's no error
                if(response.getText().indexOf("Validation error")!=-1)
                    throw new ProcessingException("failed to create a folder");

                subFolders = null;
                return getSubFolder(name);
            }
        }.run();
    }

    /**
     * Removes this folder.
     */
    public void delete() throws ProcessingException {
        new Scraper("error deleting folder "+name) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                WebResponse r = goTo(
                    project._getURL()+"/servlets/ProjectFolderDelete?folderID="+id);

                checkError(Util.getFormWithAction(r,"ProjectFolderDelete").submit());

                parent.reset();
                return null;
            }
        }.run();
    }


    /**
     * Checks if the specified file exists in the folder.
     *
     * @deprecated
     *      use <code>getFiles().containsKey(fileName)</code>
     */
    public boolean existsFile( String fileName ) throws ProcessingException {
        return getFiles().containsKey(fileName);
    }
    
    /**
     * Deletes a file from the folder.
     *
     * @deprecated
     *      use <code>getFiles().get(fileName).delete()</code>
     */
    public void deleteFile( String fileName ) throws ProcessingException {
        JNFile file = getFiles().get(fileName);
        if(file==null)
            throw new ProcessingException("no such file "+fileName);
        file.delete();
    }
    
    /**
     * Moves to the URL of the folder if necessary
     */
    private void setCurrentPage() throws IOException, SAXException, ProcessingException {
        if( getCurrentPage().getURL().toExternalForm().equals(url) )
            return;
        goTo(url);
    }

    /**
     * Forces a reparse.
     */
    /*package*/ void reset() {
        subFolders = null;
        files = null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JNFileFolder)) return false;

        final JNFileFolder that = (JNFileFolder) o;

        return this.id==that.id && this.project==that.project;
    }

    public int hashCode() {
        return id + project.hashCode()*29;
    }
}
