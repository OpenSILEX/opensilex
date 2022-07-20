package org.opensilex.nosql.datasource.coordinator;

import com.mongodb.client.ClientSession;
import org.opensilex.nosql.datasource.operation.DataSourceOperation.OPERATION_STATE;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.simplejavamail.api.internal.clisupport.model.Cli;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractDistributedCoordinator<M extends MongoOperation,S extends SparqlOperation>
        implements DistributedDataSourceCoordinator<M,S>{

    private final List<M> mongoOperations;
    private final List<S> sparqlOperations;

    protected final SPARQLService sparql;
    protected final MongoDBService mongodb;

    public AbstractDistributedCoordinator(SPARQLService sparql, MongoDBService mongodb) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.mongoOperations = new LinkedList<>();
        this.sparqlOperations = new LinkedList<>();
    }

    @Override
    public Collection<M> getMongoOperations() {
        return mongoOperations;
    }

    @Override
    public Collection<S> getSparqlOperations() {
        return sparqlOperations;
    }

    @Override
    public void addMongoOperation(M operation) {
        Objects.requireNonNull(operation);
        this.mongoOperations.add(operation);
    }

    @Override
    public void addSparqlOperation(S operation) {
        Objects.requireNonNull(operation);
        this.sparqlOperations.add(operation);
    }

}
