package org.opensilex.sparql.exceptions;

/**
 * An exception thrown when a model cannot be inserted into the triplestore because it doesn't meet specific
 * requirements.
 *
 * @author Valentin RIGOLLE
 */
public class SPARQLInvalidModelException extends SPARQLException {
    public SPARQLInvalidModelException() {
    }

    public SPARQLInvalidModelException(String message) {
        super(message);
    }
}
