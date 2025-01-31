package org.opensilex.core.data.dal;

import org.bson.Document;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;
import org.opensilex.security.account.dal.AccountModel;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

public class DataFileSearchFilter extends MongoSearchFilter {

    String name;
    AccountModel user;
    Collection<URI> experiments;
    Collection<URI> targets;
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

    public DataFileSearchFilter setUser(AccountModel user) {
        this.user = user;
        return this;
    }

    public Collection<URI> getExperiments() {
        return experiments;
    }

    public DataFileSearchFilter setExperiments(Collection<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public Collection<URI> getTargets() {
        return targets;
    }

    public DataFileSearchFilter setTargets(Collection<URI> targets) {
        this.targets = targets;
        return this;
    }

    public Collection<URI> getProvenances() {
        return provenances;
    }

    public DataFileSearchFilter setProvenances(Collection<URI> provenances) {
        this.provenances = provenances;
        return this;
    }

    public Collection<URI> getDevices() {
        return devices;
    }

    public DataFileSearchFilter setDevices(Collection<URI> devices) {
        this.devices = devices;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public DataFileSearchFilter setStartDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public DataFileSearchFilter setEndDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public Float getConfidenceMin() {
        return confidenceMin;
    }

    public DataFileSearchFilter setConfidenceMin(Float confidenceMin) {
        this.confidenceMin = confidenceMin;
        return this;
    }

    public Float getConfidenceMax() {
        return confidenceMax;
    }

    public DataFileSearchFilter setConfidenceMax(Float confidenceMax) {
        this.confidenceMax = confidenceMax;
        return this;
    }

    public Document getMetadata() {
        return metadata;
    }

    public DataFileSearchFilter setMetadata(Document metadata) {
        this.metadata = metadata;
        return this;
    }

    public Collection<URI> getOperators() {
        return operators;
    }

    public DataFileSearchFilter setOperators(Collection<URI> operators) {
        this.operators = operators;
        return this;
    }

    public String getName(){
        return name;
    }

    public DataFileSearchFilter setName(String name) {
        this.name = name;
        return this;
    }
}
