package org.opensilex.nosql.datasource.coordinator;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.Test;
import org.opensilex.nosql.datasource.operation.DataSourceOperation;
import org.opensilex.nosql.datasource.operation.MongoOperation;
import org.opensilex.nosql.datasource.operation.SparqlOperation;
import org.opensilex.nosql.datasource.saga.MongoSagaOperation;
import org.opensilex.nosql.datasource.saga.SagaDistributedCoordinator;
import org.opensilex.nosql.datasource.saga.SparqlSagaOperation;
import org.opensilex.nosql.models.MongoTestModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SparqlAndMongoCoordinatorTest {

    protected SPARQLService sparql;
    protected MongoDBService mongoDB;

    protected final MongoCollection<MongoTestModel> testCollection = mongoDB.getDatabase().getCollection("SparqlAndMongoCoordinatorTest",MongoTestModel.class);

    protected List<MongoTestModel> getMongoModels(int n){
        List<MongoTestModel> models = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            MongoTestModel model = new MongoTestModel();
            model.setInteger(i);
            model.setString("test"+i);
            model.setBool(true);
            model.setBytes(("test_bytes"+i).getBytes());
            models.add(model);
        }
        return models;
    }

    protected List<SPARQLNamedResourceModel> getSparqlModels(int n){
        List<SPARQLNamedResourceModel> models = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            SPARQLNamedResourceModel<?> model = new SPARQLNamedResourceModel<>();
            model.setName("name_"+i);
            models.add(model);
        }
        return models;
    }

    @Test
    public void testInsert() throws Exception {

        ClientSession mongoSession = mongoDB.startSession();
        DefaultDataSourceCoordinator<DataSourceOperation<?>> coordinator = new DefaultDataSourceCoordinator<>(sparql,mongoSession);
        coordinator.addOperation(mongoSession, new MongoOperation((session) -> {
            testCollection.insertMany(session, getMongoModels(100));
        }));

        coordinator.addOperation(sparql, new SparqlOperation((service) -> {
            service.create(SPARQLNamedResourceModel.class,getSparqlModels(100));
        }));
        coordinator.run();
    }

    @Test
    public void testInsertSaga() throws Exception {

        ClientSession mongoSession = mongoDB.startSession();
        SagaDistributedCoordinator coordinator = new SagaDistributedCoordinator(sparql, mongoSession);
        coordinator.addOperation(mongoSession, new MongoSagaOperation(
                mongoDB,
                (session) -> testCollection.insertMany(session, getMongoModels(100)),
                (compensateSession) -> testCollection.deleteMany(compensateSession,new Document())
        ));

        List<SPARQLNamedResourceModel> sparqlModels = getSparqlModels(100);
        coordinator.addOperation(sparql, new SparqlSagaOperation(
                (service) -> service.create(SPARQLNamedResourceModel.class, sparqlModels),
                (service) -> {
                    service.deleteAll(null, sparqlModels
                            .stream()
                            .map(SPARQLNamedResourceModel::getUri)
                            .collect(Collectors.toList()));
                }
        ));

        coordinator.run();
    }

}