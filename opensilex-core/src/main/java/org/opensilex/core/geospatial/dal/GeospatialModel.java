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
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;

/**
 * Geospatial Model
 *
 * @author Jean Philippe VERT
 */
public class GeospatialModel extends MongoModel {
    URI graph;
    URI rdfType;
    Geometry geometry;

    public URI getGraph() {
        return graph;
    }

    public void setGraph(URI graph) {
        this.graph = graph;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
