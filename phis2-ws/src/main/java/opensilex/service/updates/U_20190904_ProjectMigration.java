/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.updates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import opensilex.service.dao.ProjectDAO;
import opensilex.service.datasource.PostgreSQLDataSource;
import opensilex.service.model.Contact;
import opensilex.service.model.FinancialFunding;
import opensilex.service.model.Project;
import opensilex.service.model.User;
import opensilex.service.ontology.Foaf;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Rdfs;
import opensilex.service.utils.ResourcesUtils;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.opensilex.module.ModuleUpdate;

/**
 *
 * @author vincent
 */
public class U_20190904_ProjectMigration implements ModuleUpdate {

    @Override
    public LocalDateTime getDate() {
        return LocalDateTime.of(2019, Month.SEPTEMBER, 04, 0, 0, 0);
    }

    @Override
    public String getDescription() {
        return "This update move all projects for postgresql to rdf4j";
    }

    @Override
    public void execute() throws Exception {
        String sqlQuery = "SELECT * FROM project";
        Connection connection = PostgreSQLDataSource.getInstanceConnection();
        Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        ResultSet results = statement.executeQuery(sqlQuery);

        Map<String, Project> projectsToInsert = new HashMap<>();
        Map<Project, String> projectsWithParentToUpdate = new HashMap<>();

        while (results.next()) {
            Project p = new Project();

            // SIMPLE DATA
            String uri = results.getString("uri");
            String name = results.getString("name");
            String acronyme = results.getString("acronyme");
            String financial_name = results.getString("financial_name");
            String date_start = results.getString("date_start");
            String date_end = results.getString("date_end");
            String keywords = results.getString("keywords");
            String description = results.getString("description");
            String objective = results.getString("objective");
            String website = results.getString("website");

            p.setUri(uri);
            p.setName(name);
            p.setShortname(acronyme);
            p.setFinancialReference(financial_name);
            p.setStartDate(date_start);
            p.setEndDate(date_end);
            List<String> keywordsList = Arrays.asList(keywords.split("\\s*\\W+\\s+"));
            List<String> cleanKeywordsList = new ArrayList<>();
            for (String keyword : keywordsList) {
                if (!keyword.trim().equals("")) {
                    cleanKeywordsList.add(keyword.trim());
                }
            }
            p.setKeywords(cleanKeywordsList);
            p.setDescription(description);
            p.setObjective(objective);
            p.setHomePage(website);

            // IGNORED DATA
            String subproject_type = results.getString("subproject_type");
            String type = results.getString("type");

            // COMPLEX DATA
            String financial_support = results.getString("financial_support");
            if (financial_support.equals("Unit")) {
                financial_support = "Research Unit";
            }

            String financial_support_uri = findFinancialSupport(financial_support);
            if (financial_support_uri == null) {
                financial_support_uri = createFinancialSupport(financial_support);
            }
            FinancialFunding ff = new FinancialFunding();
            ff.setUri(financial_support_uri);
            ff.setLabel(financial_support);
            p.setFinancialFunding(ff);

            String parent_project = results.getString("parent_project");
            p.setRelatedProjects(new ArrayList<>());
            if (parent_project != null && !parent_project.equals("")) {
                projectsWithParentToUpdate.put(p, parent_project);
            } else {
                projectsToInsert.put(p.getUri(), p);
            }
        }

        try {
            while (defineParentProjects(projectsWithParentToUpdate, projectsToInsert)) {
            }
        } catch (Exception ex) {
            // List all missing parents
            throw ex;
        }

        Map<String, String> uriByEmail = new HashMap<>();
        String sqlUserListQuery = "SELECT email, uri, first_name, family_name FROM users";
        Statement statementUserList = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        ResultSet resultsUserList = statementUserList.executeQuery(sqlUserListQuery);
        while (resultsUserList.next()) {
            String userURI = resultsUserList.getString("uri");
            String userMail = resultsUserList.getString("email");
            if (userURI == null) {
                User u = new User(userURI);
                u.setUri(userURI);
                // create uri suffix
                String userUriSuffix = ResourcesUtils.createUserUriSuffix(resultsUserList.getString("first_name"), resultsUserList.getString("family_name"));
                // set uri to agent
                userURI = UriGenerator.generateNewInstanceUri(Foaf.CONCEPT_AGENT.toString(), null, userUriSuffix);

                PreparedStatement st = connection.prepareStatement("UPDATE users SET uri=? WHERE email=?");
                st.setString(1, userURI);
                st.setString(2, userMail);
                st.execute();
                connection.commit();
            }
            uriByEmail.put(userMail, userURI);
        }

        String sqlUserQuery = "SELECT * FROM at_project_users";
        Statement statementUsers = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
        ResultSet resultsUsers = statementUsers.executeQuery(sqlUserQuery);
        while (resultsUsers.next()) {
            String pUri = resultsUsers.getString("project_uri");
            String uEmail = resultsUsers.getString("users_email");
            String type = resultsUsers.getString("type");

            Project p = projectsToInsert.get(pUri);
            Contact contact = new Contact();
            contact.setEmail(uEmail);
            if (!uriByEmail.containsKey(uEmail)) {
                throw new Exception("Unknown user : " + uEmail);
            }

            contact.setUri(uriByEmail.get(uEmail));
            if (type.equals("http://www.opensilex.org/vocabulary/oeso/#ProjectCoordinator")) {
                p.addCoordinator(contact);
            } else if (type.equals("http://www.opensilex.org/vocabulary/oeso/#ScientificContact")) {
                p.addScientificContact(contact);
            } else if (type.equals("http://www.opensilex.org/vocabulary/oeso/#AdministrativeContact")) {
                p.addAdministrativeContact(contact);
            } else {
                throw new Exception("Unknown project user type: " + type);
            }
        }

        // TODO insert all projects in projectsToInsert
        ProjectDAO dao = new ProjectDAO();
        dao.create(new ArrayList<>(projectsToInsert.values()));
    }

    private boolean defineParentProjects(Map<Project, String> projectsWithParentToUpdate, Map<String, Project> projectsReady) throws Exception {
        int remainingProjects = projectsWithParentToUpdate.size();

        if (remainingProjects == 0) {
            return false;
        }

        List<Project> solvedParentProjects = new ArrayList<>();

        for (Project p : projectsWithParentToUpdate.keySet()) {
            String parentURI = projectsWithParentToUpdate.get(p);

            for (Project candidateParent : projectsReady.values()) {
                if (candidateParent.getUri().toString().equals(parentURI)) {
                    p.getRelatedProjects().add(candidateParent);
                    solvedParentProjects.add(p);
                    break;
                }
            }
        }

        for (Project p : solvedParentProjects) {
            projectsReady.put(p.getUri(), p);
        }

        for (Project solvedProject : solvedParentProjects) {
            projectsWithParentToUpdate.remove(solvedProject);
        }

        if (projectsWithParentToUpdate.size() == 0) {
            return false;
        }

        if (remainingProjects == projectsWithParentToUpdate.size()) {
            throw new Exception("No parent can be found for " + remainingProjects + " project(s)");
        }

        return true;
    }

    private String findFinancialSupport(String financialSupportLabel) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();

        //uri filter
        query.appendSelect("?uri");
        query.appendTriplet("?uri", Rdf.RELATION_TYPE.toString(), "?type", null);
        query.appendTriplet("?type", "<" + Rdfs.RELATION_SUBCLASS_OF.toString() + ">*", "http://www.opensilex.org/vocabulary/oeso#FinancialFunding", null);
        query.appendTriplet("?uri", "<" + Rdfs.RELATION_LABEL.toString() + ">", "?label", null);
        query.appendFilter("str(?label)=\"" + financialSupportLabel + "\"");

        ProjectDAO dao = new ProjectDAO();
        TupleQuery tupleQuery = dao.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

        String supportUri = null;
        try (TupleQueryResult result = tupleQuery.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                supportUri = bindingSet.getValue("uri").stringValue();
            }
        }

        return supportUri;
    }

    private String createFinancialSupport(String financial_support) {
        UpdateBuilder spql = new UpdateBuilder();

        Node graph = NodeFactory.createURI("http://www.opensilex.org/vocabulary/oeso");

        String financialSupportURI = "http://www.opensilex.org/vocabulary/oeso#" + financial_support.toLowerCase();
        Resource financialSupportResource = ResourceFactory.createResource(financialSupportURI);

        Node projectType = NodeFactory.createURI("http://www.opensilex.org/vocabulary/oeso#FinancialFunding");

        spql.addInsert(graph, financialSupportResource, RDF.type, projectType);
        spql.addInsert(graph, financialSupportResource, "<" + RDFS.LABEL.toString() + ">", financial_support);

        UpdateRequest query = spql.buildRequest();

        ProjectDAO dao = new ProjectDAO();
        Update prepareUpdate = dao.getConnection().prepareUpdate(QueryLanguage.SPARQL, query.toString());
        prepareUpdate.execute();

        return financialSupportURI;
    }

}
