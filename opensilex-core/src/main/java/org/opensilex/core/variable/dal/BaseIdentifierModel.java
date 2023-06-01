/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SKOSReferences;
import org.opensilex.core.variable.api.LabelDTO;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;


public abstract class BaseIdentifierModel<T extends SPARQLNamedResourceModel<T>> extends SPARQLNamedResourceModel<T> implements SKOSReferences {

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
            property = "broadMatch"
    )
    private List<URI> broadMatch;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "narrowMatch"
    )
    private List<URI> narrowMatch;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromSharedResourceInstance"
    )
    private URI fromSharedResourceInstance;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = ""

    )
    private List<LabelDTO> labelDTOs;

    public static final String labels_INFO = "labels_INFO";


    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "modified"
    )
    private OffsetDateTime lastUpdateTime;


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
    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    @Override
    public void setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
    }

    @Override
    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    @Override
    public void setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
    }

    public URI getFromSharedResourceInstance() {
        return fromSharedResourceInstance;
    }

    public void setFromSharedResourceInstance(URI fromSharedResourceInstance) {
        this.fromSharedResourceInstance = fromSharedResourceInstance;
    }

    public OffsetDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(OffsetDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }


    public List<LabelDTO> getLabelDTOs() {
        return labelDTOs;
    }

    public void setLabelDTOs(List<LabelDTO> labelDTOs) {
        this.labelDTOs = labelDTOs;
    }



}
