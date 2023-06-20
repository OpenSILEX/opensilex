package org.opensilex.sparql;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;
import org.opensilex.unit.test.AbstractUnitTest;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * @author Hamza Ikiou
 * This class is about testing the publisher, creation date and the last modification date for any SPARQLResourceModel
 */
public class SPARQLMetadataTest extends AbstractUnitTest {
    private static SPARQLService sparql;

    private C getModel() {
        return new C();
    }

    @BeforeClass
    public static void setupSPARQL() throws Exception {
        SPARQLServiceFactory factory = new RDF4JInMemoryServiceFactory();
        factory.setOpenSilex(opensilex);
        factory.setup();
        factory.startup();
        factory.getMapperIndex().addClasses(C.class);
        sparql = factory.provide();
    }

    @Test
    public void create() throws Exception {

        //configuring the model with an uri and a publisher
        C model = this.getModel();
        model.setUri(URI.create("test:SPARQLMetadataTest_c1"));
        model.setPublisher(URI.create("test:id/user/account.adminopensilexorg"));

        //creating the model
        sparql.create(model);

        //get the model created with a getByUri
        C modelFromGetMethod = sparql.getByURI(C.class, model.getUri(), null);

        //check if all metadata are established
        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());
        Assert.assertNull(modelFromGetMethod.getLastUpdateDate());
        Assert.assertEquals(model.getUri(), modelFromGetMethod.getUri());
        Assert.assertEquals(model.getPublisher(), modelFromGetMethod.getPublisher());
        Assert.assertEquals(model.getPublicationDate(), modelFromGetMethod.getPublicationDate());
    }


    @Test
    public void updateAfterCreate() throws Exception {
        //configuring the model with an uri
        C model = this.getModel();
        model.setUri(URI.create("test:SPARQLMetadataTest_c2"));
        model.setPublisher(URI.create("test:id/user/account.adminopensilexorg"));

        //creating the model
        sparql.create(model);

        //get the model created with a getByUri
        C modelFromGetMethod = sparql.getByURI(C.class, model.getUri(), null);

        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());

        //keeping old date before the update
        OffsetDateTime lastUpdatedDateBeforeUpdate = modelFromGetMethod.getLastUpdateDate();

        //updating the model
        sparql.update(modelFromGetMethod);

        //get the model updated with a getByUri
        C newModelFromGet = sparql.getByURI(C.class, modelFromGetMethod.getUri(), null);

        //check if all metadata are established
        Assert.assertNotNull(newModelFromGet);
        Assert.assertNotNull(newModelFromGet.getPublicationDate());
        Assert.assertNotNull(newModelFromGet.getLastUpdateDate());
        Assert.assertNotEquals(lastUpdatedDateBeforeUpdate, newModelFromGet.getLastUpdateDate());
        Assert.assertEquals(modelFromGetMethod.getUri(), newModelFromGet.getUri());
        Assert.assertEquals(modelFromGetMethod.getPublisher(), newModelFromGet.getPublisher());
        Assert.assertEquals(modelFromGetMethod.getPublicationDate(), newModelFromGet.getPublicationDate());

    }

    @Test
    public void updateAfterUpdate() throws Exception {
        //configuring the model with an uri
        C model = this.getModel();
        model.setUri(URI.create("test:SPARQLMetadataTest_c3"));
        model.setPublisher(URI.create("test:id/user/account.adminopensilexorg"));

        //creating the model
        sparql.create(model);

        //get the model created with a getByUri
        C modelFromGetMethod = sparql.getByURI(C.class, model.getUri(), null);

        Assert.assertNotNull(modelFromGetMethod);
        Assert.assertNotNull(modelFromGetMethod.getPublicationDate());

        //keeping old date before the update
        OffsetDateTime lastUpdatedDateBeforeUpdate = modelFromGetMethod.getLastUpdateDate();

        //updating the model
        sparql.update(modelFromGetMethod);

        //get the first model updated with a getByUri
        C firstUpdatedModel = sparql.getByURI(C.class, modelFromGetMethod.getUri(), null);

        Assert.assertNotNull(firstUpdatedModel);
        Assert.assertNotNull(firstUpdatedModel.getPublicationDate());
        Assert.assertNotNull(firstUpdatedModel.getLastUpdateDate());
        Assert.assertNotEquals(lastUpdatedDateBeforeUpdate, firstUpdatedModel.getLastUpdateDate());

        //keeping old date before the update
        OffsetDateTime lastUpdatedDateBeforeSecondUpdate = firstUpdatedModel.getLastUpdateDate();

        //updating again to check the new values
        sparql.update(firstUpdatedModel);

        //get the second model updated with a getByUri
        C secondUpdatedModel = sparql.getByURI(C.class, firstUpdatedModel.getUri(), null);

        //check if all metadata are established
        Assert.assertNotNull(secondUpdatedModel);
        Assert.assertNotNull(secondUpdatedModel.getPublicationDate());
        Assert.assertNotNull(secondUpdatedModel.getLastUpdateDate());
        Assert.assertNotEquals(lastUpdatedDateBeforeSecondUpdate, secondUpdatedModel.getLastUpdateDate());
        Assert.assertEquals(firstUpdatedModel.getUri(), secondUpdatedModel.getUri());
        Assert.assertEquals(firstUpdatedModel.getPublisher(), secondUpdatedModel.getPublisher());
        Assert.assertEquals(firstUpdatedModel.getPublicationDate(), secondUpdatedModel.getPublicationDate());

    }
}
