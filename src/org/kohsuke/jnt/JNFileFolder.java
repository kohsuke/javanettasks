package org.kohsuke.jnt;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.xml.sax.SAXException;

import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;

/**
 * folder in the java&#x2#;net file sharing section.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNFileFolder {
    
    private final JNProject project;
    private final WebConversation wc;
    private final String url;
    
    protected JNFileFolder(JNProject project, String url) {
        this.wc = project.wc;
        this.project = project;
        this.url = url;
    }
    
    
    /**
     * Returns a sub-folder.
     * 
     * @param folderName
     *      '/'-separated folder name. such as "/abc/def" or "abc/def". 
     */
    public JNFileFolder getSubFolder( String folderName ) throws ProcessingException {
        try {
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
        } catch( IOException e ) {
            throw new ProcessingException("unable to cd to "+folderName,e);
        } catch( SAXException e ) {
            throw new ProcessingException("unable to cd to "+folderName,e);
        }
    }

    /**
     * Uploads a file to the folder.
     * 
     * @param fileStatus
     *      can be null.
     */
    public void uploadFile( String fileName, String description, String fileStatus, File fileToUpload ) throws ProcessingException {
        try {
            setCurrentPage();
            
            WebResponse r = wc.getCurrentPage();
            
            r = r.getLinkWith("Add a file").click();
            
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
        } catch( IOException e ) {
            throw new ProcessingException("error uploading a file "+fileToUpload,e);
        } catch( SAXException e ) {
            throw new ProcessingException("error uploading a file "+fileToUpload,e);
        }
    }
    
    /**
     * Gets a document id of the given file or null if such a file doesn't exist.
     */
    private String getDocumentId( String fileName ) throws ProcessingException {
        try {
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
        } catch( SAXException e ) {
            throw new ProcessingException("error processing page "+wc.getCurrentPage().getURL(),e);
        }
    }
    
    /**
     * Checks if the specified file exists in the folder. 
     */
    public boolean existsFile( String fileName ) throws ProcessingException {
        try {
            setCurrentPage();
                
            return getDocumentId(fileName)!=null;
        } catch( IOException e ) {
            throw new ProcessingException("error checking file "+fileName,e);
        } catch( SAXException e ) {
            throw new ProcessingException("error checking file "+fileName,e);
        }
    }
    
    /**
     * Delets a file from the folder.
     */
    public void deleteFile( String fileName ) throws ProcessingException {
        try {
            setCurrentPage();
            
            String documentId = getDocumentId(fileName);
        
            WebResponse r = wc.getResponse(
                project.getURL()+"/servlets/ProjectDocumentDelete?documentID="+documentId+"&maxDepth=");
            
            r = r.getFormWithName("ProjectDocumentDeleteForm").submit();
        } catch( IOException e ) {
            throw new ProcessingException("error deleting file "+fileName,e);
        } catch( SAXException e ) {
            throw new ProcessingException("error deleting file "+fileName,e);
        }
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
