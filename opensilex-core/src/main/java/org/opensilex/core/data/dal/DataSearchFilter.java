package org.opensilex.core.data.dal;

import org.bson.Document;
import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class DataSearchFilter extends MongoSearchFilter {

    private Instant startDate;

    private Instant endDate;

    private String timezone;

    private List<URI> experiments;

    private List<URI> targets;

    private List<URI> variables;

    private List<URI> devices;

    private Float confidenceMin;

    private Float confidenceMax;

    private List<URI> provenances;

    private List<URI> operators;

    private Document metadata;

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

    public String getTimezone() {
        return timezone;
    }

    public DataSearchFilter setTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public DataSearchFilter setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public DataSearchFilter setExperiments(URI experiment) {
        if(experiment != null){
            this.experiments = Collections.singletonList(experiment);
        }
        return this;
    }

    public List<URI> getTargets() {
        return targets;
    }

    public DataSearchFilter setTargets(List<URI> targets) {
        this.targets = targets;
        return this;
    }

    public DataSearchFilter setTargets(URI target) {
        if(target != null){
            this.targets = Collections.singletonList(target);
        }
        return this;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public DataSearchFilter setVariables(List<URI> variables) {
        this.variables = variables;
        return this;
    }

    public DataSearchFilter setVariables(URI variable) {
        if(variable != null){
            this.variables = Collections.singletonList(variable);
        }
        return this;
    }

    public List<URI> getDevices() {
        return devices;
    }

    public DataSearchFilter setDevices(List<URI> devices) {
        this.devices = devices;
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

    public List<URI> getProvenances() {
        return provenances;
    }

    public DataSearchFilter setProvenances(List<URI> provenances) {
        this.provenances = provenances;
        return this;
    }

    public DataSearchFilter setProvenances(URI provenance) {
        if(provenance != null){
            this.provenances = Collections.singletonList(provenance);
        }
        return this;
    }

    public Document getMetadata() {
        return metadata;
    }

    public DataSearchFilter setMetadata(Document metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<URI> getOperators() {
        return operators;
    }

    public DataSearchFilter setOperators(List<URI> operators) {
        this.operators = operators;
        return this;
    }
}
