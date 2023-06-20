package org.opensilex.core.mongoService;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.model.C;
import org.opensilex.sparql.model.SPARQLLabel;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;

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
        mongo.create(model, MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, MONGO_MODEL_TEST_PREFIX);

        //get the model created with a findByURI
        MongoModelTest modelFromGetMethod = mongo.findByURI(MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, model.getUri());

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
        mongo.create(model, MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, MONGO_MODEL_TEST_PREFIX);

        //get the model created with a findByURI
        MongoModelTest modelFromGetMethod = mongo.findByURI(MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, model.getUri());

        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());

        //setting new information before the update
        String labelBeforeUpdate = modelFromGetMethod.getLabel();
        Instant lastUpdatedDateBeforeUpdate = modelFromGetMethod.getLastUpdateDate();
        modelFromGetMethod.setLabel("updated");

        //updating the model
        mongo.update(modelFromGetMethod, MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME);

        //get the model updated with a findByURI
        MongoModelTest newModelFromGet = mongo.findByURI(MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, modelFromGetMethod.getUri());

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
        mongo.create(model, MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, MONGO_MODEL_TEST_PREFIX);

        //get the model created with a findByURI
        MongoModelTest modelFromGetMethod = mongo.findByURI(MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, model.getUri());

        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());

        //setting new information before the update
        String labelBeforeUpdate = modelFromGetMethod.getLabel();
        Instant lastUpdatedDateBeforeUpdate = modelFromGetMethod.getLastUpdateDate();
        modelFromGetMethod.setLabel("updated");

        //updating the model
        mongo.update(modelFromGetMethod, MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME);

        //get the first model updated with a findByURI
        MongoModelTest firstUpdatedModel = mongo.findByURI(MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, modelFromGetMethod.getUri());

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
        mongo.update(firstUpdatedModel, MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME);

        //get the second model updated with a findByURI
        MongoModelTest secondUpdatedModel = mongo.findByURI(MongoModelTest.class, MONGO_MODEL_TEST_COLLECTION_NAME, firstUpdatedModel.getUri());

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
