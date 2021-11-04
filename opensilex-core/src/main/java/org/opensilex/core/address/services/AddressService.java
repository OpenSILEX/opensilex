package org.opensilex.core.address.services;

import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.opensilex.core.address.api.AddressDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

public class AddressService {
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private static final String SCHEME = "https";
    private static final String BASE_URL = "nominatim.openstreetmap.org";
    private static final String SEARCH_ENDPOINT = "search";
    private static final String QUERY_PARAMETER = "q";
    private static final String FORMAT_PARAMETER = "format";
    private static final String FORMAT = "geojson";

    public AddressService() {

    }

    public Geometry getPointFromAddress(AddressDTO address) {
        try {
            URI requestURI = buildSearchQuery(address);
            Content content = Request.Get(requestURI).execute().returnContent();
            String test = content.asString();
            JsonObject json = JSON.parse(test);
            JsonArray features = json.get("features").getAsArray();
            if (features.isEmpty()) {
                return null;
            }
            JsonArray coordsArray = features.get(0)
                    .getAsObject()
                    .getObj("geometry")
                    .get("coordinates")
                    .getAsArray();
            List<Double> coords = coordsArray.stream().map(v -> v.getAsNumber().value().doubleValue()).collect(Collectors.toList());
            return new Point(new Position(coords));
        } catch (Exception e) {
            return null;
        }
    }

    private URI buildSearchQuery(AddressDTO address) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme(SCHEME);
        builder.setHost(BASE_URL);
        builder.setPath(SEARCH_ENDPOINT);
        builder.addParameter(QUERY_PARAMETER, address.toReadableAddress());
        builder.addParameter(FORMAT_PARAMETER, FORMAT);
        return builder.build();
    }
}
