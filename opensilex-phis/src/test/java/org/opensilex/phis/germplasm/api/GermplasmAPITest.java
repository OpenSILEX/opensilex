//******************************************************************************
//                          GermplasmAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.phis.germplasm.api;

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
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.germplasm.api.GermplasmGetAllDTO;
import org.opensilex.core.germplasm.api.GermplasmGetSingleDTO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author Alice BOIZET
 */
public class GermplasmAPITest extends AbstractMongoIntegrationTest {

    protected String path = "/core/germplasm";

    protected String uriPath = path + "/{uri}";
    protected String searchPath = path;
    protected String createPath = path;
    protected String updatePath = path;
    protected String deletePath = path + "/{uri}";

    protected GermplasmCreationDTO getCreationSpeciesDTO() throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testSpecies");
        germplasmDTO.setRdfType(new URI(Oeso.Species.toString()));
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationVarietyDTO(URI speciesURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testVariety");
        germplasmDTO.setRdfType(new URI(Oeso.Variety.toString()));
        germplasmDTO.setSpecies(speciesURI);
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationAccessionDTO(URI varietyURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testAccession");
        germplasmDTO.setRdfType(new URI(Oeso.Accession.toString()));
        germplasmDTO.setVariety(varietyURI);
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationLotDTO(URI accessionURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testLot");
        germplasmDTO.setRdfType(new URI(Oeso.PlantMaterialLot.toString()));
        germplasmDTO.setAccession(accessionURI);
        return germplasmDTO;
    }

    
    protected URI createSpecies() throws URISyntaxException, Exception {
        // create species
        final Response postResultSpecies = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultSpecies.getStatus());
        URI createdSpeciesUri = extractUriFromResponse(postResultSpecies);
        return createdSpeciesUri;
    }
    
    @Test
    public void testCreate() throws Exception {        
        
        // create Variety
        URI species = createSpecies();
        final Response postResultVariety = getJsonPostResponseAsAdmin(target(createPath), getCreationVarietyDTO(species));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultVariety.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdVarietyUri = extractUriFromResponse(postResultVariety);
        final Response getResultVariety = getJsonGetByUriResponseAsAdmin(target(uriPath), createdVarietyUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultVariety.getStatus());

        // create Accession
        final Response postResultAccession = getJsonPostResponseAsAdmin(target(createPath), getCreationAccessionDTO(createdVarietyUri));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultAccession.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdAccessionUri = extractUriFromResponse(postResultAccession);
        final Response getResultAccession = getJsonGetByUriResponseAsAdmin(target(uriPath), createdAccessionUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultAccession.getStatus());

        // create Lot
        final Response postResultLot = getJsonPostResponseAsAdmin(target(createPath), getCreationLotDTO(createdAccessionUri));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultLot.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdLotUri = extractUriFromResponse(postResultLot);
        final Response getResultLot = getJsonGetByUriResponseAsAdmin(target(uriPath), createdLotUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultLot.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        URI species = createSpecies();
        final Response postResultVariety = getJsonPostResponseAsAdmin(target(createPath), getCreationVarietyDTO(species));
        URI uri = extractUriFromResponse(postResultVariety);

        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), uri.toString());
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
        URI species = createSpecies();
        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), getCreationVarietyDTO(species));
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("name", getCreationSpeciesDTO().getName());
                put("rdf_type", getCreationSpeciesDTO().getType());
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();

        assertFalse(germplasmList.isEmpty());
    }

    //@Test
    public void testUpdate() throws Exception {
        
        URI species = createSpecies();

        // create a Variety
        GermplasmCreationDTO variety = getCreationVarietyDTO(species);
        final Response postResultVariety = getJsonPostResponseAsAdmin(target(createPath), variety);
        
        // update the variety
        variety.setUri(extractUriFromResponse(postResultVariety));
        variety.setName("new alias");
        final Response updateResult = getJsonPutResponse(target(updatePath), variety);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new germplasm and compare to the expected germplasm
        final Response getResult = getJsonGetByUriResponseAsAdmin(target(uriPath), variety.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        SingleObjectResponse<GermplasmGetSingleDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {
        });
        GermplasmGetSingleDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(variety.getName(), dtoFromApi.getName());
    }

    @Test
    public void testDelete() throws Exception {

        // create the species that can't be deleted because it is linked to a variety
        GermplasmCreationDTO speciesNotToDelete = getCreationSpeciesDTO();
        Response postResponse2 = getJsonPostResponseAsAdmin(target(createPath), speciesNotToDelete);
        URI uriNotToDelete = extractUriFromResponse(postResponse2);

        // create Variety linked to this species
        Response postResultVariety = getJsonPostResponseAsAdmin(target(createPath), getCreationVarietyDTO(uriNotToDelete));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultVariety.getStatus());

        // try to delete the species that can't be deleted and get a bad request status
        Response delResult2 = getDeleteByUriResponse(target(deletePath), uriNotToDelete.toString());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), delResult2.getStatus());

        // check uri still exists
        Response getResult3 = getJsonGetByUriResponseAsAdmin(target(uriPath), uriNotToDelete.toString());
        assertEquals(Status.OK.getStatusCode(), getResult3.getStatus());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(GermplasmModel.class);
    }

}
