package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

public class BaseGermplasmAPITest extends AbstractMongoIntegrationTest {

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


    protected URI createSpecies() throws Exception {
        // create species
        final Response postResultSpecies = getJsonPostResponseAsAdmin(target(createPath), getCreationSpeciesDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResultSpecies.getStatus());
        return extractUriFromResponse(postResultSpecies);
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(GermplasmModel.class);
    }

    protected void testSearchParams(Map<String, Object> params) throws Exception {
        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 20, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        PaginatedListResponse<GermplasmGetAllDTO> germplasmListResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        List<GermplasmGetAllDTO> germplasmList = germplasmListResponse.getResult();

        assertFalse(germplasmList.isEmpty());
    }
}
