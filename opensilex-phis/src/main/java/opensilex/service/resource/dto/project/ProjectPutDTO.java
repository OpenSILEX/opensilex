//******************************************************************************
//                                ProjectPutDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 12 juil. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.project;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Contact;
import opensilex.service.model.FinancialFunding;
import opensilex.service.model.Project;
import opensilex.service.resource.ProjectResourceService;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.user.dal.UserDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.service.SPARQLService;

/**
 * The DTO for the PUT projects service.
 *
 * @see ProjectResourceService#put(ArrayList, HttpServletRequest, SecurityContext)
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectPutDTO extends AbstractVerifiedClass {

    //URI of the project to update.
    private String uri;
    //The name of the project.
    private String name;
    //A shortname for the project
    private String shortname;
    //The list of projects URI related to the project.
    private List<String> relatedProjects = new ArrayList<>();
    //The URI of the financial funding type.
    private String financialFunding;
    private String financialReference;
    //The description of the project.
    private String description;
    //The start date of the project. The format should be YYYY-MM-JJ
    private String startDate;
    //The end date of the project. The format should be YYYY-MM-JJ
    private String endDate;
    //A list of keyword corresponding to the project.
    private List<String> keywords = new ArrayList<>();
    //The home page of the project.
    private String homePage;
    //The list of administrative contacts of the project.
    private List<String> administrativeContacts = new ArrayList<>();
    //The list of coordinators of the projects.
    private List<String> coordinators = new ArrayList<>();
    //The list of scientific contacts of the project.
    private List<String> scientificContacts = new ArrayList<>();
    //The objective of the project.
    private String objective;

    @Override
    public Project createObjectFromDTO() throws Exception {
        Project project = new Project();
        project.setUri(uri);
        project.setName(name);
        project.setShortname(shortname);

        for (String relatedProjectUri : relatedProjects) {
            Project relatedProject = new Project();
            relatedProject.setUri(relatedProjectUri);
            project.addRelatedProject(relatedProject);
        }

        if (financialFunding != null) {
            FinancialFunding financialFundingObject = new FinancialFunding();
            financialFundingObject.setUri(new URI(financialFunding));
            project.setFinancialFunding(financialFundingObject);
        }

        project.setFinancialReference(financialReference);

        project.setDescription(description);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setKeywords(keywords);
        project.setHomePage(homePage);

        for (String administrativeContactUri : administrativeContacts) {
            Contact contact = new Contact();
            contact.setUri(administrativeContactUri);
            project.addAdministrativeContact(contact);
        }

        for (String coordinatorUri : coordinators) {
            Contact contact = new Contact();
            contact.setUri(coordinatorUri);
            project.addCoordinator(contact);
        }

        for (String scientificContactUri : scientificContacts) {
            Contact contact = new Contact();
            contact.setUri(scientificContactUri);
            project.addScientificContact(contact);
        }

        project.setObjective(objective);

        return project;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_FINANCIAL_REFERENCE)
    public String getFinancialReference() {
        return financialReference;
    }

    public void setFinancialReference(String financialReference) {
        this.financialReference = financialReference;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_NAME)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_SHORTNAME)
    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public List<String> getRelatedProjects() {
        return relatedProjects;
    }

    public void setRelatedProjects(List<String> relatedProjects) {
        this.relatedProjects = relatedProjects;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_FINANCIAL_URI)
    public String getFinancialFunding() {
        return financialFunding;
    }

    public void setFinancialFunding(String financialFunding) {
        this.financialFunding = financialFunding;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_DESCRIPTION)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Date(DateFormat.YMD)
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_START)
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Date(DateFormat.YMD)
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_END)
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_HOME_PAGE)
    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public List<String> getAdministrativeContacts() {
        return administrativeContacts;
    }

    public void setAdministrativeContacts(List<String> administrativeContacts) {
        this.administrativeContacts = administrativeContacts;
    }

    public List<String> getCoordinators() {
        return coordinators;
    }

    public void setCoordinators(List<String> coordinators) {
        this.coordinators = coordinators;
    }

    public List<String> getScientificContacts() {
        return scientificContacts;
    }

    public void setScientificContacts(List<String> scientificContacts) {
        this.scientificContacts = scientificContacts;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROJECT_OBJECTIVE)
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public ProjectModel getProjectModel(SPARQLService sparql, String lang) throws Exception {
        ProjectModel project = new ProjectModel();

        project.setUri(new URI(this.getUri()));

        project.setName(this.getName());
        project.setShortname(this.getShortname());
        project.setDescription(this.getDescription());
        project.setObjective(this.getObjective());

        project.setStartDate(LocalDate.parse(this.getStartDate()));
        project.setEndDate(LocalDate.parse(this.getEndDate()));

        project.setKeywords(this.getKeywords());
        if (this.getHomePage() != null && !this.getHomePage().isEmpty()) {
            project.setHomePage(new URI(this.getHomePage()));
        }

        UserDAO userDAO = new UserDAO(sparql);
        List<UserModel> addresses = new ArrayList<>();
        for (String contact : this.getAdministrativeContacts()) {
            if (contact != null && !contact.isEmpty()) {
                addresses.add(userDAO.get(new URI(contact)));
            }
        }
        project.setAdministrativeContacts(addresses);

        addresses = new ArrayList<>();
        for (String contact : this.getCoordinators()) {
            if (contact != null && !contact.isEmpty()) {
                addresses.add(userDAO.get(new URI(contact)));
            }
        }
        project.setCoordinators(addresses);

        addresses = new ArrayList<>();
        for (String contact : this.getScientificContacts()) {
            if (contact != null && !contact.isEmpty()) {
                addresses.add(userDAO.get(new URI(contact)));
            }
        }
        project.setScientificContacts(addresses);

        List<URI> uriList = new ArrayList<>();
        for (String projectUri : this.getRelatedProjects()) {
            uriList.add(new URI(projectUri));
        }
        project.setRelatedProjects(sparql.getListByURIs(ProjectModel.class, uriList, lang));

        ArrayList<SPARQLModelRelation> sparqlRelations = new ArrayList<SPARQLModelRelation>();
        if (this.getFinancialReference() != null && !this.getFinancialReference().isEmpty()) {
            SPARQLModelRelation financialRefRelation = new SPARQLModelRelation();
            financialRefRelation.setProperty(ProjectResourceService.hasFinancialReference);
            financialRefRelation.setValue(this.getFinancialReference());
            financialRefRelation.setType(String.class);
            sparqlRelations.add(financialRefRelation);
        }

        if (this.getFinancialFunding() != null) {
            SPARQLModelRelation financialRefRelation = new SPARQLModelRelation();
            financialRefRelation.setProperty(ProjectResourceService.hasFinancialFunding);
            financialRefRelation.setValue(this.getFinancialFunding());
            financialRefRelation.setType(URI.class);
            sparqlRelations.add(financialRefRelation);
        }
        
        project.setRelations(sparqlRelations);
        return project;
    }
}
