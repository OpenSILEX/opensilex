//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.geneticResourceGroup.dal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.rdf.model.Property;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Maximilian HART
 */
public class GeneticResourceGroupDAO {

    protected final SPARQLService sparql;
    protected final Node geneticResourceGroupsDefaultGraph;
    protected final Node defaultGeneticResourceGraph;


    public GeneticResourceGroupDAO(SPARQLService sparql) {
        this.sparql = sparql;

        try{
            geneticResourceGroupsDefaultGraph = sparql.getDefaultGraph(GeneticResourceGroupModel.class);
            defaultGeneticResourceGraph = sparql.getDefaultGraph(GeneticResourceModel.class);
        }catch (SPARQLException e){
           throw new RuntimeException(e);
        }
    }

    public GeneticResourceGroupModel create(GeneticResourceGroupModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public GeneticResourceGroupModel update(GeneticResourceGroupModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(GeneticResourceGroupModel.class, uri);
    }


    public List<GeneticResourceGroupModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(GeneticResourceGroupModel.class, uris, null);
    }

    private static void addGeneticResourceFilter(SelectBuilder select, List<URI> geneticResource) throws Exception{
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var geneticResourceVar = makeVar("__geneticResource");
        select.addWhere(uriVar, RDFS.member, geneticResourceVar);
        select.addFilter(SPARQLQueryHelper.inURIFilter(geneticResourceVar, geneticResource));
    }

    public ListWithPagination<GeneticResourceGroupModel> search(String name, List<URI> geneticResources, List<OrderBy> orderByList, int page, int pageSize, String lang) throws Exception {
        Expr filterName = SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, name);

        return sparql.searchWithPagination(

                GeneticResourceGroupModel.class,
                lang,
                (SelectBuilder select) -> {
                    if (filterName != null) {
                        select.addFilter(filterName);
                    }
                    if (CollectionUtils.isNotEmpty(geneticResources)) {
                        addGeneticResourceFilter(select, geneticResources);
                    }
                },

                orderByList,
                page,
                pageSize
        );
    }

    public GeneticResourceGroupModel get(URI uri, String lang, boolean withGeneticResources) throws Exception {
        GeneticResourceGroupModel model = sparql.getByURI(GeneticResourceGroupModel.class, uri, null);
        if( model != null ){
            if(withGeneticResources){
                List<GeneticResourceGroupModel> singletonList = Collections.singletonList(model);
                fetchGeneticResourcesOfGroup(singletonList, lang);
                return singletonList.get(0);
            } else {
                return model;
            }
        } else {
            return null;
        }

    }

    private void fetchGeneticResourcesOfGroup(List<GeneticResourceGroupModel> models, String lang) throws SPARQLException, ParseException {

        if(models.isEmpty()){
            return;
        }
        Map<String, Integer> modelsIndexes = new PatriciaTrie<>();
        for (int i = 0; i < models.size(); i++) {
            GeneticResourceGroupModel model = models.get(i);
            modelsIndexes.put(URIDeserializer.formatURIAsStr(model.getUri().toString()), i);
            model.setGeneticResourceList(new LinkedList<>());
        }

        Stream<URI> uris = models.stream().map(SPARQLResourceModel::getUri);

        createManyToManySPARQLRequest(
                geneticResourceGroupsDefaultGraph,
                defaultGeneticResourceGraph,
                uris,
                (ManyToManyNestedUpdateData manyToManyNestedUpdateData) -> {
                    Integer groupIdx = modelsIndexes.get(manyToManyNestedUpdateData.subjectUri);
                    GeneticResourceGroupModel group = models.get(groupIdx);
                    GeneticResourceModel nestedModelAsGeneticResource = GeneticResourceModel.fromSPARQLNamedResourceModel(manyToManyNestedUpdateData.nestedModel);
                    group.getGeneticResourceList().add(nestedModelAsGeneticResource);
                },
                RDFS.member,
                Oeso.GeneticResourceGroup,
                models.size(),
                sparql,
                lang
                );
    }

    /**
     *
     * Temporary function, to extract some of the above code that's used in multiple locations with different model types
     * TODO speak with valentin or Renaud about where to do this properly
     *
     * original documentation :
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
     */
    public static void createManyToManySPARQLRequest(
            Node subjectDefaultGraph,
            Node objectDefaultGraph,
            Stream<URI> subjectUriStream,
            Consumer<ManyToManyNestedUpdateData> addModelToSubjectObjectList,
            Property predicate,
            Resource subjectType,
            int subjectQuantity,
            SPARQLService sparql,
            String lang) throws ParseException, SPARQLException {

        Var subjectVar = makeVar("subject_var");
        Var objectVar = makeVar("object_var");
        Var object_name = makeVar("object_name");
        Var object_type = makeVar("object_type");
        SelectBuilder query = new SelectBuilder()
                .addVar(objectVar)
                .addVar(object_type)
                .addVar(object_name)
                .addGraph(subjectDefaultGraph, new WhereBuilder()
                        .addWhere(subjectVar, predicate, objectVar))
                .addGraph(objectDefaultGraph, new WhereBuilder()
                        .addWhere(objectVar, RDFS.label, object_name)
                        .addWhere(objectVar, RDF.type, object_type)
                )
                .addGroupBy(objectVar)
                .addGroupBy(object_type)
                .addGroupBy(object_name);


        //Only get labels of correct language
        if (!StringUtils.isEmpty(lang)) {
            Expr langFilter = SPARQLQueryHelper.langFilterWithDefault("object_name", Locale.forLanguageTag(lang).getLanguage());
            query.addFilter(langFilter);
        }

        // aggregate groups with variables
        SPARQLQueryHelper.appendGroupConcatAggregator(query, subjectVar, true);

        SPARQLQueryHelper.addWhereUriValues(query, subjectVar.getVarName(), subjectUriStream, subjectQuantity);

        // retrieve geneticResource class from Ontology
        ClassModel objectModelClass = SPARQLModule.getOntologyStoreInstance().getClassModel(URI.create(Oeso.GeneticResource.getURI()), null, lang);

        // read variables
        Stream<SPARQLResult> results = sparql.executeSelectQueryAsStream(query);
        results.forEach(result -> {

            SPARQLNamedResourceModel nestedModel = new SPARQLNamedResourceModel();
            nestedModel.setUri(URIDeserializer.formatURI(result.getStringValue(objectVar.getVarName())));
            nestedModel.setName(result.getStringValue(object_name.getVarName()));
            nestedModel.setType(URIDeserializer.formatURI(result.getStringValue(object_type.getVarName())));
            nestedModel.setTypeLabel(objectModelClass.getLabel());

            // get groups associated to the variable
            String joiningColumn = result.getStringValue(SPARQLQueryHelper.getConcatVarName(subjectVar.getVarName()));
            String[] foreignKeys = joiningColumn.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);

            for (String key : foreignKeys) {
                String shortKey = URIDeserializer.formatURIAsStr(key);
                addModelToSubjectObjectList.accept(new ManyToManyNestedUpdateData(shortKey, nestedModel));
            }
        });
    }

    public static class ManyToManyNestedUpdateData {
        public String subjectUri;
        public SPARQLNamedResourceModel nestedModel;

        ManyToManyNestedUpdateData(String subjectUri, SPARQLNamedResourceModel nestedModel){
            this.subjectUri = subjectUri;
            this.nestedModel = nestedModel;
        }

    }

}
