package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;

import java.util.Collection;

public interface DistributedDataSourceCoordinator<M extends MongoOperation,S extends SparqlOperation> {

    void addMongoOperation(M operation);

    void addSparqlOperation(S operation);

    Collection<M> getMongoOperations();

    Collection<S> getSparqlOperations();

    void run(boolean sparqlFirst) throws Exception;

}
