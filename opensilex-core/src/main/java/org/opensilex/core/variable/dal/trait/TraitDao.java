package org.opensilex.core.variable.dal.trait;

import org.opensilex.sparql.service.SPARQLService;

public class TraitDao {

    protected final SPARQLService sparql;

    public TraitDao(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public TraitModel create(TraitModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }
}
