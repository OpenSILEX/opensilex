/*
 * ******************************************************************************
 *                                     FactorLevelDAO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorLevelDAO {

    protected final SPARQLService sparql;

    public FactorLevelDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public FactorLevelModel create(FactorLevelModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }
    
    public List<FactorLevelModel> create(List<FactorLevelModel> instances) throws Exception {
        // TODO Fix multiple instance creation in the same transaction
        for (FactorLevelModel instance : instances) {
            sparql.create(instance);
        }
        return instances;
    }

    public FactorLevelModel update(FactorLevelModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }
    
    public void deleteCollection(List<URI> instanceURIs) throws Exception {
        for (URI instanceURI : instanceURIs) {
            sparql.delete(FactorLevelModel.class, instanceURI);
        }
    }

    

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(FactorLevelModel.class, instanceURI);
    }

    public FactorLevelModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(FactorLevelModel.class, instanceURI, null);
    }

    public ListWithPagination<FactorLevelModel> search(String alias, URI hasFactor, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {
        return sparql.searchWithPagination(
                FactorLevelModel.class,
                null,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters(alias, hasFactor, select);
                },
                orderByList,
                page,
                pageSize
        );
    }
    
    
    public List<FactorLevelModel> search(URI hasFactor) throws Exception {
        return sparql.search(
                FactorLevelModel.class,
                null,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters(null,hasFactor, select);
                }
        );
    }


    /**
     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for each non-empty simple attribute ( not a {@link List} from the {@link ExperimentSearchDTO}
     *
     * @param alias search factor levels by alias
     * @param hasFactor search factor levels by factors
     * @param select search query
     * @throws java.lang.Exception can throw an exception
     * @see SPARQLQueryHelper the utility class used to build Expr
     */
    protected void appendFilters(String alias, URI hasFactor, SelectBuilder select) throws Exception {

        List<Expr> exprList = new ArrayList<>();

       
        // build regex filters
        if (alias != null) {
            exprList.add(SPARQLQueryHelper.regexFilter(FactorLevelModel.NAME_FIELD, alias));
        }

        // build equality filters
//        if (hasFactor!= null) {
//            exprList.add(SPARQLQueryHelper.eq(FactorLevelModel.HAS_FACTOR_FIELD, hasFactor));
//        }

        for (Expr filterExpr : exprList) {
            select.addFilter(filterExpr);
        }
    }
    
    public boolean isLinkedToSth(FactorLevelModel factorLevel) throws SPARQLException {
        Var subject = makeVar("s");
        return sparql.executeAskQuery(new AskBuilder()
                        .addWhere(subject, Oeso.hasFactorLevel, SPARQLDeserializers.nodeURI(factorLevel.getUri())) 
        );
    }

    /**
     *
     * @param experiment experiment
     * @return a Stream on each unique factor level URI from the given experiment factors
     * @throws IllegalArgumentException if experiment is null
     *
     * @apiNote
     * <pre>
     * <b>SPARQL query : </b>
     *
     * {@code
     * PREFIX vocabulary: <http://www.opensilex.org/vocabulary/oeso#>
     * SELECT DISTINCT ?factorLevel WHERE {
     *      GRAPH <../set/experiment> {
     *          :experiment vocabulary:studyEffectOf ?factor
     *      }
     *      GRAPH <../set/factor>{
     *   	    ?factorLevel vocabulary:hasFactor ?factor
     *      }
     * }
     * }
     * </pre>
     */
    public Stream<String> getLevelsFromExperiment(URI experiment) throws Exception{

        Objects.requireNonNull(experiment);

        Node experimentNode = SPARQLDeserializers.nodeURI(experiment);
        Var factor = makeVar(ExperimentModel.FACTORS_FIELD);
        Var factorLevel = makeVar(FactorModel.FACTORLEVELS_SPARQL_VAR);

        SelectBuilder query = new SelectBuilder()
                .setDistinct(true)
                .addVar(factorLevel)
                .addGraph(sparql.getDefaultGraph(ExperimentModel.class), experimentNode, Oeso.studyEffectOf, factor)
                .addGraph(sparql.getDefaultGraph(FactorModel.class),factorLevel,Oeso.hasFactor,factor);

        return sparql.executeSelectQueryAsStream(query)
                .map(result -> URIDeserializer.formatURIAsStr(result.getStringValue(factorLevel.getVarName())));
    }
}
