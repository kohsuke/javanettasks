package org.kohsuke.jnt;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.text.ParseException;

import org.xml.sax.SAXException;
import org.dom4j.Document;
import org.dom4j.Element;

import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;

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
public final class JNFileFolder {
    
    /*package*/ final JNProject project;
    /*package*/ final WebConversation wc;
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
        this.wc = project.wc;
        this.project = project;
        this.parent = parent;
        this.name = name;
        this.id = id;
        this.url = project.getURL()+"/servlets/ProjectDocumentList?folderID="+id+"&expandFolder="+id;
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
                WebResponse response = wc.getResponse(url);
                Document dom = Util.getDom4j(response);

                // find the current folder
                Element current = (Element)dom.selectSingleNode("//DIV[@id='projectdocumentlist']//LI[@class='selection']");

                // parse sub folders
                subFolders = new TreeMap<String,JNFileFolder>();
                for( Element anchor : (List<Element>)current.selectNodes("UL/LI/A[SPAN]") ) {
                    // https://jaxb.dev.java.net/servlets/ProjectDocumentList?folderID=1747&expandFolder=1747
                    String name = anchor.getTextTrim();
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

    /**
     * Uploads a file to the folder.
     * 
     * @param fileStatus
     *      can be null.
     */
    public void uploadFile( final String fileName, final String description, final FileStatus fileStatus, final File fileToUpload ) throws ProcessingException {
        new Scraper("error uploading a file "+fileToUpload) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                if(!fileToUpload.exists() || !fileToUpload.isFile())
                    throw new IOException(fileToUpload+" is not a file");

                setCurrentPage();

                WebResponse r = wc.getCurrentPage();

                WebLink addFileLink = r.getLinkWith("Add new file");
                if(addFileLink==null) {
                    throw new ProcessingException("Unable to find 'add new file' link. Does this account have a permission to post a file?");
                }
                r = addFileLink.click();

                WebForm form = r.getFormWithName("ProjectDocumentAddForm");
                form.setParameter("name",fileName);
                if( fileStatus!=null )
                    form.setParameter("status",fileStatus.toString());
                form.setParameter("description",description);

                form.setParameter("type","file");
                form.setParameter("file",new UploadFileSpec[]{
                    new UploadFileSpec(fileToUpload)});
                r = form.submit();

                if( r.getImageWithAltText("Alert notification")!=null )
                    // TODO: obtain the error message
                    throw new ProcessingException("error uploading a file "+fileToUpload);
                return null;
            }
        }.run();
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
                WebResponse response = wc.getResponse(project.getURL()+"/servlets/ProjectFolderAdd?folderID="+id);

                WebForm form = response.getFormWithName("ProjectFolderAddForm");
                form.setParameter("name",name);
                form.setParameter("description",description);
                response = form.submit();

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
            protected Object scrape() throws IOException, SAXException {
                WebResponse r = wc.getResponse(
                    project.getURL()+"/servlets/ProjectFolderDelete?folderID="+id);

                r = r.getFormWithName("ProjectFolderDeleteForm").submit();

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
    private void setCurrentPage() throws IOException, SAXException {
        if( wc.getCurrentPage().getURL().toExternalForm().equals(url) )
            return;
        wc.getResponse(url);
    }

    /**
     * Forces a reparse.
     */
    /*package*/ void reset() {
        subFolders = null;
        files = null;
    }
}
