//******************************************************************************
//                                ProjectDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 juil. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Contact;
import opensilex.service.model.Project;
import opensilex.service.model.RdfResourceDefinition;
import opensilex.service.model.User;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Foaf;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.view.brapi.Status;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProjectDAO extends Rdf4jDAO<Project> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ProjectDAO.class);
    
    //Constants to query the triplestore
    private final String ADMINISTRATIVE_CONTACT = "administrativeContact";
    private final String COORDINATOR = "coordinator";
    private final String DATE_END = "dateEnd";
    private final String DATE_START = "dateStart";
    private final String DESCRIPTION = "description";
    private final String FINANCIAL_REFERENCE = "financialReference";
    private final String FINANCIAL_FUNDING_LABEL = "financialFundingLabel";
    private final String FINANCIAL_FUNDING_URI = "financialFundingURI";
    private final String HOME_PAGE = "homePage";
    private final String KEYWORD = "keyword";
    private final String NAME = "name";
    private final String OBJECTIVE = "objective";
    private final String RELATED_PROJECT_NAME = "relatedProjectName";
    private final String RELATED_PROJECT_URI = "relatedProjectURI";
    private final String SCIENTIFIC_CONTACT = "scientificContact";
    private final String SHORTNAME = "shortname";
    
    private UpdateRequest prepareInsertQuery(Project project) {
        UpdateBuilder spql = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.PROJECTS.toString());
        
        Resource projectURI = ResourceFactory.createResource(project.getUri());
        
        Node projectType = NodeFactory.createURI(Oeso.CONCEPT_PROJECT.toString());
        
        spql.addInsert(graph, projectURI, RDF.type, projectType);
        //foaf:name
        spql.addInsert(graph, projectURI, FOAF.name, project.getName());
        //oeso:shortname
        Property relationHasShortname = ResourceFactory.createProperty(Oeso.RELATION_HAS_SHORTNAME.toString());
        spql.addInsert(graph, projectURI, relationHasShortname, project.getShortname());
        
        //oeso:hasRelatedProject
        if (!project.getRelatedProjects().isEmpty()) {
            Property relationHasRelatedProject = ResourceFactory.createProperty(Oeso.RELATION_HAS_RELATED_PROJECT.toString());
            for (RdfResourceDefinition relatedProject : project.getRelatedProjects()) {
                Resource relatedProjectURI = ResourceFactory.createResource(relatedProject.getUri());
                spql.addInsert(graph, projectURI, relationHasRelatedProject, relatedProjectURI);
            }
        }
        //oeso:hasFinancialFunding
        if (project.getFinancialFunding() != null) {
            Property relationHasFinancialFunfing = ResourceFactory.createProperty(Oeso.RELATION_HAS_FINANCIAL_FUNDING.toString());
            Resource financialFunding = ResourceFactory.createResource(project.getFinancialFunding().getUri());
            spql.addInsert(graph, projectURI, relationHasFinancialFunfing, financialFunding);
        }
        //oeso:hasFinancialReference
        if (project.getFinancialReference() != null) {
            Property relationHasFinancialReference = ResourceFactory.createProperty(Oeso.RELATION_HAS_FINANCIAL_REFERENCE.toString());
            spql.addInsert(graph, projectURI, relationHasFinancialReference, project.getFinancialReference());
        }
        
        //dcterms:description
        if (project.getDescription() != null) {
            Property relationDescription = ResourceFactory.createProperty(DCTERMS.DESCRIPTION.toString());
            spql.addInsert(graph, projectURI, relationDescription, project.getDescription());
        }
        
        //oeso:startDate
        Property relationStartDate = ResourceFactory.createProperty(Oeso.RELATION_START_DATE.toString());
        spql.addInsert(graph, projectURI, relationStartDate, project.getStartDate());
        //oeso:endDate
        Property relationEndDate = ResourceFactory.createProperty(Oeso.RELATION_END_DATE.toString());
        spql.addInsert(graph, projectURI, relationEndDate, project.getEndDate());
        //oeso:hasKeyword
        if (!project.getKeywords().isEmpty()) {
            Property relationKeywords = ResourceFactory.createProperty(Oeso.RELATION_HAS_KEYWORD.toString());
            for (String keyword : project.getKeywords()) {
                spql.addInsert(graph, projectURI, relationKeywords, keyword);
            }
        }
        
        //foaf:homePage
        if (project.getHomePage() != null) {
            spql.addInsert(graph, projectURI, FOAF.homepage, project.getHomePage());
        }
        
        //oeso:hasAdministrativeContact
        if (!project.getAdministrativeContacts().isEmpty()) {
            Property relationIsAdministrativeContactOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_ADMINISTRATIVE_CONTACT.toString());
            for (Contact contact : project.getAdministrativeContacts()) {
                Resource administrativeContact = ResourceFactory.createResource(contact.getUri());
                spql.addInsert(graph, projectURI, relationIsAdministrativeContactOf, administrativeContact);
            }
        }
        //oeso:hasCoordinator
        if (!project.getCoordinators().isEmpty()) {
            Property relationIsCoordinatorOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_COORDINATOR.toString());
            for (Contact contact : project.getCoordinators()) {
                Resource coordinator = ResourceFactory.createResource(contact.getUri());
                spql.addInsert(graph, projectURI, relationIsCoordinatorOf, coordinator);
            }
        }
        //oeso:hasScientificContact
        if (!project.getScientificContacts().isEmpty()) {
            Property relationIsScientificContactOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_SCIENTIFIC_CONTACT.toString());
            for (Contact contact : project.getScientificContacts()) {
                Resource scientificContact = ResourceFactory.createResource(contact.getUri());
                spql.addInsert(graph, projectURI, relationIsScientificContactOf, scientificContact);
            }
        }
        //oeso:hasObjective
        if (project.getObjective() != null) {
            Property relationHasObjective = ResourceFactory.createProperty(Oeso.RELATION_HAS_OBJECTIVE.toString());
            spql.addInsert(graph, projectURI, relationHasObjective, project.getObjective());
        }
        
        UpdateRequest query = spql.buildRequest();
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + query.toString());
        
        return query;
    }

    @Override
    public List<Project> create(List<Project> projects) throws DAOPersistenceException, Exception {
        getConnection().begin();
        try {
            for (Project project : projects) {
                //Insert project
                UpdateRequest updateQuery = prepareInsertQuery(project);
                Update prepareUpdate = getConnection().prepareUpdate(QueryLanguage.SPARQL, updateQuery.toString());
                prepareUpdate.execute();
            }
            
            getConnection().commit();
            
            return projects;
        } catch (Exception ex) { //An error occurred, rollback
            getConnection().rollback();
            LOGGER.error("Error creation projects", ex);
            throw new Exception(ex); //Throw the exception to return the error.
        }
    }

    @Override
    public void delete(List<Project> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private UpdateRequest prepareDeleteQuery(Project project) {
        UpdateBuilder updateBuilder = new UpdateBuilder();
        
        Node graph = NodeFactory.createURI(Contexts.PROJECTS.toString());
        
        Resource projectURI = ResourceFactory.createResource(project.getUri());
        
        //name
        updateBuilder.addDelete(graph, projectURI, FOAF.name, project.getName());
        
        //shortname
        Property relationHasShortname = ResourceFactory.createProperty(Oeso.RELATION_HAS_SHORTNAME.toString());
        updateBuilder.addDelete(graph, projectURI, relationHasShortname, project.getShortname());
        
        //related projects
        Property relationHasRelatedProject = ResourceFactory.createProperty(Oeso.RELATION_HAS_RELATED_PROJECT.toString());
        for (RdfResourceDefinition relatedProject : project.getRelatedProjects()) {
            Resource relatedProjectResource = ResourceFactory.createResource(relatedProject.getUri());
            updateBuilder.addDelete(graph, projectURI, relationHasRelatedProject, relatedProjectResource);
        }
        
        //financial funding
        if (project.getFinancialFunding() != null) {
            Property relationHasFinancialFunding = ResourceFactory.createProperty(Oeso.RELATION_HAS_FINANCIAL_FUNDING.toString());
            Resource financialFunding = ResourceFactory.createResource(project.getFinancialFunding().getUri());
            updateBuilder.addDelete(graph, projectURI, relationHasFinancialFunding, financialFunding);
        }
        
        //financial reference
        if (project.getFinancialReference() != null) {
            Property relationHasFinancialReference = ResourceFactory.createProperty(Oeso.RELATION_HAS_FINANCIAL_REFERENCE.toString());
            updateBuilder.addDelete(graph, projectURI, relationHasFinancialReference, project.getFinancialReference());
        }
        
        //description
        if (project.getDescription() != null) {
            Property relationDescription = ResourceFactory.createProperty(DCTERMS.DESCRIPTION.toString());
            updateBuilder.addDelete(graph, projectURI, relationDescription, project.getDescription());
        }
        
        //startDate
        if (project.getStartDate() != null) {
            Property relationStartDate = ResourceFactory.createProperty(Oeso.RELATION_START_DATE.toString());
            updateBuilder.addDelete(graph, projectURI, relationStartDate, project.getStartDate());
        }
        
        //end date
        if (project.getEndDate() != null) {
            Property relationEndDate = ResourceFactory.createProperty(Oeso.RELATION_END_DATE.toString());
            updateBuilder.addDelete(graph, projectURI, relationEndDate, project.getEndDate());
        }
        
        //keywords
        Property relationHasKeyword = ResourceFactory.createProperty(Oeso.RELATION_HAS_KEYWORD.toString());
        for (String keyword : project.getKeywords()) {
            updateBuilder.addDelete(graph, projectURI, relationHasKeyword, keyword);
        }
        
        //home page
        if (project.getHomePage() != null) {
            updateBuilder.addDelete(graph, projectURI, FOAF.homepage, project.getHomePage());
        }
        
        //administrative contacts
        Property relationIsAdministrativeContactOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_ADMINISTRATIVE_CONTACT.toString());
        for (Contact administrativeContact : project.getAdministrativeContacts()) {
            Resource administrativeContactResource = ResourceFactory.createResource(administrativeContact.getUri());
            updateBuilder.addDelete(graph, projectURI, relationIsAdministrativeContactOf, administrativeContactResource);
        }
        
        //coordinators
        Property relationIsCoordinatorOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_COORDINATOR.toString());
        for (Contact coordinator : project.getCoordinators()) {
            Resource coordinatorResource = ResourceFactory.createResource(coordinator.getUri());
            updateBuilder.addDelete(graph, projectURI, relationIsCoordinatorOf, coordinatorResource);
        }
        
        // scientific contacts
        Property relationIsScientificContactOf = ResourceFactory.createProperty(Oeso.RELATION_HAS_SCIENTIFIC_CONTACT.toString());
        for (Contact contact : project.getScientificContacts()) {
            Resource scientificContactResource = ResourceFactory.createResource(contact.getUri());
            updateBuilder.addDelete(graph, projectURI, relationIsScientificContactOf, scientificContactResource);
        }
        
        //objective
        if (project.getObjective() != null) {
            Property relationHasObjective = ResourceFactory.createProperty(Oeso.RELATION_HAS_OBJECTIVE.toString());
            updateBuilder.addDelete(graph, projectURI, relationHasObjective, project.getObjective());
        }
        
        UpdateRequest query = updateBuilder.buildRequest();
        
        LOGGER.debug(getTraceabilityLogs() + SPARQL_QUERY + query.toString());
        return query;
    }

    @Override
    public List<Project> update(List<Project> projects) throws DAOPersistenceException, Exception {
        getConnection().begin();
        try {
            for (Project project : projects) {
                //1. Get old project data
                Project oldProject = findById(project.getUri());
                
                //2. Delete old project data
                UpdateRequest deleteQuery = prepareDeleteQuery(oldProject);
                Update prepareDelete = getConnection().prepareUpdate(deleteQuery.toString());
                prepareDelete.execute();
                
                //2. Insert new project data
                UpdateRequest insertQuery = prepareInsertQuery(project);
                Update prepareUpdate = getConnection().prepareUpdate(insertQuery.toString());
                prepareUpdate.execute();
            }
            
            getConnection().commit();
            
            return projects;
        } catch (RepositoryException ex) { //An error occurred, rollback
            getConnection().rollback();
            LOGGER.error("Error update projects", ex);
            throw new RepositoryException(ex); //Throw the exception to return the error.
        } catch (NotFoundException ex) { //A project was not found.
            getConnection().rollback();
            LOGGER.error("Error update projects", ex);
            throw new NotFoundException(ex); //Throw the exception to return the error.
        }
    }

    @Override
    public Project find(Project object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private Project getProjectFromBindingSet(BindingSet bindingSet) {
        Project project = new Project();
        if (bindingSet.getValue(URI) != null) {
            project.setUri(bindingSet.getValue(URI).stringValue());
        }
        
        if (bindingSet.getValue(NAME) != null) {
            project.setName(bindingSet.getValue(NAME).stringValue());
        }
        
        if (bindingSet.getValue(SHORTNAME) != null) {
            project.setShortname(bindingSet.getValue(SHORTNAME).stringValue());
        }
        
        if (bindingSet.getValue(FINANCIAL_FUNDING_URI) != null) {
            RdfResourceDefinition financialFunding = new RdfResourceDefinition();
            financialFunding.setUri(bindingSet.getValue(FINANCIAL_FUNDING_URI).stringValue());
            project.setFinancialFunding(financialFunding);
        }
        
        if (bindingSet.getValue(FINANCIAL_REFERENCE) != null) {
            project.setFinancialReference(bindingSet.getValue(FINANCIAL_REFERENCE).stringValue());
        }
        
        if (bindingSet.getValue(DESCRIPTION) != null) {
            project.setDescription(bindingSet.getValue(DESCRIPTION).stringValue());
        }
        
        if (bindingSet.getValue(DATE_START) != null) {
            project.setStartDate(bindingSet.getValue(DATE_START).stringValue());
        }
        
        if (bindingSet.getValue(DATE_END) != null) {
            project.setEndDate(bindingSet.getValue(DATE_END).stringValue());
        }
        
        if (bindingSet.getValue(HOME_PAGE) != null) {
            project.setHomePage(bindingSet.getValue(HOME_PAGE).stringValue());
        }
        
        if (bindingSet.getValue(OBJECTIVE) != null) {
            project.setObjective(bindingSet.getValue(OBJECTIVE).stringValue());
        }
        
        return project;
    }
    
    /**
     * Get the label of a financial funding
     * /!\ in this case, the financial funding is supposed to exist.
     * @param uri
     * @return the financial funding with the label
     */
    public RdfResourceDefinition getFinancialFunding(String uri) {
        RdfResourceDefinition financialFunding = new RdfResourceDefinition(uri);
        SPARQLQueryBuilder queryFinancialFunding = prepareGetFinancialFundingLabel(uri);  
        TupleQuery tupleQueryFinancialFunding = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryFinancialFunding.toString());
        try (TupleQueryResult result = tupleQueryFinancialFunding.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSetFinancialFunding = result.next();
                
                financialFunding.setLabel(bindingSetFinancialFunding.getValue(FINANCIAL_FUNDING_LABEL).stringValue());
            }
        }
        
        return financialFunding;
    }
    
    /**
     * Generates the query to get the labels of a given financial funding.
     * @param uri
     * @return the query
     * @example
     * SELECT ?label
     * WHERE {
     *      <http://www.opensilex.org/vocabulary/oeso#anrsupport> rdfs:label ?label
     * }
     */
    protected SPARQLQueryBuilder prepareGetFinancialFundingLabel(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        query.appendSelect("?" + FINANCIAL_FUNDING_LABEL);
        query.appendTriplet(uri, Rdfs.RELATION_LABEL.toString(), "?" + FINANCIAL_FUNDING_LABEL, null);        
        
        return query;
    }
    
    /**
     * Get projects filtered by the given search params.
     * @param page
     * @param pageSize
     * @param uri
     * @param name
     * @param shortname
     * @param financialfunding
     * @param financialReference
     * @param description
     * @param startDate
     * @param endDate
     * @param homePage
     * @param objective
     * @return the list of projects that matches the filter parameters.
     */
    public ArrayList<Project> find(Integer page, Integer pageSize, String uri, String name, String shortname, String financialfunding, 
            String financialReference, String description, String startDate, String endDate, String homePage, String objective) {
        
        SPARQLQueryBuilder query = prepareSearchQuery(page, pageSize, uri, name, shortname, financialfunding, financialReference, description, startDate, endDate, homePage, objective);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        ArrayList<Project> projects = new ArrayList<>();
        Map<String, RdfResourceDefinition>  foundedFinancialFunding = new HashMap<>();

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                Project project = getProjectFromBindingSet(bindingSet);
                
                //Get financial funding
                if (project.getFinancialFunding() != null ) {
                    if (foundedFinancialFunding.containsKey(project.getFinancialFunding().getUri())) {
                        project.setFinancialFunding(foundedFinancialFunding.get(project.getFinancialFunding().getUri()));
                    } else { //The financial funding have not been met before so we get it.
                        RdfResourceDefinition financialFunding = getFinancialFunding(project.getFinancialFunding().getUri());
                        project.setFinancialFunding(financialFunding);
                        foundedFinancialFunding.put(financialFunding.getUri(), financialFunding);
                    }
                }
                
                projects.add(project);
            }
        }
        return projects;
    }
    
    protected SPARQLQueryBuilder prepareSearchByURI(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        //name
        query.appendSelect(" ?" + NAME);
        query.appendTriplet(uri, Foaf.RELATION_NAME.toString(), "?" + NAME, null);
        
        //shortname
        query.appendSelect(" ?" + SHORTNAME);
        query.appendTriplet(uri, Oeso.RELATION_HAS_SHORTNAME.toString(), "?" + SHORTNAME, null);
        
        //date start
        query.appendSelect(" ?" + DATE_START);
        query.appendTriplet(uri, Oeso.RELATION_START_DATE.toString(), "?" + DATE_START, null);
        
        //date end
        query.appendSelect(" ?" + DATE_END);
        query.appendTriplet(uri, Oeso.RELATION_END_DATE.toString(), "?" + DATE_END, null);
        
        //objective
        query.appendSelect(" ?" + OBJECTIVE);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + "> <" + Oeso.RELATION_HAS_OBJECTIVE.toString() + "> ?" + OBJECTIVE);
        query.endBodyOptional();
        
        //description
        query.appendSelect(" ?" + DESCRIPTION);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + "> <" + DCTERMS.DESCRIPTION.toString() + "> ?" + DESCRIPTION);
        query.endBodyOptional();
        
        //coordinators
        query.appendSelect(" ?" + COORDINATOR);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + ">" + " <" + Oeso.RELATION_HAS_COORDINATOR.toString() + "> ?" + COORDINATOR);
        query.endBodyOptional();
        
        //scientific contacts
        query.appendSelect(" ?" + SCIENTIFIC_CONTACT);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + ">" + " <" + Oeso.RELATION_HAS_SCIENTIFIC_CONTACT.toString() + "> ?" + SCIENTIFIC_CONTACT);
        query.endBodyOptional();
        
        //administrative contacts
        query.appendSelect(" ?" + ADMINISTRATIVE_CONTACT);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + ">" + " <" + Oeso.RELATION_HAS_ADMINISTRATIVE_CONTACT.toString() + "> ?" + ADMINISTRATIVE_CONTACT);
        query.endBodyOptional();
        
        //related projects
        query.appendSelect(" ?" + RELATED_PROJECT_URI + " ?" + RELATED_PROJECT_NAME);
        query.beginBodyOptional();
        //SILEX:info 
        //Get the relation and its inverse.
        //\SILEX:info
        query.appendToBody("<" + uri + "> <" + Oeso.RELATION_HAS_RELATED_PROJECT.toString() + ">/^<" + Oeso.RELATION_HAS_RELATED_PROJECT.toString() + "> ?" + RELATED_PROJECT_URI + " .");
        query.appendToBody(" ?" + RELATED_PROJECT_URI + " <" + Foaf.RELATION_NAME.toString() + "> ?" + RELATED_PROJECT_NAME);
        query.endBodyOptional();
        
        //financial funding
        query.appendSelect(" ?" + FINANCIAL_FUNDING_URI + " ?" + FINANCIAL_FUNDING_LABEL);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + "> <" + Oeso.RELATION_HAS_FINANCIAL_FUNDING.toString() + "> ?" + FINANCIAL_FUNDING_URI + " . ");
        query.appendToBody("?" + FINANCIAL_FUNDING_URI + " <" + Rdfs.RELATION_LABEL.toString() + "> ?" + FINANCIAL_FUNDING_LABEL);
        query.endBodyOptional();
        
        //financial reference
        query.appendSelect(" ?" + FINANCIAL_REFERENCE);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + "> <" + Oeso.RELATION_HAS_FINANCIAL_REFERENCE.toString() + "> ?" + FINANCIAL_REFERENCE);
        query.endBodyOptional();
        
        //keywords
        query.appendSelect(" ?" + KEYWORD);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + "> <" + Oeso.RELATION_HAS_KEYWORD.toString() + "> ?" + KEYWORD);
        query.endBodyOptional();
        
        //homePage
        query.appendSelect(" ?" + HOME_PAGE);
        query.beginBodyOptional();
        query.appendToBody("<" + uri + "> <" + FOAF.homepage.toString() + "> ?" + HOME_PAGE);
        query.endBodyOptional();
        
        LOGGER.debug(query.toString());
        return query;
    } 

    @Override
    public Project findById(String id) throws DAOPersistenceException, Exception {
        
        SPARQLQueryBuilder findQuery = prepareSearchByURI(id);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, findQuery.toString());
        
        Project project = new Project();
        try(TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                project.setUri(id);
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    
                    if (project.getName() == null) {
                        project.setName(bindingSet.getValue(NAME).stringValue());
                    }
                    
                    if (project.getShortname() == null) {
                        project.setShortname(bindingSet.getValue(SHORTNAME).stringValue());
                    }
                    
                    //related projects
                    if (bindingSet.getValue(RELATED_PROJECT_URI) != null) {
                        if (!project.containsRelatedProject(bindingSet.getValue(RELATED_PROJECT_URI).stringValue())) {
                            RdfResourceDefinition relatedProject = new RdfResourceDefinition(bindingSet.getValue(RELATED_PROJECT_URI).stringValue());
                            relatedProject.setLabel(bindingSet.getValue(RELATED_PROJECT_NAME).stringValue());
                            
                            project.addRelatedProject(relatedProject);
                        }                        
                    }
                    
                    //financial funding
                    if (project.getFinancialFunding() == null && bindingSet.getValue(FINANCIAL_FUNDING_URI) != null) {
                        RdfResourceDefinition financialFunding = new RdfResourceDefinition(bindingSet.getValue(FINANCIAL_FUNDING_URI).stringValue());
                        financialFunding.setLabel(bindingSet.getValue(FINANCIAL_FUNDING_LABEL).stringValue());
                        
                        project.setFinancialFunding(financialFunding);
                    }
                    
                    //financial reference
                    if (project.getFinancialReference() == null && bindingSet.getValue(FINANCIAL_REFERENCE) != null) {
                        project.setFinancialReference(bindingSet.getValue(FINANCIAL_REFERENCE).stringValue());
                    }
                    
                    //description
                    if (project.getDescription() == null && bindingSet.getValue(DESCRIPTION) != null) {
                        project.setDescription(bindingSet.getValue(DESCRIPTION).stringValue());
                    }
                    
                    //start date
                    if (project.getStartDate() == null) {
                        project.setStartDate(bindingSet.getValue(DATE_START).stringValue());
                    }
                    
                    //end date
                    if (project.getEndDate() == null) {
                        project.setEndDate(bindingSet.getValue(DATE_END).stringValue());
                    }
                    
                    //keywords
                    if (bindingSet.getValue(KEYWORD) != null) {
                        if (!project.getKeywords().contains(bindingSet.getValue(KEYWORD).stringValue())) {
                            project.addKeyword(bindingSet.getValue(KEYWORD).stringValue());
                        }
                    }
                    
                    //home page
                    if (project.getHomePage() == null && bindingSet.getValue(HOME_PAGE) != null) {
                        project.setHomePage(bindingSet.getValue(HOME_PAGE).stringValue());
                    }
                    
                    //administrative contacts
                    if (bindingSet.getValue(ADMINISTRATIVE_CONTACT) != null) {
                        if (!project.containsAdministrativeContact(bindingSet.getValue(ADMINISTRATIVE_CONTACT).stringValue())) {
                            UserDAO userDAO = new UserDAO();
                            User userFounded = userDAO.findById(bindingSet.getValue(ADMINISTRATIVE_CONTACT).stringValue());
                            
                            Contact administrativeContact = new Contact();
                            administrativeContact.setUri(bindingSet.getValue(ADMINISTRATIVE_CONTACT).stringValue()); 
                            administrativeContact.setEmail(userFounded.getEmail());
                            administrativeContact.setFirstname(userFounded.getFirstName());
                            administrativeContact.setLastname(userFounded.getFamilyName());
                            
                            project.addAdministrativeContact(administrativeContact);
                        }
                    }
                    
                    //coordinators
                    if (bindingSet.getValue(COORDINATOR) != null) {
                        if (!project.containsCoordinator(bindingSet.getValue(COORDINATOR).stringValue())) {
                            UserDAO userDAO = new UserDAO();
                            User userFounded = userDAO.findById(bindingSet.getValue(COORDINATOR).stringValue());
                            Contact coordinator = new Contact();
                            coordinator.setUri(bindingSet.getValue(COORDINATOR).stringValue());
                            coordinator.setEmail(userFounded.getEmail());
                            coordinator.setFirstname(userFounded.getFirstName());
                            coordinator.setLastname(userFounded.getFamilyName());
                            
                            project.addCoordinator(coordinator);
                        }
                    }
                    
                    //scientific contacts
                    if (bindingSet.getValue(SCIENTIFIC_CONTACT) != null) {
                        if (!project.containsScientificContact(bindingSet.getValue(SCIENTIFIC_CONTACT).stringValue())) {
                            UserDAO userDAO = new UserDAO();
                            User userFounded = userDAO.findById(bindingSet.getValue(SCIENTIFIC_CONTACT).stringValue());
                            
                            Contact scientificContact = new Contact();
                            scientificContact.setUri(bindingSet.getValue(SCIENTIFIC_CONTACT).stringValue()); 
                            scientificContact.setEmail(userFounded.getEmail());
                            scientificContact.setFirstname(userFounded.getFirstName());
                            scientificContact.setLastname(userFounded.getFamilyName());
                            
                            project.addScientificContact(scientificContact);
                        }
                    }
                    
                    //objective
                    if (project.getObjective() == null && bindingSet.getValue(OBJECTIVE) != null) {
                        project.setObjective(bindingSet.getValue(OBJECTIVE).stringValue());
                    }
                }
            } else {
                throw new NotFoundException(id + " not found.");
            }
        }
        return project;
    }

    @Override
    public void validate(List<Project> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Check if the given projects respects the rules.
     * @param projects
     * @return the check result.
     * @throws Exception 
     */
    private POSTResultsReturn check(List<Project> projects, boolean update) throws Exception {
        List<Status> checkStatus = new ArrayList<>();
        boolean dataOk = true;
        
        //Caches
        List<String> relatedProjectsCache = new ArrayList<>();
        List<String> contactsCache = new ArrayList<>();
        List<String> financialFundingCache = new ArrayList<>();
        UserDAO userDAO = new UserDAO();
        
        for (Project project : projects) {        
            //1. If it is an update of an already existing project, check if the given project URI exist.
            if (update && !existUri(project.getUri())) {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknwon project URI : " + project.getUri()));
            //1. If it is a creation of project, check if the project URI does not exist.
            } else if (!update && existUri(project.getUri())) {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Already existing project : " + project.getUri()));
            }

            //2. Check if the related projects exist.
            for (RdfResourceDefinition relatedProject : project.getRelatedProjects()) {
                if (!relatedProjectsCache.contains(relatedProject.getUri())) {
                    if (!existUri(relatedProject.getUri())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown related project: " + relatedProject.getUri()));
                    } else {
                        relatedProjectsCache.add(relatedProject.getUri());
                    }
                }
            }

            //3. Check if the given financial funding exist.
            if (project.getFinancialFunding() != null 
                    && !financialFundingCache.contains(project.getFinancialFunding().getUri())) {
                if (!existUri(project.getFinancialFunding().getUri())) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown financial funding: " + project.getFinancialFunding().getUri()));
                } else {
                    financialFundingCache.add(project.getFinancialFunding().getUri());
                }
            }

            //4. Check if the administrative contacts exist
            for (Contact contact : project.getAdministrativeContacts()) {
                if (!contactsCache.contains(contact.getUri())) {
                    if (!userDAO.existUserUri(contact.getUri())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown administrative contact: " + contact.getUri()));
                    } else {
                        contactsCache.add(contact.getUri());
                    }
                }
            }
            //5. Check if the coordinators exist
            for (Contact contact : project.getCoordinators()) {
                if (!contactsCache.contains(contact.getUri())) {
                    if (!userDAO.existUserUri(contact.getUri())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown coordinator: " + contact.getUri()));
                    } else {
                        contactsCache.add(contact.getUri());
                    }
                }
            }

            //6. Check if the scientific contacts exist
            for (Contact contact : project.getScientificContacts()) {
                if (!contactsCache.contains(contact.getUri())) {
                    if (!userDAO.existUserUri(contact.getUri())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown scientific contact: " + contact.getUri()));
                    } else {
                        contactsCache.add(contact.getUri());
                    }
                }
            }
        }
        
        POSTResultsReturn check = new POSTResultsReturn(dataOk, null, dataOk);
        check.statusList = checkStatus;
        return check;
    }
    
    public List<Project> getProjectsWithGeneratedURIs(List<Project> projects) throws Exception {
        for (Project project : projects) {
            project.setUri(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_PROJECT.toString(), null, project.getShortname()));
        }
        
        return projects;
    }
    
    /**
     * Check and insert the project in the triplestore.
     * @param projects
     * @return the result of the insertion.
     */
    public POSTResultsReturn checkAndInsert(List<Project> projects) {
        POSTResultsReturn insertResult;
        try {
            //1. Generates URIs for the projects
            projects = getProjectsWithGeneratedURIs(projects);
            //1. Check the given projects
            insertResult = check(projects, false);
            //2. No error founded, insert projects.
            if (insertResult.statusList.isEmpty()) {
            
                List<Project> insertedProject = create(projects);
                
                //The new projects are inserted. Get the list of uri of the projects.
                List<String> insertedProjectsURIs = new ArrayList<>();
                for (Project actuator : insertedProject) {
                    insertedProjectsURIs.add(actuator.getUri());
                }
                
                insertResult = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
                insertResult.statusList = new ArrayList<>();
                insertResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, StatusCodeMsg.DATA_INSERTED));
                insertResult.createdResources = insertedProjectsURIs;
            
            } 
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            insertResult = new POSTResultsReturn(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
            insertResult.statusList.add(new Status(ex.getMessage(), StatusCodeMsg.ERR, ex.getClass().toString()));
        }
        return insertResult;
    }
    
    public POSTResultsReturn checkAndUpdate(List<Project> projects) {
        POSTResultsReturn updateResult;
        try {
            //1. Check the given projects
            updateResult = check(projects, true);
            //2. No error founded, insert projects.
            if (updateResult.statusList.isEmpty()) {
            
                List<Project> updatedProjects = update(projects);
                
                //The new projects are inserted. Get the list of uri of the projects.
                List<String> updatedProjectsURIs = new ArrayList<>();
                for (Project actuator : updatedProjects) {
                    updatedProjectsURIs.add(actuator.getUri());
                }
                
                updateResult = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
                updateResult.statusList = new ArrayList<>();
                updateResult.statusList.add(new Status(StatusCodeMsg.RESOURCES_UPDATED, StatusCodeMsg.INFO, StatusCodeMsg.RESOURCES_UPDATED));
                updateResult.createdResources = updatedProjectsURIs;
            
            } 
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            updateResult = new POSTResultsReturn(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
            updateResult.statusList.add(new Status(ex.getMessage(), StatusCodeMsg.ERR, ex.getClass().toString()));
        }
        return updateResult;
    }
    
    protected SPARQLQueryBuilder prepareSearchQuery(Integer page, Integer pageSize, String uri, String name, String shortname, 
            String financialFunding, String financialReference, String description, String startDate, String endDate, String homePage, String objective) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        
        //uri filter
        query.appendSelect("?" + URI);
        if (uri != null) {
            query.appendAndFilter("REGEX (str(?" + URI + "), \".*" + uri + ".*\",\"i\")");
        }
        query.appendTriplet("?" + URI, Rdf.RELATION_TYPE.toString(), "?" + RDF_TYPE, null);
        query.appendTriplet("?" + RDF_TYPE, "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", Oeso.CONCEPT_PROJECT.toString(), null);
        
        //name filter
        query.appendSelect("?" + NAME);
        if (name == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Foaf.RELATION_NAME.toString() + "> " + "?" + NAME + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Foaf.RELATION_NAME.toString(), "?" + NAME, null);
            query.appendAndFilter("REGEX ( str(?" + NAME + "),\".*" + name + ".*\",\"i\")");
        }
        
        //shortname filter
        query.appendSelect("?" + SHORTNAME);
        if (shortname == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + "<" + Oeso.RELATION_HAS_SHORTNAME.toString() + "> " + "?" + SHORTNAME + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_SHORTNAME.toString(), "?" + SHORTNAME, null);
            query.appendAndFilter("REGEX ( str(?" + SHORTNAME + "),\".*" + shortname + ".*\",\"i\")");
        }
        
        //financialFunding filter
        query.appendSelect("?" + FINANCIAL_FUNDING_URI + " ?" + FINANCIAL_FUNDING_LABEL);
        if (financialFunding == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_FINANCIAL_FUNDING.toString() + "> " + "?" + FINANCIAL_FUNDING_URI + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_FINANCIAL_FUNDING.toString(), "?" + FINANCIAL_FUNDING_URI, null);
        }
        
        //financialReference filter
        query.appendSelect("?" + FINANCIAL_REFERENCE);
        if (financialReference == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_FINANCIAL_REFERENCE.toString() + "> " + "?" + FINANCIAL_REFERENCE + " . ");
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_FINANCIAL_REFERENCE.toString(), "?" + FINANCIAL_REFERENCE, null);
            query.appendAndFilter("REGEX ( str(?" + FINANCIAL_REFERENCE + "),\".*" + financialReference + ".*\",\"i\")");
        }
        
        //description filter
        query.appendSelect("?" + DESCRIPTION);
        if (description == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + DCTERMS.DESCRIPTION.toString() + "> ?" + DESCRIPTION);
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, DCTERMS.DESCRIPTION.toString(), "?" + DESCRIPTION, null);
            query.appendAndFilter("REGEX ( str(?" + DESCRIPTION + "),\".*" + description + ".*\",\"i\")");
        }
        
        //startDate filter
        query.appendSelect("?" + DATE_START);
        if (startDate == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_START_DATE.toString() + "> ?" + DATE_START);
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_START_DATE.toString(), "?" + DATE_START, null);
            query.appendAndFilter("REGEX ( str(?" + DATE_START + "),\".*" + startDate + ".*\",\"i\")");
        }
        
        //endDate filter
        query.appendSelect("?" + DATE_END);
        if (endDate == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_END_DATE.toString() + "> ?" + DATE_END);
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_END_DATE.toString(), "?" + DATE_END, null);
            query.appendAndFilter("REGEX ( str(?" + DATE_END + "),\".*" + endDate + ".*\",\"i\")");
        }
        
        //homePage filter
        query.appendSelect("?" + HOME_PAGE);
        if (homePage == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + FOAF.homepage.toString() + "> ?" + HOME_PAGE);
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI,  FOAF.homepage.toString(), "?" + HOME_PAGE, null);
            query.appendAndFilter("REGEX ( str(?" + HOME_PAGE + "),\".*" + homePage + ".*\",\"i\")");
        }
        
        //objective filter
        query.appendSelect("?" + OBJECTIVE);
        if (objective == null) {
            query.beginBodyOptional();
            query.appendToBody("?" + URI + " <" + Oeso.RELATION_HAS_OBJECTIVE.toString() + "> ?" + OBJECTIVE);
            query.endBodyOptional();
        } else {
            query.appendTriplet("?" + URI, Oeso.RELATION_HAS_OBJECTIVE.toString(), "?" + OBJECTIVE, null);
            query.appendAndFilter("REGEX ( str(?" + OBJECTIVE + "),\".*" + objective + ".*\",\"i\")");
        }
        
        LOGGER.debug(query.toString());
        return query;
    }
    
    public SPARQLQueryBuilder prepareCount(String uri, String name, String shortname, String financialFunding, String financialReference, 
            String description, String startDate, String endDate, String homePage, String objective) {
        SPARQLQueryBuilder query = this.prepareSearchQuery(null, null, uri, name, shortname, financialFunding, financialReference, description, startDate, endDate, homePage, objective);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    public Integer count(String uri, String name, String shortname, String financialFunding, String financialReference, 
            String description, String startDate, String endDate, String homePage, String objective) {
        SPARQLQueryBuilder prepareCount = prepareCount(uri, name, shortname, financialFunding, financialReference, description, startDate, endDate, homePage, objective);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        return count;
    }
    
    /**
     * Get the shortname of a project.
     * @param uri the URI of the project.
     * @example
     * SELECT ?shortname
     * WHERE {
     *      <http://www.opensilex.org/demo/set/projects/DROPS> oeso:hasShortname ?shortname
     * }
     * @return the shortname, null if no shortname founded.
     */
    public String getShortnameFromURI(String uri) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        query.appendDistinct(Boolean.TRUE);
        query.appendTriplet(uri, Oeso.RELATION_HAS_SHORTNAME.toString(), "?" + SHORTNAME, null);
        
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
        
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                return bindingSet.getValue(SHORTNAME).stringValue();
            }
        }
        
        return null;
    }
}
