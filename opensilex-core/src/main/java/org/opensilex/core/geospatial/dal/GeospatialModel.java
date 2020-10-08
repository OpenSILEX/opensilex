/*
 * *******************************************************************************
 *                     GeospatialModel.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 28, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */

package org.opensilex.core.geospatial.dal;

import com.mongodb.client.model.geojson.Geometry;
import org.opensilex.core.germplasm.dal.URIStringConverter;

import javax.jdo.annotations.Convert;
import javax.persistence.Id;
import java.net.URI;

/**
 * Geospatial Model
 *
 * @author Jean Philippe VERT
 */

public class GeospatialModel {
//    public static final String TYPE_FIELD = "type";
//    public static final String NAME_VAR = "name";
    @Convert(URIStringConverter.class)
    @Id
    URI uri;
    URI graph;
    URI type;
    Geometry geometry;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getGraph() {
        return graph;
    }

    public void setGraph(URI graph) {
        this.graph = graph;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}