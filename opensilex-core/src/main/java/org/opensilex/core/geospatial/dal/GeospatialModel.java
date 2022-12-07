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
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;

/**
 * Geospatial Model
 *
 * @author Jean Philippe VERT
 */
public class GeospatialModel extends MongoModel {

    public static final String GRAPH_FIELD = "graph";
    URI graph;
    URI rdfType;
    String name;

    Geometry geometry;

    public static final String GEOMETRY_FIELD = "geometry";

    public static final String RDF_TYPE_FIELD = "rdfType";

    public GeospatialModel() {
    }

    public GeospatialModel(SPARQLNamedResourceModel<?> model, URI graph, Geometry geometry) {
        this.uri = model.getUri();
        this.name = model.getName();
        this.rdfType = model.getType();
        this.graph = graph;
        this.geometry = geometry;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
