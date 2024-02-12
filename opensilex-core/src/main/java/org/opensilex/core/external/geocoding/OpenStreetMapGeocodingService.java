package org.opensilex.core.external.geocoding;

import com.fasterxml.jackson.core.JacksonException;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * Wrapper for the OpenStreetMap geocoding API (Nominatim). Usage of this API is allowed under the
 * <a href="https://operations.osmfoundation.org/policies/nominatim/">Nominatim Usage Policy</a>.
 *
 * @author Valentin RIGOLLE
 */
public class OpenStreetMapGeocodingService implements GeocodingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStreetMapGeocodingService.class);

    private final Client httpClient;

    private static final String SCHEME = "https";
    private static final String BASE_URL = "nominatim.openstreetmap.org";
    private static final String SEARCH_ENDPOINT = "search";
    private static final String QUERY_PARAMETER = "q";
    private static final String FORMAT_PARAMETER = "format";
    private static final String FORMAT = "geojson";

    public OpenStreetMapGeocodingService() {
        httpClient = ClientBuilder.newClient();
    }

    @Override
    public Geometry getPointFromAddress(String address) {
        try {
            URI requestURI = buildSearchQuery(address);
            String responseString = httpClient.target(requestURI)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);
            GeoJsonObject geoJson = ObjectMapperContextResolver.getObjectMapper().readValue(responseString, GeoJsonObject.class);

            Feature feature;
            if (geoJson instanceof Feature) {
                feature = (Feature) geoJson;
            } else if (geoJson instanceof FeatureCollection) {
                FeatureCollection collection = (FeatureCollection) geoJson;
                if (collection.getFeatures().isEmpty()) {
                    return null;
                }
                feature = collection.getFeatures().get(0);
            } else {
                return null;
            }

            org.geojson.Point point = (org.geojson.Point) feature.getGeometry();
            return new Point(new Position(
                    point.getCoordinates().getLongitude(),
                    point.getCoordinates().getLatitude()
            ));
        } catch (WebApplicationException e) {
            LOGGER.error("OpenStreetMap API returned an error", e);
            return null;
        } catch (ProcessingException e) {
            LOGGER.error("Could not reach OpenStreetMap API", e);
            return null;
        } catch (JacksonException e) {
            LOGGER.error("Response is not valid Json", e);
            return null;
        }
    }

    private URI buildSearchQuery(String address) {
        UriBuilder builder = UriBuilder.fromUri("")
            .scheme(SCHEME)
            .host(BASE_URL)
            .path(SEARCH_ENDPOINT)
            .queryParam(QUERY_PARAMETER, address)
            .queryParam(FORMAT_PARAMETER, FORMAT);
        return builder.build();
    }
}
