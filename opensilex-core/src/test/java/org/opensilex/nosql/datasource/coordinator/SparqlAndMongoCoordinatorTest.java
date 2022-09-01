package org.opensilex.nosql.datasource.coordinator;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.nosql.datasource.operation.CompoundOperation;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.nosql.models.MongoTestModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.ws.rs.core.UriBuilder;
import java.util.*;

public class SparqlAndMongoCoordinatorTest extends AbstractMongoIntegrationTest {


    protected static final String SPARQL_GRAPH_NAME = "SparqlAndMongoCoordinatorTest";
    protected static final String MONGO_COLLECTION_NAME = "SparqlAndMongoCoordinatorTest";
    protected MongoCollection<MongoTestModel> testCollection;
    protected Node testGraph;

    @Before
    public void beforeClass() {
        testCollection = getMongoDBService().getDatabase().getCollection(MONGO_COLLECTION_NAME, MongoTestModel.class);
        testGraph = NodeFactory.createURI(
                UriBuilder.fromUri(getSparqlService().getBaseURI())
                        .path(SPARQLClassObjectMapper.DEFAULT_GRAPH_KEYWORD)
                        .path(SPARQL_GRAPH_NAME)
                        .toString()
        );

    }

    protected List<MongoTestModel> getMongoModels(int n) {
        List<MongoTestModel> models = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            MongoTestModel model = new MongoTestModel();
            model.setInteger(i);
            model.setString("test" + i);
            model.setBool(true);
            model.setBytes(("test_bytes" + i).getBytes());
            models.add(model);
        }
        return models;
    }

    protected List<SPARQLNamedResourceModel> getSparqlModels(int n, int offset) {
        List<SPARQLNamedResourceModel> models = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            SPARQLNamedResourceModel<?> model = new SPARQLNamedResourceModel<>();
            model.setName("name_" + i + offset);
            models.add(model);
        }
        return models;
    }

    @Test
    public void testInsert() throws Exception {

        SPARQLService sparql = getSparqlService();
        MongoDBService mongoDB = getMongoDBService();
        sparql.clearGraph(testGraph.toString());

        Assert.assertEquals(0, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(0, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));

        ClientSession mongoSession = mongoDB.startSession();
        DefaultDataSourceCoordinator coordinator = new DefaultDataSourceCoordinator(sparql, mongoSession);
        coordinator.addOperation(new MongoOperation(mongoSession, (session) ->
                testCollection.insertMany(session, getMongoModels(100))
        ));

        coordinator.addOperation(new SparqlOperation(sparql, (service) ->
                service.create(testGraph, getSparqlModels(100, 0)))
        );
        coordinator.run();

        Assert.assertEquals(100, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(100, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));
    }

    @Test
    public void testInsertMultiple() throws Exception {

        SPARQLService sparql = getSparqlService();
        MongoDBService mongoDB = getMongoDBService();

        Assert.assertEquals(0, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(0, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));

        ClientSession mongoSession = mongoDB.startSession();
        DefaultDataSourceCoordinator coordinator = new DefaultDataSourceCoordinator(sparql, mongoSession);

        // performs two mongo operations
        coordinator.addOperation(new MongoOperation(mongoSession, (session) -> {
            testCollection.insertMany(session, getMongoModels(100));
            testCollection.insertMany(session, getMongoModels(100));
        }));

        // similar to this
        coordinator.addOperation(new MongoOperation(mongoSession, (session) -> testCollection.insertMany(session, getMongoModels(200))));

        // performs two mongo operations
        coordinator.addOperation(new SparqlOperation(sparql, (service) -> {
            service.create(testGraph, getSparqlModels(100, 0));
            service.create(testGraph, getSparqlModels(100, 100));
        }));

        coordinator.addOperation(new SparqlOperation(sparql, (service) -> service.create(testGraph, getSparqlModels(200, 200))));

        coordinator.run();

        Assert.assertEquals(400, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(400, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));
    }

//    @Test
    /*
    public void testInsertSaga() throws Exception {

        SPARQLService sparql = getSparqlService();
        MongoDBService mongoDB = getMongoDBService();

        ClientSession mongoSession = mongoDB.startSession();
        SagaDistributedCoordinator coordinator = new SagaDistributedCoordinator(sparql, mongoSession);
        coordinator.addOperation(mongoSession, new MongoSagaOperation(
                mongoDB,
                (session) -> testCollection.insertMany(session, getMongoModels(100,0)),
                (compensateSession) -> testCollection.deleteMany(compensateSession,new Document())
        ));

        List<SPARQLNamedResourceModel> sparqlModels = getSparqlModels(100,0);
        coordinator.addOperation(sparql, new SparqlSagaOperation(sparql,
                (service) -> service.create(SPARQLNamedResourceModel.class, sparqlModels),
                (service) -> service.deleteAll(null, sparqlModels
                        .stream()
                        .map(SPARQLNamedResourceModel::getUri)
                        .collect(Collectors.toList()))
        ));

        coordinator.run();
    }
    */

    @Test
    public void testExecutionOrderOk() throws Exception {

        SPARQLService sparql = getSparqlService();
        MongoDBService mongoDB = getMongoDBService();

        List<Integer> executionOrder = new ArrayList<>();

        sparql.clearGraph(testGraph.toString());

        Assert.assertEquals(0, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(0, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));

        ClientSession mongoSession = mongoDB.startSession();
        DefaultDataSourceCoordinator coordinator = new DefaultDataSourceCoordinator(sparql, mongoSession);

        coordinator.addOperation(new MongoOperation(mongoSession, (session) -> {
            testCollection.insertMany(session, getMongoModels(100));
            executionOrder.add(1);
        }));

        coordinator.addOperation(new SparqlOperation(sparql, (service) -> {
            service.create(testGraph, getSparqlModels(100, 0));
            executionOrder.add(2);
        }));

        coordinator.addOperation(new MongoOperation(mongoSession, (session) -> {
            testCollection.insertMany(session, getMongoModels(100));
            executionOrder.add(3);
        }));

        coordinator.addOperation(new SparqlOperation(sparql, (service) -> {
            service.create(testGraph, getSparqlModels(100, 0));
            executionOrder.add(4);
        }));

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4), executionOrder);
    }

    @Test
    public void testCompoundOperationAndDirectNestedOperationEvaluation() throws Exception {

        SPARQLService sparql = getSparqlService();
        MongoDBService mongoDB = getMongoDBService();

        List<Integer> executionOrder = new ArrayList<>();

        sparql.clearGraph(testGraph.toString());

        Assert.assertEquals(0, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(0, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));

        ClientSession mongoSession = mongoDB.startSession();
        DefaultDataSourceCoordinator coordinator = new DefaultDataSourceCoordinator(sparql, mongoSession);

        coordinator.addOperation(new MongoOperation(mongoSession, (session) -> {
            testCollection.insertMany(session, getMongoModels(100));
            executionOrder.add(1);
        }));

        CompoundOperation compoundOperation = new CompoundOperation(
                coordinator,
                coordinator1 -> {
                    coordinator1.addOperation(new MongoOperation(mongoSession, (session) -> {
                        testCollection.insertMany(session, getMongoModels(100));
                        executionOrder.add(2);
                    }));

                    coordinator1.addOperation(new MongoOperation(mongoSession, (session) -> {
                        testCollection.insertMany(session, getMongoModels(100));
                        executionOrder.add(3);
                    }));
                },
                true
        );
        coordinator.addMixedOperation(compoundOperation);

        coordinator.addOperation(new SparqlOperation(sparql, (service) -> {
            service.create(testGraph, getSparqlModels(100, 0));
            executionOrder.add(4);
        }));

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4), executionOrder);
    }

    @Test
    public void testCompoundOperationAndUnDirectNestedOperationEvaluation() throws Exception {

        SPARQLService sparql = getSparqlService();
        MongoDBService mongoDB = getMongoDBService();

        List<Integer> executionOrder = new ArrayList<>();

        sparql.clearGraph(testGraph.toString());

        Assert.assertEquals(0, sparql.count(testGraph, SPARQLNamedResourceModel.class));
        Assert.assertEquals(0, mongoDB.count(MongoTestModel.class, MONGO_COLLECTION_NAME, new Document()));

        ClientSession mongoSession = mongoDB.startSession();
        DefaultDataSourceCoordinator coordinator = new DefaultDataSourceCoordinator(sparql, mongoSession);

        coordinator.addOperation(new MongoOperation(mongoSession, (session) -> {
            testCollection.insertMany(session, getMongoModels(100));
            executionOrder.add(1);
        }));

        CompoundOperation compoundOperation = new CompoundOperation(
                coordinator,
                coordinator1 -> {
                    coordinator1.addOperation(new MongoOperation(mongoSession, (session) -> {
                        testCollection.insertMany(session, getMongoModels(100));
                        executionOrder.add(3);
                    }));

                    coordinator1.addOperation(new MongoOperation(mongoSession, (session) -> {
                        testCollection.insertMany(session, getMongoModels(100));
                        executionOrder.add(4);
                    }));
                },
                true
        );
        coordinator.addMixedOperation(compoundOperation);

        coordinator.addOperation(new SparqlOperation(sparql, (service) -> {
            service.create(testGraph, getSparqlModels(100, 0));
            executionOrder.add(2);
        }));

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4), executionOrder);
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return Collections.singletonList(MONGO_COLLECTION_NAME);
    }
}