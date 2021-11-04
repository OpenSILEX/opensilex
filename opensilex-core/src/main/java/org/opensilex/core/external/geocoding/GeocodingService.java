package org.opensilex.core.external.geocoding;

import com.mongodb.client.model.geojson.Geometry;

public interface GeocodingService {
    Geometry getPointFromAddress(String address);
}
