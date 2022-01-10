package org.opensilex.core.external.geocoding;

import com.mongodb.client.model.geojson.Geometry;

/**
 * External service to perform geocoding, i.e. get geometry information (lat/long point) from an address string.
 *
 * @author Valentin RIGOLLE
 */
public interface GeocodingService {
    /**
     * Retrieves geometry information from an address string.
     *
     * @param address The address string
     * @return The geometry associated with the address (i.e. a lat/long point)
     */
    Geometry getPointFromAddress(String address);
}
