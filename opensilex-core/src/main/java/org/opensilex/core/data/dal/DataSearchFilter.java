package org.opensilex.core.data.dal;

import org.bson.Document;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.security.account.dal.AccountModel;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

public class DataSearchFilter extends MongoSearchFilter {

    AccountModel user;
    Collection<URI> experiments;
    Collection<URI> targets;
    Collection<URI> variables;
    Collection<URI> provenances;
    Collection<URI> devices;
    Instant startDate;
    Instant endDate;
    Float confidenceMin;
    Float confidenceMax;
    Document metadata;
    Collection<URI> operators;

    public AccountModel getUser() {
        return user;
    }

    public DataSearchFilter setUser(AccountModel user) {
        this.user = user;
        return this;
    }

    public Collection<URI> getExperiments() {
        return experiments;
    }

    public DataSearchFilter setExperiments(Collection<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public Collection<URI> getTargets() {
        return targets;
    }

    public DataSearchFilter setTargets(Collection<URI> targets) {
        this.targets = targets;
        return this;
    }

    public Collection<URI> getVariables() {
        return variables;
    }

    public DataSearchFilter setVariables(Collection<URI> variables) {
        this.variables = variables;
        return this;
    }

    public Collection<URI> getProvenances() {
        return provenances;
    }

    public DataSearchFilter setProvenances(Collection<URI> provenances) {
        this.provenances = provenances;
        return this;
    }

    public Collection<URI> getDevices() {
        return devices;
    }

    public DataSearchFilter setDevices(Collection<URI> devices) {
        this.devices = devices;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public DataSearchFilter setStartDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public DataSearchFilter setEndDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public Float getConfidenceMin() {
        return confidenceMin;
    }

    public DataSearchFilter setConfidenceMin(Float confidenceMin) {
        this.confidenceMin = confidenceMin;
        return this;
    }

    public Float getConfidenceMax() {
        return confidenceMax;
    }

    public DataSearchFilter setConfidenceMax(Float confidenceMax) {
        this.confidenceMax = confidenceMax;
        return this;
    }

    public Document getMetadata() {
        return metadata;
    }

    public DataSearchFilter setMetadata(Document metadata) {
        this.metadata = metadata;
        return this;
    }

    public Collection<URI> getOperators() {
        return operators;
    }

    public DataSearchFilter setOperators(Collection<URI> operators) {
        this.operators = operators;
        return this;
    }
}