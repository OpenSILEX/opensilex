package org.opensilex.core.species.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;


/**
 * @author Brice MAUSSANG
 */
public class SpeciesAPITest extends AbstractMongoIntegrationTest {

    protected final static String PATH = "/core/species";

    private static SpeciesModel s1, s2, s3;

    protected static final TypeReference<PaginatedListResponse<SpeciesDTO>> listTypeReference = new TypeReference<PaginatedListResponse<SpeciesDTO>>() {};


    public SpeciesModel getSpeciesModel(int i) {
        SpeciesModel species = new SpeciesModel();
        species.setUri(URI.create("test:species" + i));
        species.setLabel(new SPARQLLabel("species" + i, "en"));

        return species;
    }

    public void createSpecies() throws Exception {
        s1 = getSpeciesModel(1);
        s2 = getSpeciesModel(2);
        s3 = getSpeciesModel(3);

        getSparqlService().create(s1);
        getSparqlService().create(s2);
        getSparqlService().create(s3);
    }

    @Test
    public void testGetAllSpecies() throws Exception {

        createSpecies();

        Response response = getJsonGetResponseAsAdmin(target(PATH));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        Map<String,Object> searchParams = new HashMap<>();
        searchParams.put("lang","en");
        List<SpeciesDTO> results = getSearchResultsAsAdmin(PATH, searchParams, listTypeReference);

        assertEquals(3, results.size());
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), s1.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), s2.getUri())));
        Assert.assertTrue(results.stream().anyMatch(dto -> SPARQLDeserializers.compareURIs(dto.getUri(), s3.getUri())));
    }
}