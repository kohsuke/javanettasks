package org.kohsuke.jnt;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    private final WebConversation wc;
    private final String url;

    private final int id;

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

    protected JNFileFolder(JNProject project, JNFileFolder parent, int id) {
        this.wc = project.wc;
        this.project = project;
        this.parent = parent;
        this.id = id;
        this.url = "https://jaxb.dev.java.net/servlets/ProjectDocumentList?folderID="+id+"&expandFolder="+id;
    }
    
    /**
     * Returns a sub-folder.
     * 
     * @param folderName
     *      '/'-separated folder name. such as "abc", "abc/def", or "/abc/def".
     */
    public JNFileFolder getSubFolder( final String folderName ) throws ProcessingException {
        return (JNFileFolder)new Scraper("failed to cd into "+folderName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                // chdir to root
                if( folderName.startsWith("/"))
                    wc.getResponse(project.getURL()+"/servlets/ProjectDocumentList");
                else
                    wc.getResponse(url);

                StringTokenizer tokens = new StringTokenizer(folderName,"/");
                while( tokens.hasMoreTokens() ) {
                    String dirName = tokens.nextToken();
        //          System.out.println("cd "+dirName);
                    // find the link target
                    Util.findLink( wc,
                        dirName,project.getURL()+"/servlets/ProjectDocumentList?folderID=").click();
                }

                return project.getFolderFromURL(wc.getCurrentPage().getURL().toExternalForm());
            }
        }.run();
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

                    subFolders.put(name,new JNFileFolder(project,JNFileFolder.this,id));
                }

                // find the current folder
                current = (Element)dom.selectSingleNode("//DIV[@id='projectdocumentlist']//TD[@class='filebrowse']/DIV/TABLE");

                files = new TreeMap<String,JNFile>();

                List trs = current.elements("TR");
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
     * Uploads a file to the folder.
     * 
     * @param fileStatus
     *      can be null.
     */
    public void uploadFile( final String fileName, final String description, final String fileStatus, final File fileToUpload ) throws ProcessingException {
        new Scraper("error uploading a file "+fileToUpload) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
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
                    form.setParameter("status",fileStatus);
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
     * Gets a document id of the given file or null if such a file doesn't exist.
     */
    private String getDocumentId( final String fileName ) throws ProcessingException {
        return (String)new Scraper("Unable to fetch the document id for "+fileName) {
            protected Object scrape() throws SAXException {
                try {
                    WebLink link = Util.findLink(wc,fileName,
                        project.getURL()+"/servlets/ProjectDocumentView?documentID=");
                    String url = link.getURLString();
                    return url.substring(url.indexOf('=')+1,url.indexOf('&'));
                } catch( ProcessingException e ) {
                    try {
                        WebLink link = Util.findLink(wc,fileName,"/files/documents/");
                        String url = link.getURLString();
                        int l = url.lastIndexOf('/');
                        return url.substring(url.lastIndexOf('/',l-1)+1,l);
                    } catch( ProcessingException ee ) {
                        return null;
                    }
                }
            }
        }.run();
    }
    
    /**
     * Checks if the specified file exists in the folder. 
     */
    public boolean existsFile( final String fileName ) throws ProcessingException {
        String id = (String)new Scraper("error checking file "+fileName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                setCurrentPage();
                return getDocumentId(fileName);
            }
        }.run();

        return id!=null;
    }
    
    /**
     * Delets a file from the folder.
     */
    public void deleteFile( final String fileName ) throws ProcessingException {
        new Scraper("error deleting file "+fileName) {
            protected Object scrape() throws IOException, SAXException, ProcessingException {
                setCurrentPage();

                String documentId = getDocumentId(fileName);

                WebResponse r = wc.getResponse(
                    project.getURL()+"/servlets/ProjectDocumentDelete?documentID="+documentId+"&maxDepth=");

                r = r.getFormWithName("ProjectDocumentDeleteForm").submit();

                return null;
            }
        }.run();
    }
    
    /**
     * Moves to the URL of the folder if necessary
     */
    private void setCurrentPage() throws IOException, SAXException {
        if( wc.getCurrentPage().getURL().toExternalForm().equals(url) )
            return;
        wc.getResponse(url);
    }
}
