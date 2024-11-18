package org.opensilex.core.dataV2.model;


import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.util.List;
import java.util.Map;

public class TargetContext {
    List<String> notExistingTargets;
    List<String> duplicatedTargets;
    Map<String, SPARQLNamedResourceModel> nameURITargets;
    List<String> scientificObjectsNotInXp;
    Map<String, SPARQLNamedResourceModel> nameURIScientificObjects;

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

    public static TargetContext buildTargetContext(List<String> duplicatedTargets, Map<String, SPARQLNamedResourceModel> nameURITargets, List<String> notExistingTargets, Map<String, SPARQLNamedResourceModel> nameURIScientificObjectsInXp, List<String> scientificObjectsNotInXp) {
        TargetContext targetContext = new TargetContext();
        targetContext.setDuplicatedTargets(duplicatedTargets);
        targetContext.setNameURITargets(nameURITargets);
        targetContext.setNotExistingTargets(notExistingTargets);
        targetContext.setNameURIScientificObjects(nameURIScientificObjectsInXp);
        targetContext.setScientificObjectsNotInXp(scientificObjectsNotInXp);
        return targetContext;
    }
}
