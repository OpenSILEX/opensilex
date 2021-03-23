/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class CSVURINotFoundError extends CSVCell {

    private final URI rdfType;

    private final URI objectURI;

    public CSVURINotFoundError(CSVCell cell, URI classURI, URI objectURI) {
        super(cell);
        this.rdfType = classURI;
        this.objectURI = objectURI;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public URI getObjectURI() {
        return objectURI;
    }

}
