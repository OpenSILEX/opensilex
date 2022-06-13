//******************************************************************************
//                          GermplasmAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;

import org.junit.Assert;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.metadata.MetaDataModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Alice BOIZET
 */
public class GermplasmAPITest extends AbstractMongoIntegrationTest {

    public static final String path = "/core/germplasm";

    public static final String uriPath = path + "/{uri}";
    public static final String searchPath = path;
    public static final String createPath = path;
    public static final String updatePath = path;
    public static final String deletePath = path + "/{uri}";

    public static GermplasmCreationDTO getCreationSpeciesDTO() throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testSpecies");
        germplasmDTO.setRdfType(new URI(Oeso.Species.toString()));
        return germplasmDTO;
    }

    @Test
    public void testCreate() throws Exception {

        // create species
        final Response postResultSpecies = getJsonPostResponse(target(createPath), getCreationSpeciesDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultSpecies.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdSpeciesUri = extractUriFromResponse(postResultSpecies);
        final Response getResultSpecies = getJsonGetByUriResponse(target(uriPath), createdSpeciesUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultSpecies.getStatus());
        
    }

    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationSpeciesDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        SingleObjectResponse<GermplasmGetSingleDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {
        });
        GermplasmGetSingleDTO germplasmGetDto = getResponse.getResult();
        assertNotNull(germplasmGetDto);
    }

    @Test
    public void testSearch() throws Exception {
        GermplasmCreationDTO creationDTO = getCreationSpeciesDTO();
        final Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", getCreationSpeciesDTO().name);
                put("rdf_type", getCreationSpeciesDTO().rdfType);                
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        final Response getResult = appendToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();

        assertFalse(germplasmList.isEmpty());
    }

    /**
     * Run this test at Dao level since the search method don't return attributes.
     * For now the {@link GermplasmDAO#search(GermplasmSearchFilter, boolean)} with fetchMetada = true
     * is only used into {@link GermplasmAPI#exportGermplasm(GermplasmSearchFilter)}.
     *
     */
    @Test
    public void testSearchWithMetadata() throws Exception {

        GermplasmDAO dao = new GermplasmDAO(getSparqlService(),getMongoDBService());

        // create two germplasm models with attributes
        GermplasmModel model1 = new GermplasmModel();
        model1.setLabel(new SPARQLLabel("germplasm1", ""));
        model1.setType(URI.create(Oeso.Species.toString()));

        Map<String,String> attributes = new HashMap<>();
        attributes.put("p1","v1");
        attributes.put("p2","v2");
        model1.setMetadata(new MetaDataModel(attributes));

        dao.create(model1);

        GermplasmModel model2 = new GermplasmModel();
        model2.setLabel(new SPARQLLabel("germplasm2", ""));
        model2.setType(URI.create(Oeso.Species.toString()));

        Map<String,String> attributes2 = new HashMap<>();
        attributes2.put("p3","v3");
        attributes2.put("p4","v4");
        model2.setMetadata(new MetaDataModel(attributes2));

        dao.create(model2);

        // search models and ensure that metadata are OK for each model
        ListWithPagination<GermplasmModel> models = dao.search(new GermplasmSearchFilter(),true);

        GermplasmModel model1FromDb = models.getList().stream()
                .filter(modelFromDb -> SPARQLDeserializers.compareURIs(modelFromDb.getUri(),model1.getUri()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("testSearchWithMetadata"));

        Assert.assertEquals(model1FromDb.getMetadata().getAttributes(),attributes);

        GermplasmModel model2FromDb = models.getList().stream()
                .filter(modelFromDb -> SPARQLDeserializers.compareURIs(modelFromDb.getUri(),model2.getUri()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("testSearchWithMetadata"));

        Assert.assertEquals(model2FromDb.getMetadata().getAttributes(),attributes2);
    }

    @Test
    public void testUpdate() throws Exception {

        // create a species
        GermplasmCreationDTO species = getCreationSpeciesDTO();
        final Response postResult = getJsonPostResponse(target(createPath), species);

        // update the germplasm
        species.setUri(extractUriFromResponse(postResult));
        species.setName("new alias");
        
        //check that you can update species
        final Response updateSpecies = getJsonPutResponse(target(updatePath), species);
        assertEquals(Status.OK.getStatusCode(), updateSpecies.getStatus());        

    }

    @Test
    public void testDelete() throws Exception {

        // create the species that can be deleted and check if URI exists
        GermplasmCreationDTO speciesToDelete = getCreationSpeciesDTO();
        Response postResponse1 = getJsonPostResponse(target(createPath), speciesToDelete);
        URI uriToDelete = extractUriFromResponse(postResponse1);
        // delete the species that can be deleted 
        Response delResult = getDeleteByUriResponse(target(deletePath), uriToDelete.toString());
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        // check if URI no longer exists
        Response getResult = getJsonGetByUriResponse(target(uriPath), uriToDelete.toString());
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());

    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(GermplasmModel.class);
    }

}
