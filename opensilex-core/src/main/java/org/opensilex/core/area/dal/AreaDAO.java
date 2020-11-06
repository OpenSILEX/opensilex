/*
 * *******************************************************************************
 *                     AreaDAO.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 14, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */
package org.opensilex.core.area.dal;

import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Area DAO
 *
 * @author Jean Philippe VERT
 */
public class AreaDAO {

    protected final SPARQLService sparql;

    public AreaDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public URI create(URI uri, String name, URI rdfType, String description, URI user) throws Exception {
        AreaModel area = initArea(uri, name, rdfType, description, user);

        sparql.create(area);

        return area.getUri();
    }

    private AreaModel initArea(URI uri, String name, URI type, String description, URI user) throws URISyntaxException {
        AreaModel area = new AreaModel();
        area.setName(name);
        area.setType(type);
        area.setDescription(description);
        area.setCreator(user);

        if (uri != null) {
            uri = new URI(SPARQLDeserializers.getExpandedURI(uri.toString()));
            area.setUri(uri);
        }

        return area;
    }

    public AreaModel getByURI(URI instanceURI) throws Exception {
        return sparql.getByURI(AreaModel.class, instanceURI, null);
    }

    public void delete(URI areaURI) throws Exception {
        sparql.delete(AreaModel.class, areaURI);
    }

    public URI update(URI uri, String name, URI type, String description, URI user) throws Exception {
        AreaModel area = initArea(uri, name, type, description, user);

        sparql.update(area);

        return area.getUri();
    }
}