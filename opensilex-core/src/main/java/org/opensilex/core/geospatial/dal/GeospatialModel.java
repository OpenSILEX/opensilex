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
import java.net.URI;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Geospatial Model
 *
 * @author Jean Philippe VERT
 */
public class GeospatialModel extends MongoModel {
//    public static final String TYPE_FIELD = "type";
//    public static final String NAME_VAR = "name";
    URI graph;
    URI type;
    Geometry geometry;

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
