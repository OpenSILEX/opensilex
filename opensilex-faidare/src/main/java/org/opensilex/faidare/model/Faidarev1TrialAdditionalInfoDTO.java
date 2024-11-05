package org.opensilex.faidare.model;

import java.util.List;

public class Faidarev1TrialAdditionalInfoDTO {
    private String shortName;
    private String description;
    private String financialFunding;
    private List<String> relatedProjects;
    private List<Faidarev1ContactDTO> coordinators;

    public String getShortName() {
        return shortName;
    }

    public Faidarev1TrialAdditionalInfoDTO setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Faidarev1TrialAdditionalInfoDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getFinancialFunding() {
        return financialFunding;
    }

    public Faidarev1TrialAdditionalInfoDTO setFinancialFunding(String financialFunding) {
        this.financialFunding = financialFunding;
        return this;
    }

    public List<String> getRelatedProjects() {
        return relatedProjects;
    }

    public Faidarev1TrialAdditionalInfoDTO setRelatedProjects(List<String> relatedProjects) {
        this.relatedProjects = relatedProjects;
        return this;
    }

    public List<Faidarev1ContactDTO> getCoordinators() {
        return coordinators;
    }

    public Faidarev1TrialAdditionalInfoDTO setCoordinators(List<Faidarev1ContactDTO> coordinators) {
        this.coordinators = coordinators;
        return this;
    }

    public Faidarev1TrialAdditionalInfoDTO(String shortName, String description, String financialFunding, List<String> relatedProjects, List<Faidarev1ContactDTO> coordinators){
        this.shortName = shortName;
        this.description = description;
        this.financialFunding = financialFunding;
        this.relatedProjects = relatedProjects;
        this.coordinators = coordinators;
    }
}
