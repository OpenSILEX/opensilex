package org.opensilex.core.dataV2.model;


import org.opensilex.core.experiment.dal.ExperimentModel;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class ExperimentContext {
    private URI experiment;
    private List<String> notExistingExperiments;
    private List<String> duplicatedExperiments;
    private Map<String, ExperimentModel> nameURIExperiments;

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public Map<String, ExperimentModel> getNameURIExperiments() {
        return nameURIExperiments;
    }

    public void setNameURIExperiments(Map<String, ExperimentModel> nameURIExperiments) {
        this.nameURIExperiments = nameURIExperiments;
    }

    public List<String> getNotExistingExperiments() {
        return notExistingExperiments;
    }

    public void setNotExistingExperiments(List<String> notExistingExperiments) {
        this.notExistingExperiments = notExistingExperiments;
    }

    public List<String> getDuplicatedExperiments() {
        return duplicatedExperiments;
    }

    public void setDuplicatedExperiments(List<String> duplicatedExperiments) {
        this.duplicatedExperiments = duplicatedExperiments;
    }

    public static ExperimentContext buildExperimentContext(URI experiment, List<String> duplicatedExperiments, Map<String, ExperimentModel> nameURIExperiments, List<String> notExistingExperiments) {
        ExperimentContext experimentContext = new ExperimentContext();
        experimentContext.setExperiment(experiment);
        experimentContext.setDuplicatedExperiments(duplicatedExperiments);
        experimentContext.setNameURIExperiments(nameURIExperiments);
        experimentContext.setNotExistingExperiments(notExistingExperiments);
        return experimentContext;
    }
}
