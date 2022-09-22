package org.opensilex.core.variable.dal;

import org.opensilex.OpenSilex;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SparqlSearchFilter;

import java.net.URI;
import java.util.List;

/**
 * Object which group all filters (can be null/empty) which can apply for a {@link VariableModel} search
 * @author rcolin
 */
public class VariableSearchFilter extends SparqlSearchFilter {

    private String namePattern;
    private URI entity;
    private URI interestEntity;
    private URI characteristic;
    private URI method;
    private URI unit;
    private URI group;
    private URI dataType;
    private String timeInterval;
    private List<URI> species;
    private boolean withAssociatedData;
    private List<URI> devices;
    private List<URI> experiments;
    private List<URI> objects;

    private UserModel userModel;

    private boolean fetchSpecies;

    public VariableSearchFilter() {
        super();
        this.fetchSpecies = false;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public VariableSearchFilter setNamePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }

    public URI getEntity() {
        return entity;
    }

    public VariableSearchFilter setEntity(URI entity) {
        this.entity = entity;
        return this;
    }

    public URI getInterestEntity() {
        return interestEntity;
    }

    public VariableSearchFilter setInterestEntity(URI interestEntity) {
        this.interestEntity = interestEntity;
        return this;
    }

    public URI getCharacteristic() {
        return characteristic;
    }

    public VariableSearchFilter setCharacteristic(URI characteristic) {
        this.characteristic = characteristic;
        return this;
    }

    public URI getMethod() {
        return method;
    }

    public VariableSearchFilter setMethod(URI method) {
        this.method = method;
        return this;
    }

    public URI getUnit() {
        return unit;
    }

    public VariableSearchFilter setUnit(URI unit) {
        this.unit = unit;
        return this;
    }

    public URI getGroup() {
        return group;
    }

    public VariableSearchFilter setGroup(URI group) {
        this.group = group;
        return this;
    }

    public URI getDataType() {
        return dataType;
    }

    public VariableSearchFilter setDataType(URI dataType) {
        this.dataType = dataType;
        return this;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public VariableSearchFilter setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
        return this;
    }

    public List<URI> getSpecies() {
        return species;
    }

    public VariableSearchFilter setSpecies(List<URI> species) {
        this.species = species;
        return this;
    }

    public boolean isWithAssociatedData() {
        return withAssociatedData;
    }

    public VariableSearchFilter setWithAssociatedData(boolean withAssociatedData) {
        this.withAssociatedData = withAssociatedData;
        return this;
    }

    public List<URI> getDevices() {
        return devices;
    }

    public VariableSearchFilter setDevices(List<URI> devices) {
        this.devices = devices;
        return this;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public VariableSearchFilter setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public List<URI> getObjects() {
        return objects;
    }

    public VariableSearchFilter setObjects(List<URI> objects) {
        this.objects = objects;
        return this;
    }

    public UserModel getUserModel() {
        return userModel;
    }

    public VariableSearchFilter setUserModel(UserModel userModel) {
        this.userModel = userModel;
        return this;
    }

    public boolean isFetchSpecies() {
        return fetchSpecies;
    }

    public VariableSearchFilter setFetchSpecies(boolean fetchSpecies) {
        this.fetchSpecies = fetchSpecies;
        return this;
    }
}
