package org.kohsuke.jnt;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class TestCaseBase extends TestCase {
    JavaNet con;

    protected TestCaseBase() {
        setName(this.getClass().getName());
    }

    public void setUp() throws ProcessingException {
        // specify the password as
        // maven -Dmaven.junit.sysproperties=password -Dpassword=****
        con = JavaNet.connect("javanettaskstest",System.getProperty("password"));
    }

    public void tearDown() {
        con = null;
    }

}
