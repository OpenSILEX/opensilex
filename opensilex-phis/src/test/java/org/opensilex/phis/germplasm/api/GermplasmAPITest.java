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
import org.junit.Test;
import org.opensilex.core.germplasm.api.BaseGermplasmAPITest;
import org.opensilex.core.germplasm.api.GermplasmCreationDTO;
import org.opensilex.core.germplasm.api.GermplasmGetAllDTO;
import org.opensilex.core.germplasm.api.GermplasmGetSingleDTO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author Alice BOIZET
 */
public class GermplasmAPITest extends BaseGermplasmAPITest {
    @Test
    public void testCreate() throws Exception {        
        
        // create Variety
        URI species = createSpecies();
        final Response postResultVariety = getJsonPostResponseAsAdmin(target(createPath), getCreationVarietyDTO(species));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultVariety.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        URI createdVarietyUri = extractUriFromResponse(postResultVariety);
        final Response getResultVariety = getJsonGetByUriResponseAsAdmin(target(uriPath), createdVarietyUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultVariety.getStatus());

        // create Accession
        final Response postResultAccession = getJsonPostResponseAsAdmin(target(createPath), getCreationAccessionDTO(createdVarietyUri));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultAccession.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        URI createdAccessionUri = extractUriFromResponse(postResultAccession);
        final Response getResultAccession = getJsonGetByUriResponseAsAdmin(target(uriPath), createdAccessionUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultAccession.getStatus());

        // create Lot
        final Response postResultLot = getJsonPostResponseAsAdmin(target(createPath), getCreationLotDTO(createdAccessionUri));
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultLot.getStatus());

        // ensure that the result is a well-formed URI, else throw exception
        URI createdLotUri = extractUriFromResponse(postResultLot);
        final Response getResultLot = getJsonGetByUriResponseAsAdmin(target(uriPath), createdLotUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResultLot.getStatus());
    }

    @Test
    public void CreatedVarietyShouldHaveASpecies() throws Exception {
        GermplasmCreationDTO germplasmDTO = getCreationSpeciesDTO();
        germplasmDTO.setRdfType(URI.create(Oeso.Variety.getURI()));
        germplasmDTO.setSpecies(null);

        new UserCallBuilder(create)
                .setBody(germplasmDTO)
                .buildAdmin()
                .executeCallAndAssertStatus(Status.BAD_REQUEST);
    }

    @Test
    public void SpeciesShouldBeAutomaticallyRetrievedWhenVarietyIsGiven() throws Exception {
        GermplasmCreationDTO baseSpecies = getCreationSpeciesDTO();
        final URI baseSpeciesURI = new UserCallBuilder(create)
                .setBody(baseSpecies)
                .buildAdmin()
                .executeCallAndReturnURI();

        GermplasmCreationDTO variety = getCreationVarietyDTO(baseSpeciesURI);
        final URI varietyURI = new UserCallBuilder(create)
                .setBody(variety)
                .buildAdmin()
                .executeCallAndReturnURI();

        GermplasmCreationDTO germplasmWithVariety = getCreationSpeciesDTO();
        germplasmWithVariety.setVariety(varietyURI);
        final URI createdURI = new UserCallBuilder(create)
                .setBody(germplasmWithVariety)
                .buildAdmin()
                .executeCallAndReturnURI();

        final GermplasmGetSingleDTO germplasmDTO = new UserCallBuilder(get)
                .addPathTemplateParam("uri", createdURI)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {
        })
                .getDeserializedResponse()
                .getResult();

        assertEquals("When creating the 'germplasmWithVariety' germplasm, the API should have automatically added the baseSpecies linked to the given variety",
                SPARQLDeserializers.getShortURI(baseSpeciesURI),
                SPARQLDeserializers.getShortURI(germplasmDTO.getSpecies()) );
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
        SingleObjectResponse<GermplasmGetSingleDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        GermplasmGetSingleDTO germplasmGetDto = getResponse.getResult();
        assertNotNull(germplasmGetDto);
    }

    @Test
    public void testSearch() throws Exception {
        URI species = createSpecies();
        getJsonPostResponseAsAdmin(target(createPath), getCreationVarietyDTO(species));

        Map<String, Object> params = new HashMap<>() {
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
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();

        assertFalse(germplasmList.isEmpty());
    }

    //@Test
    @Test
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
        SingleObjectResponse<GermplasmGetSingleDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        GermplasmGetSingleDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(variety.getName(), dtoFromApi.getName());
    }

    @Test
    public void UpdatedGermplasmShouldHaveASpecies() throws Exception {
        GermplasmCreationDTO speciesDTO = getCreationSpeciesDTO();
        final URI speciesURI = new UserCallBuilder(create)
                .setBody(speciesDTO)
                .buildAdmin()
                .executeCallAndReturnURI();

        GermplasmCreationDTO germplasmToUpdateDTO = getCreationSpeciesDTO();
        germplasmToUpdateDTO.setRdfType(URI.create(Oeso.Accession.toString()));
        germplasmToUpdateDTO.setSpecies(speciesURI);
        final URI createdURI = new UserCallBuilder(create)
                .setBody(germplasmToUpdateDTO)
                .buildAdmin()
                .executeCallAndReturnURI();

        germplasmToUpdateDTO.setUri(createdURI);
        germplasmToUpdateDTO.setSpecies(null);
        new UserCallBuilder(update)
                .setBody(germplasmToUpdateDTO)
                .buildAdmin()
                .executeCallAndAssertStatus(Status.BAD_REQUEST);
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
}
