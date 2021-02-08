/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.SKOSReferences;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.List;


public abstract class BaseVariableModel<T extends SPARQLNamedResourceModel<T>> extends SPARQLNamedResourceModel<T> implements SKOSReferences {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "comment";

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "exactMatch"
    )
    private List<URI> exactMatch;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "closeMatch"
    )
    private List<URI> closeMatch;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "broader"
    )
    private List<URI> broader;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "narrower"
    )
    private List<URI> narrower;

    public String getDescription() {
        return description;
    }

    public void setDescription(String comment) {
        this.description = comment;
    }

    @Override
    public List<URI> getExactMatch() {
        return exactMatch;
    }

    @Override
    public void setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
    }

    @Override
    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    @Override
    public void setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
    }

    @Override
    public List<URI> getBroader() {
        return broader;
    }

    @Override
    public void setBroader(List<URI> broader) {
        this.broader = broader;
    }

    @Override
    public List<URI> getNarrower() {
        return narrower;
    }

    @Override
    public void setNarrower(List<URI> narrower) {
        this.narrower = narrower;
    }
}
