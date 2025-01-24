/*
 * *****************************************************************************
 *                         LocationsAPITest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/05/2024 11:38
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.geojson.Geometry;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.core.geospatial.dal.GeospatialDAO.geoJsonToGeometry;

public class LocationsAPITest extends FaidareAPITest {

    protected static final ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    LocationsAPI.class.getMethod(
                            "getLocationsList", URI.class, int.class, int.class),
                    "/faidare/v1/locations"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGet() throws Exception {
        BrapiPaginatedListResponse<JsonNode> deserializedSearchResult = new AbstractSecurityIntegrationTest.UserCallBuilder(search)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<JsonNode>>(){})
                .getDeserializedResponse();
        Assert.assertEquals(5, deserializedSearchResult.getResult().getData().size());
    }

    @Test
    public void testGetByUri() throws Exception {
        FacilityCreationDTO facilityCreationDTO = facilityBuilder.getDTOList().get(0);
        BrapiPaginatedListResponse<JsonNode> deserializedSearchResult = new UserCallBuilder(search)
                .addParam("locationDbId", facilityCreationDTO.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<JsonNode>>(){})
                .getDeserializedResponse();

        Assert.assertEquals(1, deserializedSearchResult.getResult().getData().size());

        // Check first level mapping
        Map<String, String> keysMatching = new HashMap<>(){{
            put("name", "locationName");
        }};
        JsonNode expected = mapper.convertValue(facilityCreationDTO, JsonNode.class);
        JsonNode actual = deserializedSearchResult.getResult().getData().get(0);
        assertTrue(valuesMatch(
                expected,
                actual,
                keysMatching
        ));

        // Check geometry
        Geometry facilityGeometry = geoJsonToGeometry(facilityCreationDTO.getLocations().get(0).getGeojson());
        org.locationtech.jts.geom.Geometry facilityJtsGeometry = new GeoJsonReader().read(facilityGeometry.toJson());
        Point centroid = facilityJtsGeometry.getCentroid();
        assertEquals(centroid.getX(), actual.get("longitude").asDouble());
        assertEquals(centroid.getY(), actual.get("latitude").asDouble());

        // Check type
        SPARQLDeserializers.compareURIs(expected.get("rdf_type").asText(), actual.get("locationType").asText());
    }
}
