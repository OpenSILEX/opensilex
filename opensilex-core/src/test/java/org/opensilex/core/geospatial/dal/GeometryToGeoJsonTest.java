/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.geospatial.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Position;
import java.util.ArrayList;
import java.util.List;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.junit.Test;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vmigot
 */
public class GeometryToGeoJsonTest extends AbstractUnitTest {

    @Test
    public void dtoToModel() throws JsonProcessingException {
        Feature feature = new Feature();
        Point point = new Point(1.3, 2.4);
        feature.setGeometry(point);
        Geometry geometry = GeospatialDAO.geoJsonToGeometry(feature);

        assertTrue(geometry instanceof com.mongodb.client.model.geojson.Point);
        com.mongodb.client.model.geojson.Point mongoPoint = (com.mongodb.client.model.geojson.Point) geometry;

        assertEquals(mongoPoint.getPosition().getValues().get(0), 1.3);
        assertEquals(mongoPoint.getPosition().getValues().get(1), 2.4);
    }

    @Test
    public void modelToDTO() throws JsonProcessingException {
        List<Double> coordinates = new ArrayList<>();
        coordinates.add(1.3);
        coordinates.add(2.4);
        Position pos = new Position(coordinates);
        com.mongodb.client.model.geojson.Point mongoPoint = new com.mongodb.client.model.geojson.Point(pos);

        GeoJsonObject geoJson = GeospatialDAO.geometryToGeoJson(mongoPoint);

        assertTrue(geoJson instanceof Feature);
        Feature geoJsonFeature = (Feature) geoJson;

        assertTrue(geoJsonFeature.getGeometry() instanceof Point);

        Point geoJsonPoint = (Point) geoJsonFeature.getGeometry();
        assertEquals(geoJsonPoint.getCoordinates().getLongitude(), 1.3);
        assertEquals(geoJsonPoint.getCoordinates().getLatitude(), 2.4);
    }

}
