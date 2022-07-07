package org.opensilex.nosql.mongodb;

import com.mongodb.client.ClientSession;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingConsumer;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class used to store a list of {@link ThrowingConsumer} for any SPARQL and mongodb operation
 * @author rcolin
 */
public class MultipleDataSourceOperation {

    /**
     * List of operations to run over mongodb with some {@link ClientSession}
     */
    private final List<ThrowingConsumer<ClientSession, Exception>> mongoConsumers;

    /**
     * List of operations to run over SPARQL with some {@link SPARQLService}
     * #TODO use a specific connection per operation (like mongodb) instead of the full service
     */
    private final List<ThrowingConsumer<SPARQLService, Exception>> sparqlConsumers;
    private boolean sparqlFirst;

    public MultipleDataSourceOperation(){
        mongoConsumers = new LinkedList<>();
        sparqlConsumers = new LinkedList<>();
        sparqlFirst = true;
    }

    /**
     * @param consumer custom MongoDB-based function/code to apply after transaction start and before transaction commit.
     */
    public MultipleDataSourceOperation addMongoConsumer(ThrowingConsumer<ClientSession, Exception> consumer){
        mongoConsumers.add(consumer);
        return this;
    }

    /**
     * @param consumer custom SPARQL based function/code to apply after transaction start and before transaction commit.
     */
    public MultipleDataSourceOperation addSparqlConsumer(ThrowingConsumer<SPARQLService, Exception> consumer){
        sparqlConsumers.add(consumer);
        return this;
    }

    public List<ThrowingConsumer<ClientSession, Exception>> getMongoConsumers() {
        return mongoConsumers;
    }

    public List<ThrowingConsumer<SPARQLService, Exception>> getSparqlConsumers() {
        return sparqlConsumers;
    }

    public boolean isSparqlFirst() {
        return sparqlFirst;
    }

    public MultipleDataSourceOperation setSparqlFirst(boolean sparqlFirst) {
        this.sparqlFirst = sparqlFirst;
        return this;
    }

    public boolean useMongoDb(){
        return ! mongoConsumers.isEmpty();
    }

    public boolean useSparql(){
        return ! sparqlConsumers.isEmpty();
    }
}
