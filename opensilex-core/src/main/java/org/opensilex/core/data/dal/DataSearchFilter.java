package org.opensilex.core.data.dal;

import org.bson.Document;
import org.opensilex.nosql.dao.read.MongoSearchFilter;
import org.opensilex.security.account.dal.AccountModel;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class DataSearchFilter extends MongoSearchFilter {

    AccountModel user;
    List<URI> experiments;
    List<URI> targets;

    List<URI> variables;

    List<URI> provenances;
    List<URI> devices;
    Instant startDate;
    Instant endDate;

    Float confidenceMin;
    Float confidenceMax;

    Document metadata;


    public AccountModel getUser() {
        return user;
    }

    public DataSearchFilter setUser(AccountModel user) {
        this.user = user;
        return this;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public DataSearchFilter setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public DataSearchFilter setExperiment(URI experiment){
        if(experiment == null){
            return this;
        }
        return setExperiments(Collections.singletonList(experiment));
    }

    public List<URI> getTargets() {
        return targets;
    }

    public DataSearchFilter setTargets(List<URI> targets) {
        this.targets = targets;
        return this;
    }

    public DataSearchFilter setTarget(URI target){
        if(target == null){
            return this;
        }
        return setTargets(Collections.singletonList(target));
    }

    public List<URI> getProvenances() {
        return provenances;
    }

    public DataSearchFilter setProvenances(List<URI> provenances) {
        this.provenances = provenances;
        return this;
    }

    public DataSearchFilter setProvenance(URI provenance){
        if(provenance == null){
            return this;
        }
        return setProvenances(Collections.singletonList(provenance));
    }

    public List<URI> getDevices() {
        return devices;
    }

    public DataSearchFilter setDevices(List<URI> devices) {
        this.devices = devices;
        return this;
    }

    public DataSearchFilter setDevice(URI device){
        if(device == null){
            return this;
        }
        return setDevices(Collections.singletonList(device));
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

    public Document getMetadata() {
        return metadata;
    }

    public DataSearchFilter setMetadata(Document metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public DataSearchFilter setVariables(List<URI> variables) {
        this.variables = variables;
        return this;
    }

    public DataSearchFilter setVariable(URI variable){
        if(variable == null){
            return this;
        }
        return setVariables(Collections.singletonList(variable));
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
}
