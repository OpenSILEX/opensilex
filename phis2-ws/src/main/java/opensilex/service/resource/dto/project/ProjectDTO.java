//******************************************************************************
//                                ProjectDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 16 juil. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.project;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.FinancialFunding;
import opensilex.service.model.Project;
import opensilex.service.resource.ProjectResourceService;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.RdfResourceDTO;

/**
 * This class is the project DTO for the get project by search.
 * @see ProjectResourceService#getBySearch(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectDTO extends AbstractVerifiedClass {
    
    //URI of the project
    private String uri;
    //Foaf:name of the project
    private String name;
    //Shortname of the project
    private String shortname;
    //Financial funding of the project
    private RdfResourceDTO financialFunding;
    //Financial reference of the project
    private String financialReference;
    //dcterms:description of the project
    private String description;
    //Start date of the project
    private String startDate;
    //End date of the project
    private String endDate;
    //Foaf:homePage of the project
    private String homePage;
    //Objective of the project
    private String objective;
    
    public ProjectDTO(Project project) {
        uri = project.getUri();
        name = project.getName();
        shortname = project.getShortname();
        financialReference = project.getFinancialReference();
        description = project.getDescription();
        startDate = project.getStartDate();
        endDate = project.getEndDate();
        homePage = project.getHomePage();
        objective = project.getObjective();
        
        if (project.getFinancialFunding() != null) {
            FinancialFunding financialFundingObject = project.getFinancialFunding();
            financialFunding = new RdfResourceDTO(financialFundingObject.getUri(), financialFundingObject.getLabel());
        }
    }

    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_NAME)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_SHORTNAME)
    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public RdfResourceDTO getFinancialFunding() {
        return financialFunding;
    }

    public void setFinancialFunding(RdfResourceDTO financialFunding) {
        this.financialFunding = financialFunding;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_FINANCIAL_REFERENCE)
    public String getFinancialReference() {
        return financialReference;
    }

    public void setFinancialReference(String financialReference) {
        this.financialReference = financialReference;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_DESCRIPTION)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_START)
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_END)
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_HOME_PAGE)
    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_OBJECTIVE)
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }
}
