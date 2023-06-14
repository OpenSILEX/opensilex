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
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.time.OffsetDateTime;
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
    public static final String EXACT_MATCH_FIELD = "exactMatch";

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "closeMatch"
    )
    private List<URI> closeMatch;
    public static final String CLOSE_MATCH_FIELD = "closeMatch";

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "broadMatch"
    )
    private List<URI> broadMatch;
    public static final String BROAD_MATCH_FIELD = "broadMatch";

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "narrowMatch"
    )
    private List<URI> narrowMatch;
    public static final String NARROW_MATCH_FIELD = "narrowMatch";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "fromSharedResourceInstance",
            ignoreUpdateIfNull = true
    )
    private URI fromSharedResourceInstance;

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "modified",
            ignoreUpdateIfNull = true
    )
    private OffsetDateTime lastUpdateTime;

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
}
