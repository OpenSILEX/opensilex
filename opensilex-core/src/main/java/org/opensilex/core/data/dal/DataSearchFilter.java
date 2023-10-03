package org.opensilex.core.data.dal;

import org.opensilex.nosql.mongodb.dao.MongoSearchFilter;

import java.net.URI;
import java.util.List;

public class DataSearchFilter extends MongoSearchFilter {

    private String startDate;

    private String endDate;

    private String timezone;

    private List<URI> experiments;

    private List<URI> objects;

    private List<URI> variables;

    private List<URI> targets;

    private Float confidenceMin;

    private Float confidenceMax;

    private List<URI> provenances;

    public String getStartDate() {
        return startDate;
    }

    public DataSearchFilter setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public DataSearchFilter setEndDate(String endDate) {
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

    public List<URI> getObjects() {
        return objects;
    }

    public DataSearchFilter setObjects(List<URI> objects) {
        this.objects = objects;
        return this;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public DataSearchFilter setVariables(List<URI> variables) {
        this.variables = variables;
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
}
