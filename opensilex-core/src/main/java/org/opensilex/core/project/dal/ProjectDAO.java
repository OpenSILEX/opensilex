//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;

/**
 * @author Julien BONNEFONT
 */
public class ProjectDAO {

    protected final SPARQLService sparql;

    public ProjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ProjectModel create(ProjectModel instance) throws Exception {
        checkURIs(instance);
        sparql.create(instance);
        return instance;
    }

    public ProjectModel update(ProjectModel instance) throws Exception {
        checkURIs(instance);
        sparql.update(instance);
        return instance;
    }

    /**
     * check if all URI from uris have the typeResource as {@link RDF#type} into
     * the SPARQL graph
     *
     * @param uris the {@link List} of {@link URI} to check
     * @param typeResource the {@link Resource} indicating the {@link RDF#type
     */
    protected void checkURIs(List<URI> uris, Resource typeResource) throws URISyntaxException, SPARQLException {

        if (uris == null || uris.isEmpty()) {
            return;
        }
        for (URI uri : uris) {
            if (!sparql.uriExists(new URI(typeResource.getURI()), uri)) {
                throw new IllegalArgumentException("Trying to insert an experiment with an unknown " + typeResource.getLocalName() + " : " + uri);
            }
        }
    }

    /**
     * Check that all URI(s) which refers to a non
     * {@link org.opensilex.sparql.annotations.SPARQLResource}-compliant model
     * exists.
     *
     * @param model the experiment for which we check if all URI(s) exists
     * @throws SPARQLException if the SPARQL uri checking query fail
     * @throws IllegalArgumentException if the given model contains a unknown
     * URI
     */
    protected void checkURIs(ProjectModel model) throws SPARQLException, IllegalArgumentException, URISyntaxException {

        // #TODO use a method to test in one query if multiple URI(s) exists and are of a given type, or use SHACL validation
//
//        checkURIs(model.getInfrastructures(), (Oeso.Infrastructure));
//        checkURIs(model.getSensors(), (Oeso.SensingDevice));
//        checkURIs(model.getVariables(), (Oeso.Variable));
//        checkURIs(model.getDevices(), (Oeso.Installation));
//
//        if (model.getSpecies() != null && !sparql.uriExists(new URI(Oeso.Species.getURI()), model.getSpecies())) {
//            throw new IllegalArgumentException("Trying to insert an experiment with an unknown species : " + model.getSpecies());
//        }
    }

    

    public void delete(List<URI> prjctUris) throws Exception {
        sparql.delete(ProjectModel.class, prjctUris);
    }
    
    
    public void delete(URI instanceURI) throws Exception {
        sparql.delete(ProjectModel.class, instanceURI);
    }

    public ProjectModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(ProjectModel.class, instanceURI, null);
    }

    
    public ListWithPagination<ProjectModel> search(
            URI uri,
            String name,
            String startDate,
            String endDate,
            Boolean isEnded,
            List<URI> experiments,
            Boolean isPublic, 
            List<URI> groups,boolean admin,
            List<OrderBy> orderByList,Integer page,Integer pageSize) throws Exception {

        return sparql.searchWithPagination(
                ProjectModel.class,
                null,
                (SelectBuilder select) -> {
                    appendUriRegexFilter(select, uri);
                    appendRegexLabelFilter(select, name);
                    appendDateFilters(select, isEnded, startDate, endDate);
//                    appendGroupsListFilters(select, admin, isPublic, groups);
                    appendExperimentListFilter(select, experiments);
                },
                orderByList,
                page,
                pageSize
        );
    }
    
    

    protected void appendRegexLabelFilter(SelectBuilder select, String label) {
        if (!StringUtils.isEmpty(label)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ProjectModel.LABEL_VAR, label));
        }
    }

    protected void appendUriRegexFilter(SelectBuilder select, URI uri) {
        if (uri != null) {
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Expr strUriExpr = SPARQLQueryHelper.getExprFactory().str(uriVar);
            select.addFilter(SPARQLQueryHelper.regexFilter(strUriExpr, uri.toString(), null));
        }
    }

    protected void appendDateFilters(SelectBuilder select, Boolean ended, String startDate, String endDate) throws Exception {

        if (ended != null) {

            Node endDateVar = NodeFactory.createVariable(ProjectModel.END_DATE_SPARQL_VAR);
            Node currentDateNode = SPARQLDeserializers.getForClass(LocalDate.class).getNode(LocalDate.now());

            // an experiment is ended if the end date is less than the the current date
            if (ended) {
                select.addFilter(SPARQLQueryHelper.getExprFactory().le(endDateVar, currentDateNode));
            } else {
                ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
                Expr noEndDateFilter = exprFactory.not(exprFactory.bound(endDateVar));
                select.addFilter(exprFactory.or(noEndDateFilter, exprFactory.gt(endDateVar, currentDateNode)));
            }
        }
        if (startDate == null && endDate == null) {
            return;
        }

        LocalDate startLocateDate = startDate == null ? null : LocalDate.parse(startDate);
        LocalDate endLocalDate = endDate == null ? null : LocalDate.parse(endDate);

        Expr dateRangeExpr = SPARQLQueryHelper.dateRange(ProjectModel.START_DATE_SPARQL_VAR, startLocateDate, ProjectModel.END_DATE_SPARQL_VAR, endLocalDate);
        select.addFilter(dateRangeExpr);
    }

    protected void appendExperimentListFilter(SelectBuilder select, List<URI> experiments) throws Exception {

        if (experiments != null && !experiments.isEmpty()) {
            addWhere(select, ProjectModel.URI_FIELD, Oeso.hasProject, ProjectModel.EXPERIMENT_URI_SPARQL_VAR);
            SPARQLQueryHelper.addWhereValues(select, ProjectModel.EXPERIMENT_URI_SPARQL_VAR, experiments);
        }
    }

    protected static void addWhere(SelectBuilder select, String subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(subjectVar), property.asNode(), makeVar(objectVar)));
    }
    
    
//     protected void appendGroupsListFilters(SelectBuilder select, boolean admin, Boolean isPublic, List<URI> groups) {
//
//        if (admin) {
//            // add no filter on groups for the admin
//            return;
//        }
//        Var groupVar = makeVar(ProjectModel.GROUP_SPARQL_VAR);
//        Triple groupTriple = new Triple(makeVar(ProjectModel.URI_FIELD), Oeso.hasGroup.asNode(), groupVar);
//
//        if (CollectionUtils.isEmpty(groups) || (isPublic != null && isPublic)) {
//            // get project without any group
//            select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(groupTriple)));
//        } else {
//            ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
//
//            // get project with no group specified or in the given list
//            ElementGroup rootFilteringElem = new ElementGroup();
//            ElementGroup optionals = new ElementGroup();
//            optionals.addTriplePattern(groupTriple);
//
//            Expr boundExpr = exprFactory.not(exprFactory.bound(groupVar));
//            Expr groupInUrisExpr = exprFactory.in(groupVar, groups.stream()
//                    .map(uri -> NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(uri.toString())))
//                    .toArray());
//
//            rootFilteringElem.addElement(new ElementOptional(optionals));
//            rootFilteringElem.addElementFilter(new ElementFilter(SPARQLQueryHelper.or(boundExpr, groupInUrisExpr)));
//            select.getWhereHandler().getClause().addElement(rootFilteringElem);
//        }
//    }
}
