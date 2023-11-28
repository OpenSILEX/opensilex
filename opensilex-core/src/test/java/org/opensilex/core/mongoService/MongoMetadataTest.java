package org.opensilex.core.mongoService;

import com.mongodb.client.MongoCollection;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.time.Instant;

/**
 * @author Hamza Ikiou
 * This class is about testing the publisher, creation date and the last modification date for any MongoModel
 */
public class MongoMetadataTest extends AbstractMongoIntegrationTest {

    private static final String MONGO_MODEL_TEST_COLLECTION_NAME = "mongoModelTest";
    private static final String MONGO_MODEL_TEST_PREFIX = "mongoModelTest";
    private static MongoDBService mongo;

    private MongoModelTest getModel() {
        return new MongoModelTest();
    }

    @BeforeClass
    public static void initialisation() {
        mongo = getMongoDBService();
    }

    @Test
    public void create() throws Exception {

        //configuring the model with an uri and a publisher
        MongoModelTest model = this.getModel();
        model.setUri(URI.create("test:MongoMetadataTest_model1"));
        model.setPublisher(URI.create("test:id/user/account.adminopensilexorg"));

        //creating the model
        MongoCollection<MongoModelTest> collection = mongo.getDatabase().getCollection(MONGO_MODEL_TEST_COLLECTION_NAME, MongoModelTest.class);
        mongo.create(model, collection, MONGO_MODEL_TEST_PREFIX, null);

        //get the model created with a findByURI
        MongoModelTest modelFromGetMethod = mongo.findByURI(collection, model.getUri());

        //check if all metadata are established
        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());
        Assert.assertNull(modelFromGetMethod.getLastUpdateDate());
        Assert.assertEquals(model.getUri(), modelFromGetMethod.getUri());
        Assert.assertEquals(model.getPublisher(), modelFromGetMethod.getPublisher());
        Assert.assertEquals(model.getPublicationDate().getEpochSecond(), modelFromGetMethod.getPublicationDate().getEpochSecond());
    }

    @Test
    public void updateAfterCreate() throws Exception {
        //configuring the model with an uri
        MongoModelTest model = this.getModel();
        model.setUri(URI.create("test:MongoMetadataTest_model2"));
        model.setPublisher(URI.create("test:id/user/account.adminopensilexorg"));

        //creating the model
        MongoCollection<MongoModelTest> collection = mongo.getDatabase().getCollection(MONGO_MODEL_TEST_COLLECTION_NAME, MongoModelTest.class);
        mongo.create(model, collection, MONGO_MODEL_TEST_PREFIX, null);

        //get the model created with a findByURI
        MongoModelTest modelFromGetMethod = mongo.findByURI(collection, model.getUri());

        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());

        //setting new information before the update
        String labelBeforeUpdate = modelFromGetMethod.getLabel();
        Instant lastUpdatedDateBeforeUpdate = modelFromGetMethod.getLastUpdateDate();
        modelFromGetMethod.setLabel("updated");

        //updating the model
        mongo.update(modelFromGetMethod, collection, MongoModel.URI_FIELD, null);

        //get the model updated with a findByURI
        MongoModelTest newModelFromGet = mongo.findByURI(collection, modelFromGetMethod.getUri());

        //check if all metadata are established
        Assert.assertNotNull(newModelFromGet);
        Assert.assertNotNull(newModelFromGet.getPublicationDate());
        Assert.assertNotNull(newModelFromGet.getLastUpdateDate());
        Assert.assertNotEquals(labelBeforeUpdate, newModelFromGet.getLabel());
        Assert.assertNotEquals(lastUpdatedDateBeforeUpdate, newModelFromGet.getLastUpdateDate());
        Assert.assertEquals(modelFromGetMethod.getUri(), newModelFromGet.getUri());
        Assert.assertEquals(modelFromGetMethod.getPublisher(), newModelFromGet.getPublisher());
        Assert.assertEquals(modelFromGetMethod.getPublicationDate(), newModelFromGet.getPublicationDate());

    }

    @Test
    public void updateAfterUpdate() throws Exception {
        //configuring the model with an uri
        MongoModelTest model = this.getModel();
        model.setUri(URI.create("test:MongoMetadataTest_model3"));
        model.setPublisher(URI.create("test:id/user/account.adminopensilexorg"));

        //creating the model
        MongoCollection<MongoModelTest> collection = mongo.getDatabase().getCollection(MONGO_MODEL_TEST_COLLECTION_NAME, MongoModelTest.class);
        mongo.create(model, collection, MONGO_MODEL_TEST_PREFIX, null);

        //get the model created with a findByURI
        MongoModelTest modelFromGetMethod = mongo.findByURI(collection, model.getUri());

        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());

        //setting new information before the update
        String labelBeforeUpdate = modelFromGetMethod.getLabel();
        Instant lastUpdatedDateBeforeUpdate = modelFromGetMethod.getLastUpdateDate();
        modelFromGetMethod.setLabel("updated");

        //updating the model
        mongo.update(modelFromGetMethod, collection, MongoModel.URI_FIELD, null);

        //get the first model updated with a findByURI
        MongoModelTest firstUpdatedModel = mongo.findByURI(collection, modelFromGetMethod.getUri());

        Assert.assertNotNull(firstUpdatedModel);
        Assert.assertNotNull(firstUpdatedModel.getPublicationDate());
        Assert.assertNotNull(firstUpdatedModel.getLastUpdateDate());
        Assert.assertNotEquals(labelBeforeUpdate, firstUpdatedModel.getLabel());
        Assert.assertNotEquals(lastUpdatedDateBeforeUpdate, firstUpdatedModel.getLastUpdateDate());

        //setting new information before the second update
        String labelBeforeSecondUpdate = firstUpdatedModel.getLabel();
        Instant lastUpdatedDateBeforeSecondUpdate = firstUpdatedModel.getLastUpdateDate();
        firstUpdatedModel.setLabel("second update");

        //updating again to check the new values
        mongo.update(firstUpdatedModel, collection, MongoModel.URI_FIELD, null);

        //get the second model updated with a findByURI
        MongoModelTest secondUpdatedModel = mongo.findByURI(collection, firstUpdatedModel.getUri());

        //check if all metadata are established
        Assert.assertNotNull(secondUpdatedModel);
        Assert.assertNotNull(secondUpdatedModel.getPublicationDate());
        Assert.assertNotNull(secondUpdatedModel.getLastUpdateDate());
        Assert.assertNotEquals(labelBeforeSecondUpdate, secondUpdatedModel.getLabel());
        Assert.assertNotEquals(lastUpdatedDateBeforeSecondUpdate, secondUpdatedModel.getLastUpdateDate());
        Assert.assertEquals(firstUpdatedModel.getUri(), secondUpdatedModel.getUri());
        Assert.assertEquals(firstUpdatedModel.getPublisher(), secondUpdatedModel.getPublisher());
        Assert.assertEquals(firstUpdatedModel.getPublicationDate(), secondUpdatedModel.getPublicationDate());

    }
}
