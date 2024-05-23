package org.opensilex.core.data.dal;

import java.net.URI;
import java.util.Collection;

/**
 * Extends DataFileSearchFilter as everything is the same apart from the extra variables field
 */
public class DataSearchFilter extends DataFileSearchFilter {

    Collection<URI> variables;

    public Collection<URI> getVariables() {
        return variables;
    }

    public DataSearchFilter setVariables(Collection<URI> variables) {
        this.variables = variables;
        return this;
    }
}