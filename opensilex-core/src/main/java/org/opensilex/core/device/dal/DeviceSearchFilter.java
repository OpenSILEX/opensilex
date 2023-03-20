package org.opensilex.core.device.dal;

import org.bson.Document;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SparqlSearchFilter;

import java.net.URI;
import java.time.LocalDate;

public class DeviceSearchFilter extends SparqlSearchFilter {

    private String namePattern;
    private URI rdfType;
    private Boolean includeSubTypes;
    private URI variable;
    private Integer year;
    private LocalDate existenceDate;
    private String brandPattern;
    private String modelPattern;
    private String snPattern;
    private Document metadata;
    private AccountModel currentUser;

    public DeviceSearchFilter() {
        super();
    }

    public String getNamePattern() {
        return namePattern;
    }

    public DeviceSearchFilter setNamePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public DeviceSearchFilter setRdfType(URI rdfType) {
        this.rdfType = rdfType;
        return this;
    }

    public Boolean getIncludeSubTypes() {
        return includeSubTypes;
    }

    public DeviceSearchFilter setIncludeSubTypes(Boolean includeSubTypes) {
        this.includeSubTypes = includeSubTypes;
        return this;
    }

    public URI getVariable() {
        return variable;
    }

    public DeviceSearchFilter setVariable(URI variable) {
        this.variable = variable;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public DeviceSearchFilter setYear(Integer year) {
        this.year = year;
        return this;
    }

    public LocalDate getExistenceDate() {
        return existenceDate;
    }

    public DeviceSearchFilter setExistenceDate(LocalDate existenceDate) {
        this.existenceDate = existenceDate;
        return this;
    }

    public String getBrandPattern() {
        return brandPattern;
    }

    public DeviceSearchFilter setBrandPattern(String brandPattern) {
        this.brandPattern = brandPattern;
        return this;
    }

    public String getModelPattern() {
        return modelPattern;
    }

    public DeviceSearchFilter setModelPattern(String modelPattern) {
        this.modelPattern = modelPattern;
        return this;
    }

    public String getSnPattern() {
        return snPattern;
    }

    public DeviceSearchFilter setSnPattern(String snPattern) {
        this.snPattern = snPattern;
        return this;
    }

    public Document getMetadata() {
        return metadata;
    }

    public DeviceSearchFilter setMetadata(Document metadata) {
        this.metadata = metadata;
        return this;
    }

    public AccountModel getCurrentUser() {
        return currentUser;
    }

    public DeviceSearchFilter setCurrentUser(AccountModel currentUser) {
        this.currentUser = currentUser;
        return this;
    }
}
