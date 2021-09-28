package org.opensilex.core.scientificObject.dal;

import org.opensilex.sparql.service.SparqlSearchFilter;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public class ScientificObjectSearchFilter extends SparqlSearchFilter {

    protected List<URI> uris;

    protected List<URI> excludedUris;

    protected URI experiment;

    protected List<URI> rdfTypes;

    protected String pattern;

    protected URI parentURI;

    /**
     * Flag which indicate if only OS with no parent must be retrieved
     */
    protected Boolean onlyFetchOsWithNoParent;

    protected URI germplasm;

    protected List<URI> factorLevels;

    protected URI facility;

    protected LocalDate existenceDate;

    protected LocalDate creationDate;

    public List<URI> getUris() {
        return uris;
    }

    public ScientificObjectSearchFilter setUris(List<URI> uris) {
        this.uris = uris;
        return this;
    }

    public List<URI> getExcludedUris() {
        return excludedUris;
    }

    public ScientificObjectSearchFilter setExcludedUris(List<URI> excludedUris) {
        this.excludedUris = excludedUris;
        return this;
    }

    public URI getExperiment() {
        return experiment;
    }

    public ScientificObjectSearchFilter setExperiment(URI experiment) {
        this.experiment = experiment;
        return this;
    }

    public List<URI> getRdfTypes() {
        return rdfTypes;
    }

    public ScientificObjectSearchFilter setRdfTypes(List<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public ScientificObjectSearchFilter setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public URI getParentURI() {
        return parentURI;
    }

    public ScientificObjectSearchFilter setParentURI(URI parentURI) {
        this.parentURI = parentURI;
        return this;
    }

    public URI getGermplasm() {
        return germplasm;
    }

    public ScientificObjectSearchFilter setGermplasm(URI germplasm) {
        this.germplasm = germplasm;
        return this;
    }

    public List<URI> getFactorLevels() {
        return factorLevels;
    }

    public ScientificObjectSearchFilter setFactorLevels(List<URI> factorLevels) {
        this.factorLevels = factorLevels;
        return this;
    }

    public URI getFacility() {
        return facility;
    }

    public ScientificObjectSearchFilter setFacility(URI facility) {
        this.facility = facility;
        return this;
    }

    public LocalDate getExistenceDate() {
        return existenceDate;
    }

    public ScientificObjectSearchFilter setExistenceDate(LocalDate existenceDate) {
        this.existenceDate = existenceDate;
        return this;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public ScientificObjectSearchFilter setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public Boolean getOnlyFetchOsWithNoParent() {
        return onlyFetchOsWithNoParent;
    }

    public ScientificObjectSearchFilter setOnlyFetchOsWithNoParent(Boolean onlyFetchOsWithNoParent) {
        this.onlyFetchOsWithNoParent = onlyFetchOsWithNoParent;
        return this;
    }
}
