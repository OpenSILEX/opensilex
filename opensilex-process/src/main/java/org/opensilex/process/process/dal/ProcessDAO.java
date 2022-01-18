//******************************************************************************
//                          ProcessDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.dal;

import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.process.ontology.PO2;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.exceptions.SPARQLException;
import java.time.OffsetDateTime;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.apache.jena.sparql.syntax.ElementFilter;


/**
 *
 * @author Emilie Fernandez
 */
public class ProcessDAO {

    protected final SPARQLService sparql;
    public static final Var startTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(ProcessModel.START_FIELD));
    public static final Var endTimeStampVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(ProcessModel.END_FIELD));
    public static final Var startStepVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(StepModel.START_FIELD));
    public static final Var endStepVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getTimeStampVarName(StepModel.END_FIELD));

    public ProcessDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ProcessModel create(ProcessModel instance, UserModel user) throws Exception {
        String lang = null;
        if (user != null) {
            lang = user.getLanguage();
        }
        List<StepModel> stepModel = sparql.getListByURIs(StepModel.class, instance.getStepUris(), lang);
        instance.setStep(stepModel);
        sparql.create(instance);
        return instance;
    }

    public ProcessModel update(ProcessModel instance, UserModel user) throws Exception {
        List<StepModel> stepModels = sparql.getListByURIs(StepModel.class, instance.getStepUris(), user.getLanguage());
        instance.setStep(stepModels);
        sparql.update(instance);
        return instance;
    }

    public ProcessModel getProcessByURI(URI instanceURI) throws Exception {
        return sparql.getByURI(ProcessModel.class, instanceURI, null);
    }
    public ListWithPagination<ProcessModel> searchProcess(UserModel user, String name, OffsetDateTime start, OffsetDateTime end, List<URI> step, List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        
        return sparql.searchWithPagination(
            ProcessModel.class,
            user.getLanguage(),
            (SelectBuilder select) -> {
                Node processGraph = sparql.getDefaultGraph(ProcessModel.class);
                ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                ElementGroup multipleGraphGroupElem =  SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, processGraph);
               
                appendNameProcessFilter(select, name);
                appendTimeFilter(multipleGraphGroupElem, start, end);
                appendStepProcessFilter(select, step);
            },
            orderByList,
            page,
            pageSize
        );
    }

    private void appendNameProcessFilter(SelectBuilder select, String name) throws Exception {
        if (!StringUtils.isEmpty(name)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(ProcessModel.NAME_FIELD, name));
        }
    }

    protected void appendTimeFilter(ElementGroup processGraphGroupElem, OffsetDateTime start, OffsetDateTime end
    ) throws Exception {

        if (start == null && end == null) {
            return;
        }

        // startTimeStampVar and endTimeStampVar auto-added (in SELECT and WHERE) by the SPARQLClassQueryBuilder

        Expr dateRange = SPARQLQueryHelper.eventsIntervalDateRange(startTimeStampVar.getVarName(), start, endTimeStampVar.getVarName(), end);
        processGraphGroupElem.addElementFilter(new ElementFilter(dateRange));
    }

    private void appendStepProcessFilter(SelectBuilder select, List<URI> step) throws Exception {
        if (step != null && !step.isEmpty()) {
            addWhere(select, ProcessModel.URI_FIELD, PO2.hasForStep, ProcessModel.STEP_FIELD);
            select.addFilter(SPARQLQueryHelper.inURIFilter(ProcessModel.STEP_FIELD, step));
        }
    }

    public void deleteProcess(URI uri, UserModel user) throws Exception {
        sparql.delete(ProcessModel.class, uri);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////

    public StepModel createStep(StepModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public StepModel update(StepModel instance, UserModel user) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public StepModel getStepByURI(URI instanceURI) throws Exception {
        return sparql.getByURI(StepModel.class, instanceURI, null);
    }

    public ListWithPagination<StepModel> searchStep(UserModel user, String name, OffsetDateTime start, OffsetDateTime end, List<URI> input, List<URI> output, List<OrderBy> orderByList, int page, int pageSize) throws Exception {
        
        return sparql.searchWithPagination(
            StepModel.class,
            user.getLanguage(),
            (SelectBuilder select) -> {
                Node stepGraph = sparql.getDefaultGraph(StepModel.class);
                ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                ElementGroup multipleGraphGroupElem =  SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, stepGraph);
               
                appendNameStepFilter(select, name);
                appendTimeStepFilter(select, multipleGraphGroupElem, start, end);
                appendInputStepFilter(select, input);
                appendOutputStepFilter(select, output);
            },
            orderByList,
            page,
            pageSize
        );
    }

    private void appendNameStepFilter(SelectBuilder select, String name) throws Exception {
        if (!StringUtils.isEmpty(name)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(StepModel.NAME_FIELD, name));
        }
    }

    protected void appendTimeStepFilter(SelectBuilder select, ElementGroup stepGraphGroupElem, OffsetDateTime start, OffsetDateTime end
    ) throws Exception {
        select.getVars().removeIf(var ->
                var.getVarName().equals(StepModel.START_FIELD) || var.getVarName().equals(StepModel.END_FIELD)
        );
        select.addVar(startStepVar);
        select.addVar(endStepVar);

        if (start == null && end == null) {
            return;
        }

        Expr dateRange = SPARQLQueryHelper.eventsIntervalDateRange(startStepVar.getVarName(), start, endStepVar.getVarName(), end);
        stepGraphGroupElem.addElementFilter(new ElementFilter(dateRange));
    }

    private void appendInputStepFilter(SelectBuilder select, List<URI> input) throws Exception {
        if (input !=null && !input.isEmpty()) {
            addWhere(select, StepModel.URI_FIELD, PO2.hasInput, StepModel.INPUT_FIELD);
            select.addFilter(SPARQLQueryHelper.inURIFilter(StepModel.INPUT_FIELD, input));
        }
    }

    private void appendOutputStepFilter(SelectBuilder select, List<URI> output) throws Exception {
        if (output != null && !output.isEmpty()) {
            addWhere(select, StepModel.URI_FIELD, PO2.hasOutput, StepModel.OUTPUT_FIELD);
            select.addFilter(SPARQLQueryHelper.inURIFilter(StepModel.OUTPUT_FIELD, output));
        }
    }

    private static void addWhere(SelectBuilder select, String subjectVar, Property property, String objectVar) {
        select.getWhereHandler().getClause().addTriplePattern(new Triple(makeVar(subjectVar), property.asNode(), makeVar(objectVar)));
    }

    public void deleteStep(URI uri, UserModel user) throws Exception {
        sparql.delete(StepModel.class, uri);
    }

    public boolean isLinkedToProcess(StepModel step) throws SPARQLException {
        Var subject = makeVar("s");
        return sparql.executeAskQuery(
                new AskBuilder()
                        .addWhere(subject, PO2.hasForStep, SPARQLDeserializers.nodeURI(step.getUri()))
        );
        
    }


}