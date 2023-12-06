package org.opensilex.nosql.mongodb.model;

import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public class MongoTestModel extends MongoModel {

    private String name;
    public static final String NAME_FIELD = "name";

    private Long id;
    public static final String ID_FIELD = "id";


    private MongoNestTestModel target;
    private URI variable;
    private Object value;
    private Instant timestamp;

    public static final String TIMESTAMP_FIELD = "timestamp";
    private List<MongoNestTestModel> agents;
    private List<MongoNestTestModel> activities;

    private URI experiment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MongoNestTestModel getTarget() {
        return target;
    }

    public void setTarget(MongoNestTestModel target) {
        this.target = target;
    }

    public URI getVariable() {
        return variable;
    }

    public void setVariable(URI variable) {
        this.variable = variable;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<MongoNestTestModel> getAgents() {
        return agents;
    }

    public void setAgents(List<MongoNestTestModel> agents) {
        this.agents = agents;
    }

    public List<MongoNestTestModel> getActivities() {
        return activities;
    }

    public void setActivities(List<MongoNestTestModel> activities) {
        this.activities = activities;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }
}
