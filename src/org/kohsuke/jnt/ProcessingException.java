/*
 * Created on Aug 7, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt;

/**
 * Signals processing error.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ProcessingException extends Exception {

	public ProcessingException() {
		super();
	}

	public ProcessingException(String message) {
		super(message);
	}

	public ProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessingException(Throwable cause) {
		super(cause);
	}
}
