//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasmGroup.dal;

import org.apache.commons.collections4.CollectionUtils;
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
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Maximilian HART
 */
public class GermplasmGroupDAO {

    protected final SPARQLService sparql;
    protected final Node defaultGraph;
    protected final Node defaultGermplasmGraph;


    public GermplasmGroupDAO(SPARQLService sparql) {
        this.sparql = sparql;

        try{
            defaultGraph = sparql.getDefaultGraph(GermplasmGroupModel.class);
            defaultGermplasmGraph = sparql.getDefaultGraph(GermplasmModel.class);
        }catch (SPARQLException e){
           throw new RuntimeException(e);
        }
    }

    public GermplasmGroupModel create(GermplasmGroupModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public GermplasmGroupModel update(GermplasmGroupModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(GermplasmGroupModel.class, uri);
    }


    public List<GermplasmGroupModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(GermplasmGroupModel.class, uris, null);
    }

    private static void addGermplasmFilter(SelectBuilder select, List<URI> germplasm) throws Exception{
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var germplasmVar = makeVar("__germplasm");
        select.addWhere(uriVar, RDFS.member, germplasmVar);
        select.addFilter(SPARQLQueryHelper.inURIFilter(germplasmVar, germplasm));
    }

    public ListWithPagination<GermplasmGroupModel> search(String name, List<URI> germplasms, List<OrderBy> orderByList, int page, int pageSize, String lang) throws Exception {
        Expr filterName = SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, name);

        return sparql.searchWithPagination(

                GermplasmGroupModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (filterName != null) {
                        select.addFilter(filterName);
                    }
                    if (CollectionUtils.isNotEmpty(germplasms)) {
                        addGermplasmFilter(select, germplasms);
                    }
                },

                orderByList,
                page,
                pageSize
        );
    }

    public GermplasmGroupModel get(URI uri, String lang, boolean withGermplasms) throws Exception {
        GermplasmGroupModel model = sparql.getByURI(GermplasmGroupModel.class, uri, null);
        if( model != null ){
            if(withGermplasms){
                List<GermplasmGroupModel> singletonList = Collections.singletonList(model);
                fetchGermplasmsGroup(singletonList, lang);
                return singletonList.get(0);
            } else {
                return model;
            }
        } else {
            return null;
        }

    }

    private void fetchGermplasmsGroup(List<GermplasmGroupModel> models, String lang) throws SPARQLException, ParseException {

        if(models.isEmpty()){
            return;
        }

        Map<String, Integer> modelsIndexes = new PatriciaTrie<>();
        for (int i = 0; i < models.size(); i++) {
            GermplasmGroupModel model = models.get(i);
            modelsIndexes.put(URIDeserializer.formatURIAsStr(model.getUri().toString()), i);
            model.setGermplasmList(new LinkedList<>());
        }

        Stream<URI> uris = models.stream().map(SPARQLResourceModel::getUri);

        Var germplasmGroup = makeVar("germplasm_group");
        Var germplasm = makeVar("germplasm");
        Var germplasmName = makeVar("germplasm_name");
        Var germplasmType = makeVar("germplasm_type");
        SelectBuilder query = new SelectBuilder()
                .addVar(germplasm)
                .addVar(germplasmType)
                .addVar(germplasmName)
                .addGraph(defaultGraph, new WhereBuilder()
                        .addWhere(germplasmGroup, RDF.type, Oeso.GermplasmGroup)
                        .addWhere(germplasmGroup, RDFS.member, germplasm))
                .addGraph(defaultGermplasmGraph, new WhereBuilder()
                        .addWhere(germplasm, RDFS.label, germplasmName)
                        .addWhere(germplasm, RDF.type, germplasmType)
                )
                .addGroupBy(germplasm)
                .addGroupBy(germplasmType)
                .addGroupBy(germplasmName);

        // aggregate groups with variables
        SPARQLQueryHelper.appendGroupConcatAggregator(query, germplasmGroup, true);

        SPARQLQueryHelper.addWhereUriValues(query, germplasmGroup.getVarName(), uris, models.size());

        // retrieve germplasm class from Ontology
        ClassModel germplasmClass = SPARQLModule.getOntologyStoreInstance().getClassModel(URI.create(Oeso.Germplasm.getURI()), null, lang);

        // read variables
        Stream<SPARQLResult> results = sparql.executeSelectQueryAsStream(query);
        results.forEach(result -> {

            GermplasmModel nestedModel = new GermplasmModel();
            nestedModel.setUri(URIDeserializer.formatURI(result.getStringValue(germplasm.getVarName())));
            nestedModel.setName(result.getStringValue(germplasmName.getVarName()));
            nestedModel.setType(URIDeserializer.formatURI(result.getStringValue(germplasmType.getVarName())));
            nestedModel.setTypeLabel(germplasmClass.getLabel());

            // get groups associated to the variable
            String joiningColumn = result.getStringValue(SPARQLQueryHelper.getConcatVarName(germplasmGroup.getVarName()));
            String[] foreignKeys = joiningColumn.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);

            for (String key : foreignKeys) {
                String shortKey = URIDeserializer.formatURIAsStr(key);
                Integer groupIdx = modelsIndexes.get(shortKey);
                GermplasmGroupModel group = models.get(groupIdx);
                group.getGermplasmList().add(nestedModel);
            }
        });

    }

}
