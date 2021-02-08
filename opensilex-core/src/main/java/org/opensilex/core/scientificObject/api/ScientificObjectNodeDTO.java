/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import org.geojson.GeoJsonObject;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.net.URISyntaxException;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 *
 * @author vmigot
 */
public class ScientificObjectNodeDTO extends NamedResourceDTO<ScientificObjectModel> {
    private GeoJsonObject geometry;

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    public static ScientificObjectNodeDTO getDTOFromModel(ScientificObjectModel model) {
        ScientificObjectNodeDTO dto = new ScientificObjectNodeDTO();
        dto.fromModel(model);

        return dto;
    }


    public static ScientificObjectNodeDTO getDTOFromModel(ScientificObjectModel model, Geometry geometryByURI)  {
        ScientificObjectNodeDTO dto = getDTOFromModel(model);
        if (geometryByURI != null) {
            try {
                dto.setGeometry(geometryToGeoJson(geometryByURI));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return dto;
    }

    public static ScientificObjectNodeDTO getDTOFromModel(GeospatialModel geospatialModel) {
        ScientificObjectNodeDTO dto = new ScientificObjectNodeDTO();
        if (geospatialModel != null) {
            try {
                dto.setType(geospatialModel.getRdfType());
                dto.setUri(new URI(SPARQLDeserializers.getExpandedURI(geospatialModel.getUri())));
                dto.setName(geospatialModel.getName());
                dto.setGeometry(geometryToGeoJson(geospatialModel.getGeometry()));
            } catch (JsonProcessingException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return dto;
    }
}
