//******************************************************************************
//                          ExperimentSearch.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;


/**
 * @author Renaud COLIN
 */
public class ExperimentSearch {

    protected URI uri;

    protected String label;

    protected List<URI> projects = new LinkedList<>();

    protected String startDate;

    protected String endDate;

    protected String objective;

    protected Integer campaign;

    protected List<String> keywords = new LinkedList<>();

    protected List<URI> groups = new LinkedList<>();

    protected List<URI> infrastructures = new LinkedList<>();

    protected List<URI> installations = new LinkedList<>();

    protected URI species;

    protected Boolean isPublic;

    protected List<URI> variables = new LinkedList<>();

    protected List<URI> sensors = new LinkedList<>();

    private Boolean ended;

    private boolean admin = false;


    public URI getUri() {
        return uri;
    }

    public ExperimentSearch setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ExperimentSearch setLabel(String label) {
        this.label = label;
        return this;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public ExperimentSearch setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public ExperimentSearch setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public ExperimentSearch setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getObjective() {
        return objective;
    }

    public ExperimentSearch setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    public Integer getCampaign() {
        return campaign;
    }

    public ExperimentSearch setCampaign(Integer campaign) {
        this.campaign = campaign;
        return this;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public ExperimentSearch setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public ExperimentSearch setGroups(List<URI> groups) {
        this.groups = groups;
        return this;
    }

    public List<URI> getInfrastructures() {
        return infrastructures;
    }

    public ExperimentSearch setInfrastructures(List<URI> infrastructures) {
        this.infrastructures = infrastructures;
        return this;
    }

    public List<URI> getInstallations() {
        return installations;
    }

    public ExperimentSearch setInstallations(List<URI> installations) {
        this.installations = installations;
        return this;
    }

    public URI getSpecies() {
        return species;
    }

    public ExperimentSearch setSpecies(URI species) {
        this.species = species;
        return this;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public ExperimentSearch setIsPublic(Boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public ExperimentSearch setVariables(List<URI> variables) {
        this.variables = variables;
        return this;
    }

    public List<URI> getSensors() {
        return sensors;
    }

    public ExperimentSearch setSensors(List<URI> sensors) {
        this.sensors = sensors;
        return this;
    }

    public Boolean getEnded() {
        return ended;
    }

    public ExperimentSearch setEnded(Boolean ended) {
        this.ended = ended;
        return this;
    }

    public boolean isAdmin() {
        return admin;
    }

    public ExperimentSearch setAdmin(boolean admin) {
        this.admin = admin;
        return this;
    }

    protected void appendCampaignFilter(SelectBuilder select) throws Exception {
        if (campaign != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.CAMPAIGN_SPARQL_VAR, campaign));
        }
    }

    protected void appendSpeciesFilter(SelectBuilder select) throws Exception {
        if (species != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.SPECIES_SPARQL_VAR, species));
        }
    }

    protected void appendRegexObjectiveFilter(SelectBuilder select) {
        if (!StringUtils.isEmpty(objective)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.OBJECTIVE_SPARQL_VAR, objective));
        }
    }

    protected void appendRegexLabelFilter(SelectBuilder select) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ExperimentModel.LABEL_VAR, label));
        }
    }

    protected void appendUriRegexFilter(SelectBuilder select) {
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(),null));
        }
    }

    protected void appendDateFilters(SelectBuilder select) throws Exception {

        if (ended != null) {

            Node endDateVar = NodeFactory.createVariable(ExperimentModel.END_DATE_SPARQL_VAR);
            Node currentDateNode = SPARQLDeserializers.getForClass(LocalDate.class).getNode(LocalDate.now());

            // an experiment is ended if the end date is less than the the current date
            if (ended) {
                select.addFilter(SPARQLQueryHelper.getExprFactory().le(endDateVar, currentDateNode));
            } else {
                select.addFilter(SPARQLQueryHelper.getExprFactory().gt(endDateVar, currentDateNode));
            }
        }
        if (startDate != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.START_DATE_SPARQL_VAR, LocalDate.parse(startDate)));
        }
        if (endDate != null) {
            select.addFilter(SPARQLQueryHelper.eq(ExperimentModel.END_DATE_SPARQL_VAR, LocalDate.parse(endDate)));
        }
    }

    protected static void addWhere(SelectBuilder select, String subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(subjectVar), property.asNode(), makeVar(objectVar)));
    }

    protected void appendProjectListFilter(Map<String, List<?>> valuesByVar, SelectBuilder select) {

        if (projects != null && !projects.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasProject, ExperimentModel.PROJECT_URI_SPARQL_VAR);
            valuesByVar.put(ExperimentModel.PROJECT_URI_SPARQL_VAR, projects);
        }
    }

    protected void appendKeywordsListFilter(Map<String, List<?>> valuesByVar, SelectBuilder select) {
        if (keywords != null && !keywords.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasKeyword, ExperimentModel.KEYWORD_SPARQL_VAR);
            valuesByVar.put(ExperimentModel.KEYWORD_SPARQL_VAR, keywords);
        }
    }

    protected void appendInstallationsListFilter(Map<String, List<?>> valuesByVar, SelectBuilder select) {
        if (installations != null && !installations.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasDevice, ExperimentModel.DEVICES_SPARQL_VAR);
            valuesByVar.put(ExperimentModel.DEVICES_SPARQL_VAR, installations);
        }
    }

    protected void appendInfrastructuresListFilter(Map<String, List<?>> valuesByVar, SelectBuilder select) {
        if (infrastructures != null && !infrastructures.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.hasInfrastructure, ExperimentModel.INFRASTRUCTURE_SPARQL_VAR);
            valuesByVar.put(ExperimentModel.INFRASTRUCTURE_SPARQL_VAR, infrastructures);
        }
    }

    protected void appendGroupsListFilters(SelectBuilder select) {

        if (admin) {
            // add no filter on groups for the admin
            return;
        }
        Var groupVar = makeVar(ExperimentModel.GROUP_SPARQL_VAR);
        Triple groupTriple = new Triple(makeVar(ExperimentModel.URI_FIELD), Oeso.hasGroup.asNode(), groupVar);

        if (groups == null || groups.isEmpty()) {
            // get experiment without any group
            select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(groupTriple)));
        } else {
            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

            // get experiment with no group specified or in the given list
            ElementGroup rootFilteringElem = new ElementGroup();
            ElementGroup optionals = new ElementGroup();
            optionals.addTriplePattern(groupTriple);

            Expr boundExpr = exprFactory.not(exprFactory.bound(groupVar));
            Expr groupInUrisExpr = exprFactory.in(groupVar, groups.stream()
                    .map(uri -> NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString())))
                    .toArray());

            rootFilteringElem.addElement(new ElementOptional(optionals));
            rootFilteringElem.addElementFilter(new ElementFilter(SPARQLQueryHelper.or(boundExpr, groupInUrisExpr)));
            select.getWhereHandler().getClause().addElement(rootFilteringElem);
        }
    }

    protected void appendSensorListFilters(Map<String, List<?>> valuesByVar, SelectBuilder select) {
        if (sensors != null && !sensors.isEmpty()) {
            valuesByVar.put(ExperimentModel.SENSORS_SPARQL_VAR, sensors);
            addWhere(select, ExperimentModel.SENSORS_SPARQL_VAR, Oeso.participatesIn, ExperimentModel.URI_FIELD);

            // append a restriction on ?sensors variable to make sure that only instance of SensingDevice are retrieved
            Var sensorTypeVar = makeVar("SensingDeviceType");
            TriplePath typePath = select.makeTriplePath(makeVar(ExperimentModel.SENSORS_SPARQL_VAR), RDF.type, sensorTypeVar);
            TriplePath subClassPath = select.makeTriplePath(sensorTypeVar, Ontology.subClassAny, Oeso.SensingDevice.asNode());
            select.addWhere(subClassPath).addWhere(typePath);
        }
    }

    protected void appendVariablesListFilter(Map<String, List<?>> valuesByVar, SelectBuilder select) {
        if (variables != null && !variables.isEmpty()) {
            addWhere(select, ExperimentModel.URI_FIELD, Oeso.measures, ExperimentModel.VARIABLES_SPARQL_VAR);
            valuesByVar.put(ExperimentModel.VARIABLES_SPARQL_VAR, variables);
        }
    }


    /**
     * Update a {@link SelectBuilder} according search attributes
     * @param select the {@link SelectBuilder} to update
     */
    protected void apply(SelectBuilder select) throws Exception {

        appendGroupsListFilters(select);

        // build equality filters
        appendCampaignFilter(select);
        appendSpeciesFilter(select);

        // build regex based filter
        appendUriRegexFilter(select);
        appendRegexObjectiveFilter(select);
        appendRegexLabelFilter(select);

        appendDateFilters(select);

        // build list filters
        Map<String, List<?>> valuesByVar = new HashMap<>();

        appendProjectListFilter(valuesByVar, select);
        appendKeywordsListFilter(valuesByVar, select);
        appendInfrastructuresListFilter(valuesByVar, select);
        appendInstallationsListFilter(valuesByVar, select);
        appendSensorListFilters(valuesByVar, select);
        appendVariablesListFilter(valuesByVar, select);

        SPARQLQueryHelper.addWhereValues(select, valuesByVar);

    }
}
