package org.opensilex.core.data.dal;

import org.bson.Document;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.security.account.dal.AccountModel;

import java.net.URI;
import java.time.Instant;
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