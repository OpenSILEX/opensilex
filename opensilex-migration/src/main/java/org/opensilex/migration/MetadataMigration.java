package org.opensilex.migration;

import org.apache.jena.sparql.core.mem.TupleSlot;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;

public class MetadataMigration extends DatabaseMigrationModuleUpdate {
    @Override
    public String getDescription() {
        return "Rename dc:creator with dc:publisher and dc:created with dc:issued";
    }

    @Override
    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) {
        return true;
    }

    @Override
    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws Exception {
        sparql.renameTripleURI(URI.create(DCTerms.creator.getURI()), URI.create(DCTerms.publisher.getURI()), TupleSlot.PREDICATE, true);
        sparql.renameTripleURI(URI.create(DCTerms.created.getURI()), URI.create(DCTerms.issued.getURI()), TupleSlot.PREDICATE, true);
    }
}
