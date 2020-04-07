//******************************************************************************
//                                AdditionalInfo.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 14 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

import java.util.ArrayList;

/**
 * Represents the additionalInfo of a study
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class AdditionalInfo {
    private String keywords;
    private ArrayList<String> projectsNames;

    public AdditionalInfo() {
        this.projectsNames = new ArrayList();
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public ArrayList<String> getProjectsNames() {
        return projectsNames;
    }

    public void addProjectsNames(String projectName) {
        this.projectsNames.add(projectName);
    }
    
}
