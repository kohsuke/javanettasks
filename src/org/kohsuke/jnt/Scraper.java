package org.kohsuke.jnt;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

/**
 * Used internally to confine the HTML scraping code
 * so that exceptions thrown by it can be handled in uniform way.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Scraper<T> {

    private final String errorSummary;

    /**
     * @param operationName
     *      Human-readable summary of this scraping operation.
     *      Used for error messages.
     */
    protected Scraper(String operationName) {
        this.errorSummary = operationName+' ';
    }

    public final T run() throws ProcessingException {
        try {
            return scrape();
        } catch( RuntimeException e ) {
            throw new ProcessingException(errorSummary+e.getMessage(),e);
        } catch( SAXException e ) {
            throw new ProcessingException(errorSummary+e.getMessage(),e);
        } catch( IOException e ) {
            throw new ProcessingException(errorSummary+e.getMessage(),e);
        } catch (ParseException e) {
            throw new ProcessingException(errorSummary+e.getMessage(),e);
        }
    }

    /**
     * Runs the scraping and returns a value.
     */
    protected abstract T scrape() throws IOException, SAXException, ProcessingException, ParseException ;
}
