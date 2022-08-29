//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.dal;

import java.net.URI;

import java.util.*;
import java.util.stream.Stream;


import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;

import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author Hamza IKIOU
 */
public class VariablesGroupDAO {

    protected final SPARQLService sparql;
    protected final Node defaultGraph;
    protected final Node defaultVariableGraph;


    public VariablesGroupDAO(SPARQLService sparql) {
        this.sparql = sparql;

        try{
            defaultGraph = sparql.getDefaultGraph(VariablesGroupModel.class);
            defaultVariableGraph = sparql.getDefaultGraph(VariableModel.class);
        }catch (SPARQLException e){
           throw new RuntimeException(e);
        }
    }

    public VariablesGroupModel create(VariablesGroupModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public VariablesGroupModel update(VariablesGroupModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(VariablesGroupModel.class, uri);
    }

    public VariablesGroupModel get(URI uri) throws Exception {
        VariablesGroupModel model = sparql.getByURI(VariablesGroupModel.class, uri, null);
        if(model != null){
            fetchVariablesGroup(Collections.singletonList(model),null);
        }
        return model;
    }

    public List<VariablesGroupModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(VariablesGroupModel.class, uris, null);
    }

    private static void addVariableFilter(SelectBuilder select, URI variableUri) throws Exception{
        Triple triple = new Triple(makeVar(SPARQLResourceModel.URI_FIELD),RDFS.member.asNode(),makeVar(VariablesGroupModel.VARIABLES_LIST_FIELD));
        select.addWhere(triple);
        Expr filterVariableUri = SPARQLQueryHelper.eq(VariablesGroupModel.VARIABLES_LIST_FIELD, variableUri);
        select.addFilter(filterVariableUri);
    }

    public ListWithPagination<VariablesGroupModel> search(String name, URI variableUri,List<OrderBy> orderByList, int page, int pageSize, String lang) throws Exception {
        Expr filterName = SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, name);

        // use fetcher for model attributes retrieval
        SparqlNoProxyFetcher<VariablesGroupModel> fetcher = new SparqlNoProxyFetcher<>(VariablesGroupModel.class, sparql);

        ListWithPagination<VariablesGroupModel> models = sparql.searchWithPagination(
                defaultGraph,
                VariablesGroupModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (filterName != null) {
                        select.addFilter(filterName);
                    }
                    if (variableUri != null) {
                        addVariableFilter(select, variableUri);
                    }
                },
                Collections.emptyMap(),
                result -> fetcher.getInstance(result, lang),
                orderByList,
                page,
                pageSize
        );

        // custom optimized retrieval of variables
        fetchVariablesGroup(models.getList(),lang);
        return models;
    }

    /**
     *
     * @param models variables group
     * @param lang Language code, used to determine associated the translated label of the associated variable
     *
     * @throws SPARQLException if SPARQL query evaluation fail
     *
     * @apiNote Example of generated SPARQL query
     *
     * <pre>
     * SELECT ?variable_group ?variable ?variable_type ?variable_name
     * (GROUP_CONCAT(DISTINCT ?variable_group ; separator=',') AS ?variable_group__opensilex__concat)  WHERE {
     *
     *      GRAPH test:variablesGroup {
     *          ?variable_group a vocabulary:VariablesGroup .
     *          ?variable_group rdfs:member ?variable
     *      }
     *      GRAPH test:variables {
     *          ?variable rdfs:label ?variable_name .
     *          ?variable a ?variable_type .
     *      }
     *      VALUES ?variable_group {  test:variable_group1 test:variable_group2  }
     *      GROUP BY ?variable ?variable_name ?variable_type
     * }
     * </pre>
     *
     * Considering n groups and m the total number of variables
     * This method has the following complexity :
     * <ul>
     *     <li>Time complexity : O(n+m)</li>
     *     <li>Query complexity : O(1)</li>
     *     <li>Space complexity : O(n)</li>
     * </ul>
     * #TODO : extract this method by developping a many-to-many relationship fetcher
     */
    public void fetchVariablesGroup(List<VariablesGroupModel> models, String lang) throws SPARQLException, ParseException {

        if(models.isEmpty()){
            return;
        }

        Map<String, Integer> modelsIndexes = new PatriciaTrie<>();
        for (int i = 0; i < models.size(); i++) {
            VariablesGroupModel model = models.get(i);
            modelsIndexes.put(URIDeserializer.formatURIAsStr(model.getUri().toString()), i);
            model.setVariablesList(new LinkedList<>());
        }

        Stream<URI> uris = models.stream().map(SPARQLResourceModel::getUri);

        Var variableGroup = makeVar("variable_group");
        Var variable = makeVar("variable");
        Var variableName = makeVar("variable_name");
        Var variableType = makeVar("variable_type");
        SelectBuilder query = new SelectBuilder()
                .addVar(variable)
                .addVar(variableType)
                .addVar(variableName)
                .addGraph(defaultGraph, new WhereBuilder()
                        .addWhere(variableGroup, RDF.type, Oeso.VariablesGroup)
                        .addWhere(variableGroup, RDFS.member, variable))
                .addGraph(defaultVariableGraph, new WhereBuilder()
                        .addWhere(variable, RDFS.label, variableName)
                        .addWhere(variable, RDF.type, variableType)
                )
                .addGroupBy(variable)
                .addGroupBy(variableType)
                .addGroupBy(variableName);

        // aggregate groups with variables
        SPARQLQueryHelper.appendGroupConcatAggregator(query, variableGroup, true);

        SPARQLQueryHelper.addWhereUriValues(query, variableGroup.getVarName(), uris, models.size());

        // retrieve variable class from Ontology
        ClassModel variableClass = SPARQLModule.getOntologyStoreInstance().getClassModel(URI.create(Oeso.Variable.getURI()), null, lang);

        // read variables
        Stream<SPARQLResult> results = sparql.executeSelectQueryAsStream(query);
        results.forEach(result -> {

            VariableModel nestedModel = new VariableModel();
            nestedModel.setUri(URIDeserializer.formatURI(result.getStringValue(variable.getVarName())));
            nestedModel.setName(result.getStringValue(variableName.getVarName()));
            nestedModel.setType(URIDeserializer.formatURI(result.getStringValue(variableType.getVarName())));
            nestedModel.setTypeLabel(variableClass.getLabel());

            // get groups associated to the variable
            String joiningColumn = result.getStringValue(SPARQLQueryHelper.getConcatVarName(variableGroup.getVarName()));
            String[] foreignKeys = joiningColumn.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);

            for (String key : foreignKeys) {
                String shortKey = URIDeserializer.formatURIAsStr(key);
                Integer groupIdx = modelsIndexes.get(shortKey);
                VariablesGroupModel group = models.get(groupIdx);
                group.getVariablesList().add(nestedModel);
            }
        });

    }

}
