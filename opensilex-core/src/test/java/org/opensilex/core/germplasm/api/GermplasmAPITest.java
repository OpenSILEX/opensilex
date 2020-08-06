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
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensilex.OpenSilex;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.integration.test.IntegrationTestCategory;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author Alice BOIZET
 */
@Category(IntegrationTestCategory.class)
public class GermplasmAPITest extends AbstractSecurityIntegrationTest {

    protected String path = "/core/germplasm";

    protected String uriPath = path + "/get/{uri}";
    protected String searchPath = path + "/search";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String deletePath = path + "/delete/{uri}";

    protected GermplasmCreationDTO getCreationSpeciesDTO() throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setLabel("testSpecies");
        germplasmDTO.setRdfType(new URI(Oeso.Species.toString()));
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationVarietyDTO(URI speciesURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setLabel("testVariety");
        germplasmDTO.setRdfType(new URI(Oeso.Variety.toString()));
        germplasmDTO.setFromSpecies(speciesURI);
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationAccessionDTO(URI varietyURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setLabel("testAccession");
        germplasmDTO.setRdfType(new URI(Oeso.Accession.toString()));
        germplasmDTO.setFromVariety(varietyURI);
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationLotDTO(URI accessionURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setLabel("testLot");
        germplasmDTO.setRdfType(new URI(Oeso.PlantMaterialLot.toString()));
        germplasmDTO.setFromAccession(accessionURI);
        return germplasmDTO;
    }

    @Test
    public void testCreate() throws Exception {

        // create species
        final Response postResultSpecies = getJsonPostResponse(target(createPath), getCreationSpeciesDTO());
        LOGGER.info(postResultSpecies.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultSpecies.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdSpeciesUri = extractUriFromResponse(postResultSpecies);
        final Response getResultSpecies = getJsonGetByUriResponse(target(uriPath), createdSpeciesUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultSpecies.getStatus());

        // create Variety
        final Response postResultVariety = getJsonPostResponse(target(createPath), getCreationVarietyDTO(createdSpeciesUri));
        LOGGER.info(postResultVariety.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultVariety.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdVarietyUri = extractUriFromResponse(postResultVariety);
        final Response getResultVariety = getJsonGetByUriResponse(target(uriPath), createdVarietyUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultVariety.getStatus());

        // create Accession
        final Response postResultAccession = getJsonPostResponse(target(createPath), getCreationAccessionDTO(createdVarietyUri));
        LOGGER.info(postResultAccession.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultAccession.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdAccessionUri = extractUriFromResponse(postResultAccession);
        final Response getResultAccession = getJsonGetByUriResponse(target(uriPath), createdAccessionUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultAccession.getStatus());

        // create Lot
        final Response postResultLot = getJsonPostResponse(target(createPath), getCreationLotDTO(createdAccessionUri));
        LOGGER.info(postResultLot.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultLot.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdLotUri = extractUriFromResponse(postResultLot);
        final Response getResultLot = getJsonGetByUriResponse(target(uriPath), createdLotUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultLot.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationSpeciesDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
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
        LOGGER.info(uri.toString());

        GermplasmSearchDTO searchDTO = new GermplasmSearchDTO();
        searchDTO.label = getCreationSpeciesDTO().label;
        searchDTO.rdfType = getCreationSpeciesDTO().rdfType;

        Response getResult = getJsonPostResponse(target(searchPath), searchDTO, OpenSilex.DEFAULT_LANGUAGE);

        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<GermplasmGetAllDTO>>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();

        assertFalse(germplasmList.isEmpty());
    }

    @Test
    public void testUpdate() throws Exception {

        // create the germplasm
        GermplasmCreationDTO germplasm = getCreationSpeciesDTO();
        final Response postResult = getJsonPostResponse(target(createPath), germplasm);

        // update the germplasm
        germplasm.setUri(extractUriFromResponse(postResult));
        germplasm.setLabel("new alias");

        final Response updateResult = getJsonPutResponse(target(updatePath), germplasm);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new germplasm and compare to the expected germplasm
        final Response getResult = getJsonGetByUriResponse(target(uriPath), germplasm.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        SingleObjectResponse<GermplasmGetSingleDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {
        });
        GermplasmGetSingleDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(germplasm.getLabel(), dtoFromApi.getLabel());
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
        LOGGER.info("status no longer exists " + getResult.getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());

        // create the species that can't be deleted because it is linked to a variety
        GermplasmCreationDTO speciesNotToDelete = getCreationSpeciesDTO();
        Response postResponse2 = getJsonPostResponse(target(createPath), speciesNotToDelete);
        URI uriNotToDelete = extractUriFromResponse(postResponse2);
        LOGGER.info("uri not to delete " + uriNotToDelete);

        // create Variety linked to this species
        Response postResultVariety = getJsonPostResponse(target(createPath), getCreationVarietyDTO(uriNotToDelete));
        LOGGER.info("variety creation " + postResultVariety.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultVariety.getStatus());

        // try to delete the species that can't be deleted and get a bad request status
        Response delResult2 = getDeleteByUriResponse(target(deletePath), uriNotToDelete.toString());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), delResult2.getStatus());

        // check uri still exists
        Response getResult3 = getJsonGetByUriResponse(target(uriPath), uriNotToDelete.toString());
        assertEquals(Status.OK.getStatusCode(), getResult3.getStatus());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(GermplasmModel.class);
    }

}
