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
 * <p>
 * This library heavily relies on HTML scraping, and because
 * java.net keeps changing HTML representation, there's a
 * non-ignorable risk that the scraping fails.
 *
 * <p>
 * There's also other risks, such as the server disconnects the connection,
 * the server send malformed HTML, etc.
 *
 * <p>
 * Many of the methods defined in this library throws this exception
 * when it has a problem parsing HTML.
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
