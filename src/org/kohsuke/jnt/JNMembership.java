package org.kohsuke.jnt;

import com.meterware.httpunit.HTMLSegment;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.SubmitButton;
import com.meterware.httpunit.TableCell;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Membership of a project.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Bruno Souza
 */
public class JNMembership extends JNObject {
    
    private final JNProject project;

    /**
     * Lazily created. {@link JNUser} to sets of {@link JNRole}s in this project.
     * @see #getMembers()
     */
    private Map<JNUser,Set<JNRole>> members;

    /**
     * Lazily created.
     * {@link JNRole} to sets of {@link JNUser}s that hae that role in this project.
     */
    private Map<JNRole,Set<JNUser>> roles;

    protected JNMembership(JNProject project) {
        super(project);
        this.project = project;
    }

    private void parseMembershipInfo() throws ProcessingException {
        // load all information that is on the membership pages

        members = new TreeMap<JNUser,Set<JNRole>>();
        roles = new TreeMap<JNRole,Set<JNUser>>();

        try {
            WebResponse response = goTo(project._getURL()+"/servlets/ProjectMemberList");

            while(true) {
                WebTable users = response.getTableStartingWithPrefix("User");

                if (users == null) {
                    // there's no member table.
                    // TODO: isn't that an error?
                    return;
                }

                users.purgeEmptyCells();

                int numRows = users.getRowCount();

                // we start from row 1, since row 0 is the header row.
                // and we ignore the last row, since it is the submit button

                // TODO: treat 2 special cases
                //       1) when the user has no permission on the page.
                //          In this case, the submit button does not shows. The code
                //          will work, but will not count one user in each page.
                //       2) in java-net project, theres a "User Group" section on
                //          the top of this table. This will work, but will give
                //          incorrect results, since it will count the "User" header
                //          and the groups as members.

                for (int r = 1; r < numRows-1; r++) {
                    JNUser user = root.getUser(users.getCellAsText(r,0).trim());

                    // when there are more then one role for a single user, the
                    // roleList are separated by commas. This is new layout
                    String cell = users.getCellAsText(r, 2);
                    StringTokenizer roleList = new StringTokenizer(cell,"\n");
                    Set<JNRole> ra = new TreeSet<JNRole>();
                    while(roleList.hasMoreTokens()) {
                        String roleName = roleList.nextToken().trim();
                        if(roleName.length()==0)    continue;
                        JNRole role = root.getRole(roleName);
                        ra.add(role);

                        Set<JNUser> l = roles.get(role);
                        if(l==null) {
                            roles.put(role,l=new TreeSet<JNUser>());
                        }
                        l.add(user);
                    }

                    members.put(user,ra);
                }

                WebLink nextPage = response.getLinkWith("Next");
                if(nextPage==null)
                    return; // done

                // continue to parse the next page
                response = nextPage.click();
            }
        } catch( SAXException e ) {
            throw new ProcessingException(e);
        } catch( IOException e ) {
            throw new ProcessingException(e);
        } catch( DOMException e ) {
            throw new ProcessingException(e);
        } catch(HttpException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Gets all the members of this project as a {@link Set} of {@link JNUser}s.
     *
     * @return
     *      the set can be empty, but always non-null. The set is read-only.
     */
    public Set getMembers() throws ProcessingException {
        if(members==null)
            parseMembershipInfo();
        return Collections.unmodifiableSet(members.keySet());
    }

    /**
     * Gets all the roles used in this project as a {@link Set} of {@link JNRole}s.
     *
     * @return
     *      the set can be empty, but always non-null. The set is read-only.
     */
    public Set getRoles() throws ProcessingException {
        if(roles==null)
            parseMembershipInfo();
        return Collections.unmodifiableSet(roles.keySet());
    }

    /**
     * Gets the {@link JNRole}s that a given user has in this project.
     *
     * <p>
     * This method returns a copy of the information, and changing
     * the values on the array will not grant/revoke roles.
     *
     * @return
     *      always return a read-only non-null (but possibly empty) set.
     */
    public Set<JNRole> getRolesOf(JNUser user) throws ProcessingException {
        if(members==null)
            parseMembershipInfo();
        Set<JNRole> r = members.get(user);
        if(r==null)
            return Collections.EMPTY_SET;

        return Collections.unmodifiableSet(r);
    }

    /**
     * Gets the {@link JNUser}s who has a specified role in this project.
     *
     * @return
     *      always return a read-only non-null (but possibly empty) set.
     */
    public Set<JNUser> getUserOf(JNRole role) throws ProcessingException {
        if(roles==null)
            parseMembershipInfo();
        Set<JNUser> s = roles.get(role);
        if(s==null)
            return Collections.EMPTY_SET;

        return Collections.unmodifiableSet(s);
    }


    /**
     * Grants the specified role request.
     */
    public void grantRole( JNUser user, String roleName ) throws ProcessingException {
        try {
            String url = project._getURL() + "/servlets/ProjectMemberAdd";
            WebResponse r = goTo(url);
            WebForm[] forms = r.getForms();
            WebForm form = forms[forms.length-1];   // last one is it
            
            if(form==null)
                throw new IllegalStateException("form not found in "+r.getURL());
                
            form.setParameter("massAdd",user.getName());
            form.setParameter("roles", Util.getOptionValueFor(form,"roles",roleName) );
            
            SubmitButton submitButton = form.getSubmitButton("Button");
            if(submitButton==null)
                throw new IllegalStateException("no grant role button");
            r = checkError(form.submit(submitButton));

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
        grantRole( root.getUser(userName), roleName );
    }


    /**
     * Revokes the specified role from the user.
     */
    public void revokeRole( JNUser user, String roleName ) throws ProcessingException {
        try {
            WebResponse r = goTo(project._getURL()+"/servlets/ProjectMemberList");
            WebForm form = Util.getFormWithAction(r,"ProjectMemberList");

            if(form==null)
                throw new IllegalStateException("form not found in "+r.getURL());

            // starts-with is necesasry because someone fails to handle &nbsp; correctly
            String propName  = (String)Util.getDom4j(r).selectObject(
                "string(//FORM[@action='ProjectMemberList']//TR[normalize-space(TD[1])='"+user.getName()+"']/TD[3]/text()[normalize-space(.)='"+roleName+"']/preceding-sibling::INPUT[1]/@name)");
            if(propName==null || propName.length()==0)
                throw new ProcessingException("Unable to find the user "+user.getName()+" or the role "+roleName);
            form.toggleCheckbox(propName);

            SubmitButton submitButton = form.getSubmitButton("Button");
            if(submitButton==null)
                throw new IllegalStateException("no submit button");
            r = checkError(form.submit(submitButton));

            if( r.getURL().toExternalForm().endsWith("ProjectMemberList") )
                return; // successful

            throw new ProcessingException("failed to revoke role from "+user.getName());
        } catch( IOException e ) {
            throw new ProcessingException("failed to revoke role from "+user.getName(),e);
        } catch( SAXException e ) {
            throw new ProcessingException("failed to revoke role from "+user.getName(),e);
        }
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
        declineRole( root.getUser(userName), roleName, reason );
    }
    
    /**
     * Declines the specified role request.
     * 
     * The role request has to be pending. In other words, this method is not
     * for revoking a role from an user.
     */
    public void declineRole( JNUser user, String roleName, String reason ) throws ProcessingException {
        try {
            WebResponse r = goTo(project._getURL()+"/servlets/ProjectMemberList");
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
            
            checkError(form.submit(form.getSubmitButton("Button","Submit")));
            
        } catch( IOException e ) {
            throw new ProcessingException("error revoking role from "+user.getName(),e);
        } catch( SAXException e ) {
            throw new ProcessingException("error revoking role from "+user.getName(),e);
        }
    }
    
    private WebTable findPendingRoleTable( HTMLSegment r ) throws SAXException {

        for( WebTable tbl : r.getTables() ) {

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
