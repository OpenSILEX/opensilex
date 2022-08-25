package org.opensilex.core.exception;

import java.net.URI;

public class NoVariableDimensionsException extends Exception {
    private final URI variable;

    public NoVariableDimensionsException(URI variable) {
        super("The variable " + variable.toString() + " has not enough dimensions defined");
        this.variable = variable;
    }

    public URI getVariable() {
        return variable;
    }
}