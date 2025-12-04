package org.opensilex.core.geneticResourceGroup.dal;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.geneticResource.api.GeneticResourceCreationDTO;
import org.opensilex.core.geneticResource.dal.GeneticResourceDAO;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.geneticResourceGroup.api.GeneticResourceGroupApiTest;
import org.opensilex.core.geneticResourceGroup.api.GeneticResourceGroupCreationDTO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests to verify contents of updates, create and searches
 */
public class GeneticResourceGroupDAOTest extends AbstractMongoIntegrationTest {

    public static GeneticResourceDAO geneticResourceDAO;
    public static GeneticResourceGroupDAO geneticResourceGroupDAO;

    //Define a first group : pre1 , has two geneticResource in it
    public static GeneticResourceGroupModel preCreatedGroup;
    public static GeneticResourceModel preSpecies1;
    public static GeneticResourceModel preSpecies2;

    //Define a second group : pre1 , has two geneticResource in it
    public static GeneticResourceGroupModel preCreatedGroup2;
    public static GeneticResourceModel pre2Species1;
    public static GeneticResourceModel pre2Species2;

    @Before
    public void beforeStuff() throws Exception {
        //2 Species used to create and update groups
        SPARQLService sparqlService = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        MongoDBService mongoDBService = getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        geneticResourceDAO = new GeneticResourceDAO(sparqlService, mongoDBService);
        GeneticResourceCreationDTO pre1spe1DTO = GeneticResourceGroupApiTest.getCreationSpeciesDTO("pre1spe1");
        GeneticResourceCreationDTO pre1spe2DTO = GeneticResourceGroupApiTest.getCreationSpeciesDTO("pre1spe2");
        GeneticResourceModel pre1spe1Model = pre1spe1DTO.newModel(null, null, null);
        preSpecies1 = geneticResourceDAO.create(pre1spe1Model);
        GeneticResourceModel pre1spe2Model = pre1spe2DTO.newModel(null, null, null);
        preSpecies2 = geneticResourceDAO.create(pre1spe2Model);
        GeneticResourceCreationDTO pre2spe1DTO = GeneticResourceGroupApiTest.getCreationSpeciesDTO("pre2spe1");
        GeneticResourceCreationDTO pre2spe2DTO = GeneticResourceGroupApiTest.getCreationSpeciesDTO("pre2spe2");
        GeneticResourceModel pre2spe1Model = pre2spe1DTO.newModel(null, null, null);
        pre2Species1 = geneticResourceDAO.create(pre2spe1Model);
        GeneticResourceModel pre2spe2Model = pre2spe2DTO.newModel(null, null, null);
        pre2Species2 = geneticResourceDAO.create(pre2spe2Model);

        //2 pre created groups to test stuff on
        GeneticResourceGroupCreationDTO preCreatedGroupDTO = GeneticResourceGroupApiTest.getCreationGroupDTO("pre");
        GeneticResourceGroupCreationDTO preCreatedGroupDTO2 = GeneticResourceGroupApiTest.getCreationGroupDTO("pre2");

        List<URI> geneticResourceUrisPre1 = new ArrayList<>();
        geneticResourceUrisPre1.add(preSpecies1.getUri());
        geneticResourceUrisPre1.add(preSpecies2.getUri());
        preCreatedGroupDTO.setGeneticResourceList(geneticResourceUrisPre1);
        geneticResourceGroupDAO = new GeneticResourceGroupDAO(sparqlService);
        preCreatedGroup = geneticResourceGroupDAO.create(preCreatedGroupDTO.newModel());

        List<URI> geneticResourceUrisPre2 = new ArrayList<>();
        geneticResourceUrisPre2.add(pre2Species1.getUri());
        geneticResourceUrisPre2.add(pre2Species2.getUri());
        preCreatedGroupDTO2.setGeneticResourceList(geneticResourceUrisPre2);
        geneticResourceGroupDAO = new GeneticResourceGroupDAO(sparqlService);
        preCreatedGroup2 = geneticResourceGroupDAO.create(preCreatedGroupDTO2.newModel());
    }

    @Test
    public void testCreate() {
        assertEquals(2, preCreatedGroup.getGeneticResourceList().size());
    }

    /**
     * Tests :
     * - Updates a group with a new geneticResource list, checks if geneticResource list has right size
     *
     */
    @Test
    public void testUpdate() throws Exception {
        List<GeneticResourceModel> newGeneticResourceList = new ArrayList<>();
        newGeneticResourceList.add(preSpecies1);
        preCreatedGroup.setGeneticResourceList(newGeneticResourceList);
        GeneticResourceGroupModel updated = geneticResourceGroupDAO.update(preCreatedGroup);
        assertEquals(1, updated.getGeneticResourceList().size());
    }

    /**
     * Tests :
     * - Search all, quantity of groups is equal to 2 (the amount we created before tests)
     * - Search by a name that doesn't exist, return a quantity of 0
     * - Search by "pre2", a substring that only exists in one test group , check that 1 group is returned and that it has correct uri
     * - search by GeneticResource pre2Species2, found only in preCreatedGroup2, so quantity should be 1 and check that uri is correct
     *
     */
    @Test
    public void testSearch() throws Exception {
        ListWithPagination<GeneticResourceGroupModel> searchAllResult = geneticResourceGroupDAO.search(
                null, null, null, 0, 20, "en");
        assertEquals(2, searchAllResult.getTotal());
        ListWithPagination<GeneticResourceGroupModel> failedNameSearchResult = geneticResourceGroupDAO.search(
                "hooligans!",null, null, 0, 20, "en");
        assertEquals(0, failedNameSearchResult.getTotal());
        ListWithPagination<GeneticResourceGroupModel> successfulNameSearchResult = geneticResourceGroupDAO.search(
                "pre2", null, null, 0, 20, "en");
        assertEquals(1, successfulNameSearchResult.getTotal());
        assertEquals(preCreatedGroup2.getUri().toString(), SPARQLDeserializers.getExpandedURI(successfulNameSearchResult.getList().get(0).getUri()) );
        List<URI> geneticResourcesFilter = new ArrayList<>();
        geneticResourcesFilter.add(pre2Species2.getUri());
        ListWithPagination<GeneticResourceGroupModel> containsGeneticResourceSearchResult = geneticResourceGroupDAO.search(
                null, geneticResourcesFilter, null, 0, 20, "en");
        assertEquals(1, containsGeneticResourceSearchResult.getTotal());
        assertEquals(preCreatedGroup2.getUri().toString(), SPARQLDeserializers.getExpandedURI(containsGeneticResourceSearchResult.getList().get(0).getUri()) );
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return new ArrayList<Class<? extends SPARQLResourceModel>>() {{
            add(GeneticResourceGroupModel.class);
            add(GeneticResourceModel.class);
        }};
    }
}