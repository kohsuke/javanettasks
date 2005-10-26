package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

/**
 * @author Kohsuke Kawaguchi
 */
public interface ConnectionFactory {
    JavaNet connect() throws ProcessingException;
}
