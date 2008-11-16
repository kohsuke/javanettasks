package org.kohsuke.jnt;

/**
 * @author Kohsuke Kawaguchi
 */
public final class JNIssueSubcomponent extends JNObject {
    private final String name;

    public JNIssueSubcomponent(JNIssueComponent component, String name) {
        super(component);
        this.name = name;    
    }
}
