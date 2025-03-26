package org.opensilex.core.data.bll.dataImport;


import org.opensilex.core.experiment.dal.ExperimentModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentContext {
    private URI experiment;
    private List<String> notExistingExperiments = new ArrayList<>();
    private List<String> duplicatedExperiments = new ArrayList<>();
    private Map<String, ExperimentModel> nameURIExperiments = new HashMap<>();
    ;

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

    public static ExperimentContext buildExperimentContext(URI experiment) {
        ExperimentContext experimentContext = new ExperimentContext();
        experimentContext.setExperiment(experiment);
        return experimentContext;
    }
}
