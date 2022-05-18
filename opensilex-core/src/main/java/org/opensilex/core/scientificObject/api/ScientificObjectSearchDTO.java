/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * @author vince
 */
public class ScientificObjectSearchDTO {

    @ValidURI
    @JsonProperty("uris")
    protected List<URI> uris;

    @ValidURI
    @JsonProperty("excluded_uris")
    protected List<URI> excludedUris;

    @ValidURI
    @JsonProperty("experiment")
    protected URI experiment;

    @ValidURI
    @JsonProperty("rdf_types")
    protected List<URI> rdfTypes;

    @JsonProperty("name")
    protected String pattern;

    @ValidURI
    @JsonProperty("parent")
    protected URI parentURI;

    @ValidURI
    @JsonProperty("germplasm")
    protected URI germplasm;

    @ValidURI
    @JsonProperty("factor_levels")
    protected List<URI> factorLevels;

    @ValidURI
    @JsonProperty("facility")
    protected URI facility;

    @JsonProperty("existence_date")
    protected LocalDate existenceDate;

    @JsonProperty("creation_date")
    protected LocalDate creationDate;

    @JsonProperty("order_by")
    protected List<OrderBy> orderByList;

    @JsonProperty("page")
    protected Integer page;

    @JsonProperty("page_size")
    protected Integer pageSize;

    public List<URI> getUris() {
        return uris;
    }

    public List<URI> getExcludedUris() {
        return excludedUris;
    }

    public URI getExperiment() {
        return experiment;
    }

    public List<URI> getRdfTypes() {
        return rdfTypes;
    }

    public String getPattern() {
        return pattern;
    }

    public URI getParentURI() {
        return parentURI;
    }

    public URI getGermplasm() {
        return germplasm;
    }

    public List<URI> getFactorLevels() {
        return factorLevels;
    }

    public URI getFacility() {
        return facility;
    }

    public LocalDate getExistenceDate() {
        return existenceDate;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public List<OrderBy> getOrderByList() {
        return orderByList;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public ScientificObjectSearchDTO setUris(List<URI> uris) {
        this.uris = uris;
        return this;
    }

    public ScientificObjectSearchDTO setExcludedUris(List<URI> excludedUris) {
        this.excludedUris = excludedUris;
        return this;
    }

    public ScientificObjectSearchDTO setExperiment(URI experiment) {
        this.experiment = experiment;
        return this;
    }

    public ScientificObjectSearchDTO setRdfTypes(List<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
        return this;
    }

    public ScientificObjectSearchDTO setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public ScientificObjectSearchDTO setParentURI(URI parentURI) {
        this.parentURI = parentURI;
        return this;
    }

    public ScientificObjectSearchDTO setGermplasm(URI germplasm) {
        this.germplasm = germplasm;
        return this;
    }

    public ScientificObjectSearchDTO setFactorLevels(List<URI> factorLevels) {
        this.factorLevels = factorLevels;
        return this;
    }

    public ScientificObjectSearchDTO setFacility(URI facility) {
        this.facility = facility;
        return this;
    }

    public ScientificObjectSearchDTO setExistenceDate(LocalDate existenceDate) {
        this.existenceDate = existenceDate;
        return this;
    }

    public ScientificObjectSearchDTO setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public ScientificObjectSearchDTO setOrderByList(List<OrderBy> orderByList) {
        this.orderByList = orderByList;
        return this;
    }

    public ScientificObjectSearchDTO setPage(Integer page) {
        this.page = page;
        return this;
    }

    public ScientificObjectSearchDTO setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public ScientificObjectSearchFilter toModel(){

        ScientificObjectSearchFilter filter = new ScientificObjectSearchFilter()
                .setUris(uris)
                .setExcludedUris(excludedUris)
                .setExperiment(experiment)
                .setRdfTypes(rdfTypes)
                .setPattern(pattern)
                .setParentURI(parentURI)
                .setGermplasm(germplasm)
                .setFactorLevels(factorLevels)
                .setFacility(facility)
                .setExistenceDate(existenceDate)
                .setCreationDate(creationDate);

        filter.setPage(page)
                .setPageSize(pageSize)
                .setOrderByList(orderByList);

        return filter;
    }
}
