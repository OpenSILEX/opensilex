/*
 * *******************************************************************************
 *                     AreaModelRDF4J.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 14, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */
package org.opensilex.core.area.dal;

import org.apache.jena.vocabulary.RDFS;
import org.geojson.GeoJsonObject;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Area Model
 *
 * @author Jean Philippe VERT
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Area",
        graph = "set/area",
        prefix = "area"
)
public class AreaModel extends SPARQLResourceModel implements ClassURIGenerator<AreaModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String name;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String description;

    GeoJsonObject geometry;

    public AreaModel() {
    }

    public AreaModel(URI uri, String name, URI type, String description, URI author) throws URISyntaxException {
        this.setName(name);
        this.setType(type);
        this.setDescription(description);
        setCreator(author);

        if (uri != null) {
            uri = new URI(SPARQLDeserializers.getExpandedURI(uri.toString()));
            this.setUri(uri);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    @Override
    public String[] getUriSegments(@NotNull AreaModel instance) {
        return new String[]{
                String.valueOf(instance.getName())
        };
    }
}