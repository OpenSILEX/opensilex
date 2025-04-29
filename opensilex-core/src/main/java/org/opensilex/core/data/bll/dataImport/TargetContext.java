package org.opensilex.core.data.bll.dataImport;


import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TargetContext {
    List<String> notExistingTargets = new ArrayList<>();
    //This represents target uris that already exist multiple times in graph
    List<String> duplicatedTargets = new ArrayList<>();
    Map<String, SPARQLNamedResourceModel> nameURITargets = new HashMap<>();
    List<String> scientificObjectsNotInXp = new ArrayList<>();
    Map<String, SPARQLNamedResourceModel> nameURIScientificObjects = new HashMap<>();

    public List<String> getNotExistingTargets() {
        return notExistingTargets;
    }

    public void setNotExistingTargets(List<String> notExistingTargets) {
        this.notExistingTargets = notExistingTargets;
    }

    public List<String> getDuplicatedTargets() {
        return duplicatedTargets;
    }

    public void setDuplicatedTargets(List<String> duplicatedTargets) {
        this.duplicatedTargets = duplicatedTargets;
    }

    public Map<String, SPARQLNamedResourceModel> getNameURITargets() {
        return nameURITargets;
    }

    public void setNameURITargets(Map<String, SPARQLNamedResourceModel> nameURITargets) {
        this.nameURITargets = nameURITargets;
    }

    public List<String> getScientificObjectsNotInXp() {
        return scientificObjectsNotInXp;
    }

    public void setScientificObjectsNotInXp(List<String> scientificObjectsNotInXp) {
        this.scientificObjectsNotInXp = scientificObjectsNotInXp;
    }

    public Map<String, SPARQLNamedResourceModel> getNameURIScientificObjects() {
        return nameURIScientificObjects;
    }

    public void setNameURIScientificObjects(Map<String, SPARQLNamedResourceModel> nameURIScientificObjects) {
        this.nameURIScientificObjects = nameURIScientificObjects;
    }
    
}
