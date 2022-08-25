package org.opensilex.core.exception;

import java.net.URI;

public class VariableDimensionsException extends Exception {
    private final URI variable;

    public VariableDimensionsException(URI variable) {
        super("The variable " + variable.toString() + " should not have dimensions defined");
        this.variable = variable;
    }

    public URI getVariable() {
        return variable;
    }
}