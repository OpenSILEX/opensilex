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

    public URI create(String name, URI rdfType, String description) throws Exception {
        AreaModel area = new AreaModel();
        area.setName(name);
        area.setType(rdfType);
        area.setDescription(description);

        if (area.getUri() != null) {
            URI uri = new URI(SPARQLDeserializers.getExpandedURI(area.getUri().toString()));
            area.setUri(uri);
        }

        sparql.create(area);

        return area.getUri();
    }

    public AreaModel getByURI(URI instanceURI) throws Exception {
        return sparql.getByURI(AreaModel.class, instanceURI, null);
    }
}
