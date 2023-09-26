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

import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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
        AreaModel area = new AreaModel(uri, name, rdfType, description, user);

        sparql.create(area);

        return area.getUri();
    }

    public AreaModel getByURI(URI instanceURI) throws Exception {
        return sparql.getByURI(AreaModel.class, instanceURI, null);
    }

    public void delete(URI areaURI) throws Exception {
        sparql.delete(AreaModel.class, areaURI);
    }

    public URI update(URI uri, String name, URI rdfType, String description, URI user) throws Exception {
        AreaModel area = new AreaModel(uri, name, rdfType, description, user);
        sparql.update(area);

        return area.getUri();
    }

    public List<AreaModel> searchByURIs(List<URI> areaURI, AccountModel currentUser) throws Exception {
        List<URI> uniqueAreasUri = areaURI.stream()
                .distinct()
                .collect(Collectors.toList());
        return sparql.getListByURIs(AreaModel.class, uniqueAreasUri, currentUser.getLanguage());
    }
}