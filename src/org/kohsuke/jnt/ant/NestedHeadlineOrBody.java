/*
 * $Id: NestedHeadlineOrBody.java 56 2003-12-17 05:40:20Z ryan_shoemaker $
 *  
 */
package org.kohsuke.jnt.ant;

/**
 * This class handles nested headline and body elements of the NewsItemTask.
 * 
 * @author Ryan Shoemaker
 * @version $Revision: 56 $
 */
public class NestedHeadlineOrBody {

    private String content;

    public NestedHeadlineOrBody() {
    }

    public void addText(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
