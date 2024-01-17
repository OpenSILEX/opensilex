package org.opensilex.core.germplasmGroup.dal;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.api.GermplasmGroupApiTest;
import org.opensilex.core.germplasmGroup.api.GermplasmGroupCreationDTO;
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
public class GermplasmGroupDAOTest extends AbstractMongoIntegrationTest {

    public static GermplasmDAO germplasmDAO;
    public static GermplasmGroupDAO germplasmGroupDAO;

    //Define a first group : pre1 , has two germplasm in it
    public static GermplasmGroupModel preCreatedGroup;
    public static GermplasmModel preSpecies1;
    public static GermplasmModel preSpecies2;

    //Define a second group : pre1 , has two germplasm in it
    public static GermplasmGroupModel preCreatedGroup2;
    public static GermplasmModel pre2Species1;
    public static GermplasmModel pre2Species2;

    @Before
    public void beforeStuff() throws Exception {
        //2 Species used to create and update groups
        SPARQLService sparqlService = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        MongoDBService mongoDBService = getOpensilex().getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        germplasmDAO = new GermplasmDAO(sparqlService, mongoDBService);
        GermplasmCreationDTO pre1spe1DTO = GermplasmGroupApiTest.getCreationSpeciesDTO("pre1spe1");
        GermplasmCreationDTO pre1spe2DTO = GermplasmGroupApiTest.getCreationSpeciesDTO("pre1spe2");
        GermplasmModel pre1spe1Model = pre1spe1DTO.newModel(null, null);
        preSpecies1 = germplasmDAO.create(pre1spe1Model);
        GermplasmModel pre1spe2Model = pre1spe2DTO.newModel(null, null);
        preSpecies2 = germplasmDAO.create(pre1spe2Model);
        GermplasmCreationDTO pre2spe1DTO = GermplasmGroupApiTest.getCreationSpeciesDTO("pre2spe1");
        GermplasmCreationDTO pre2spe2DTO = GermplasmGroupApiTest.getCreationSpeciesDTO("pre2spe2");
        GermplasmModel pre2spe1Model = pre2spe1DTO.newModel(null, null);
        pre2Species1 = germplasmDAO.create(pre2spe1Model);
        GermplasmModel pre2spe2Model = pre2spe2DTO.newModel(null, null);
        pre2Species2 = germplasmDAO.create(pre2spe2Model);

        //2 pre created groups to test stuff on
        GermplasmGroupCreationDTO preCreatedGroupDTO = GermplasmGroupApiTest.getCreationGroupDTO("pre");
        GermplasmGroupCreationDTO preCreatedGroupDTO2 = GermplasmGroupApiTest.getCreationGroupDTO("pre2");

        List<URI> germplasmUrisPre1 = new ArrayList<>();
        germplasmUrisPre1.add(preSpecies1.getUri());
        germplasmUrisPre1.add(preSpecies2.getUri());
        preCreatedGroupDTO.setGermplasmList(germplasmUrisPre1);
        germplasmGroupDAO = new GermplasmGroupDAO(sparqlService);
        preCreatedGroup = germplasmGroupDAO.create(preCreatedGroupDTO.newModel());

        List<URI> germplasmUrisPre2 = new ArrayList<>();
        germplasmUrisPre2.add(pre2Species1.getUri());
        germplasmUrisPre2.add(pre2Species2.getUri());
        preCreatedGroupDTO2.setGermplasmList(germplasmUrisPre2);
        germplasmGroupDAO = new GermplasmGroupDAO(sparqlService);
        preCreatedGroup2 = germplasmGroupDAO.create(preCreatedGroupDTO2.newModel());
    }

    @Test
    public void testCreate() {
        assertEquals(2, preCreatedGroup.getGermplasmList().size());
    }

    /**
     * Tests :
     * - Updates a group with a new germplasm list, checks if germplasm list has right size
     *
     */
    @Test
    public void testUpdate() throws Exception {
        List<GermplasmModel> newGermplasmList = new ArrayList<>();
        newGermplasmList.add(preSpecies1);
        preCreatedGroup.setGermplasmList(newGermplasmList);
        GermplasmGroupModel updated = germplasmGroupDAO.update(preCreatedGroup);
        assertEquals(1, updated.getGermplasmList().size());
    }

    /**
     * Tests :
     * - Search all, quantity of groups is equal to 2 (the amount we created before tests)
     * - Search by a name that doesn't exist, return a quantity of 0
     * - Search by "pre2", a substring that only exists in one test group , check that 1 group is returned and that it has correct uri
     * - search by Germplasm pre2Species2, found only in preCreatedGroup2, so quantity should be 1 and check that uri is correct
     *
     */
    @Test
    public void testSearch() throws Exception {
        ListWithPagination<GermplasmGroupModel> searchAllResult = germplasmGroupDAO.search(
                null, null, null, 0, 20, "en");
        assertEquals(2, searchAllResult.getTotal());
        ListWithPagination<GermplasmGroupModel> failedNameSearchResult = germplasmGroupDAO.search(
                "hooligans!",null, null, 0, 20, "en");
        assertEquals(0, failedNameSearchResult.getTotal());
        ListWithPagination<GermplasmGroupModel> successfulNameSearchResult = germplasmGroupDAO.search(
                "pre2", null, null, 0, 20, "en");
        assertEquals(1, successfulNameSearchResult.getTotal());
        assertEquals(preCreatedGroup2.getUri().toString(), SPARQLDeserializers.getExpandedURI(successfulNameSearchResult.getList().get(0).getUri()) );
        List<URI> germplasmsFilter = new ArrayList<>();
        germplasmsFilter.add(pre2Species2.getUri());
        ListWithPagination<GermplasmGroupModel> containsGermplasmSearchResult = germplasmGroupDAO.search(
                null, germplasmsFilter, null, 0, 20, "en");
        assertEquals(1, containsGermplasmSearchResult.getTotal());
        assertEquals(preCreatedGroup2.getUri().toString(), SPARQLDeserializers.getExpandedURI(containsGermplasmSearchResult.getList().get(0).getUri()) );
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return new ArrayList<Class<? extends SPARQLResourceModel>>() {{
            add(GermplasmGroupModel.class);
            add(GermplasmModel.class);
        }};
    }
}