package org.kohsuke.jnt;

import java.io.IOException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.meterware.httpunit.HTMLSegment;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

/**
 * Membership of a project.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JNMembership {
    
    private final JNProject project;
    private final WebConversation wc;
    
    protected JNMembership(JNProject project) {
        this.wc = project.wc;
        this.project = project;
    }

    /**
     * Grants the specified role request.
     */
    public void grantRole( JNUser user, String roleName ) throws ProcessingException {
        try {
            WebResponse r = wc.getResponse(project.getURL()+"/servlets/ProjectMemberAdd");
            WebForm form = r.getFormWithName("ProjectMemberAddForm");
            
            if(form==null)
                throw new IllegalStateException("form not found in "+r.getURL());
                
            form.setParameter("massAdd",user.getName());
            form.setParameter("roles", Util.getOptionValueFor(form,"roles",roleName) );
            
            SubmitButton submitButton = form.getSubmitButton("Button");
            if(submitButton==null)
                throw new IllegalStateException("no grant role button");
            r = form.submit(submitButton);
            
            if( r.getURL().toExternalForm().endsWith("ProjectMemberList") )
                return; // successful
            
            throw new ProcessingException("failed to grant role to "+user.getName());
        } catch( IOException e ) {
            throw new ProcessingException("failed to grant role to "+user.getName(),e);
        } catch( SAXException e ) {
            throw new ProcessingException("failed to grant role to "+user.getName(),e);
        }
    }
    
    
    /**
     * Grants the specified role request.
     * 
     * @deprecated
     * @see #grantRole(JNUser, String)
     */
    public void grantRole( String userName, String roleName ) throws ProcessingException {
        grantRole( project.net.getUser(userName), roleName );
    }

    /**
     * Declines the specified role request.
     * 
     * The role request has to be pending. In other words, this method is not
     * for revoking a role from an user.
     * 
     * @deprecated
     * @see #declineRole(JNUser, String, String)
     */
    public void declineRole( String userName, String roleName, String reason ) throws ProcessingException {
        declineRole( project.net.getUser(userName), roleName, reason );
    }
    
    /**
     * Declines the specified role request.
     * 
     * The role request has to be pending. In other words, this method is not
     * for revoking a role from an user.
     */
    public void declineRole( JNUser user, String roleName, String reason ) throws ProcessingException {
        try {
            WebResponse r = wc.getResponse(project.getURL()+"/servlets/ProjectMemberList");
            WebForm form = r.getFormWithName("ProjectMemberListPendingForm");
            
            if(form==null)
                throw new ProcessingException("form not found");
            
            WebTable t = findPendingRoleTable(r);
            if(t==null)
                throw new ProcessingException("the table in the form was not found");
            
            boolean foundRequest = false;
            for( int i=1; i<t.getRowCount(); i++ ) {
                if( !t.getCellAsText(i,0).trim().equals(user.getName()) ) continue;
                if( !t.getCellAsText(i,2).trim().equals(roleName) ) continue;
                
                 TableCell opCell = t.getTableCell(i,3);
                 Element input = Util.getFirstElementChild((Element)opCell.getDOM());
                 if(!input.getTagName().equalsIgnoreCase("input"))
                    throw new ProcessingException("expected input tag but found "+input.getTagName());
                 
                 String fieldName = input.getAttribute("name");
                 form.setParameter(fieldName,"Disapprove");
                 foundRequest = true;
                 break;
            }
            if(!foundRequest)
                throw new ProcessingException("request was not found on the web "+user.getName());
            
            form.setParameter("disapprovalReason",reason);
            
            form.submit(form.getSubmitButton("Button","Submit"));
            
        } catch( IOException e ) {
            throw new ProcessingException("error revoking role from "+user.getName(),e);
        } catch( SAXException e ) {
            throw new ProcessingException("error revoking role from "+user.getName(),e);
        }
    }
    
    private WebTable findPendingRoleTable( HTMLSegment r ) throws SAXException {
        
        WebTable[] tables = r.getTables();
        for( int i=0; i<tables.length; i++ ) {
            WebTable tbl = tables[i];

//          System.out.println(tbl.getRowCount()+"x"+tbl.getColumnCount());
//          System.out.println("["+tbl.getCellAsText(0,0)+"]");
    
            // recursive search
            for (int j = 0; j < tbl.getRowCount(); j++) {
                for (int k = 0; k < tbl.getColumnCount(); k++) {
                    TableCell cell = tbl.getTableCell(j,k);
                    if (cell != null) {
                        WebTable tt = findPendingRoleTable(cell);
                        if(tt!=null)    return tt;
                    }
                }
            }
            
            if( tbl.getColumnCount()<4 )
                continue;
            if( tbl.getCellAsText(0,2).indexOf("Roles requested")==-1 )
                continue;
            if( tbl.getCellAsText(0,3).indexOf("Operations")==-1 )
                continue;
            
            return tbl; // bingo
        }
        
        return null;
    }
}
