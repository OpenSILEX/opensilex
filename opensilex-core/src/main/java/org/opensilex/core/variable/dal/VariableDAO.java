//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import com.google.common.base.CaseFormat;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vidalmor
 * @author rcolin
 */
public class VariableDAO extends BaseVariableDAO<VariableModel> {

    static Var entityLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.ENTITY_FIELD_NAME));
    static Var entityOfInterestLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.ENTITY_OF_INTEREST_FIELD_NAME));
    static Var characteristicLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.CHARACTERISTIC_FIELD_NAME));
    static Var methodLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.METHOD_FIELD_NAME));
    static Var unitLabelVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectNameVarName(VariableModel.UNIT_FIELD_NAME));

    /**
        * Contains the pre-computed list of SPARQL variables which could be used in order to filter or
        * entity, entity of interest, characteristic, method or unit name.
        * <p>
        * This {@link Map} is indexed by the variables names
    */
    static Map<String, Var> varsByVarName;
    static {
        varsByVarName = new HashMap<>();
        varsByVarName.put(entityLabelVar.getVarName(), entityLabelVar);
        varsByVarName.put(entityOfInterestLabelVar.getVarName(), entityOfInterestLabelVar);
        varsByVarName.put(characteristicLabelVar.getVarName(), characteristicLabelVar);
        varsByVarName.put(methodLabelVar.getVarName(), methodLabelVar);
        varsByVarName.put(unitLabelVar.getVarName(), unitLabelVar);
    }

    protected final DataDAO dataDAO;

    public VariableDAO(SPARQLService sparql, MongoDBService nosql, FileStorageService fs) throws URISyntaxException {
        super(VariableModel.class, sparql);
        this.dataDAO = new DataDAO(nosql, sparql, fs);
    }

    @Override
    public void delete(URI varUri) throws Exception {
        int linkedDataNb = getLinkedDataNb(varUri);
        if (linkedDataNb > 0) {
            throw new ForbiddenURIAccessException(varUri, "Variable can't be deleted. " + linkedDataNb + " linked data");
        }
        super.delete(varUri);
    }

    protected int getLinkedDataNb(URI uri) throws Exception {
        return dataDAO.count(null, null, null, Collections.singletonList(uri), null, null, null, null, null, null, null);
    }

    @Override
    public VariableModel update(VariableModel instance) throws Exception {

        VariableModel oldInstance = sparql.loadByURI(VariableModel.class, instance.getUri(), null, null);
        if (oldInstance == null) {
            throw new SPARQLInvalidURIException(instance.getUri());
        }

        // if the datatype has changed, check that they are no linked data
        if (!SPARQLDeserializers.compareURIs(oldInstance.getDataType(), instance.getDataType())) {
            int linkedDataNb = getLinkedDataNb(instance.getUri());
            if (linkedDataNb > 0) {
                throw new ForbiddenURIAccessException(instance.getUri(), "Variable datatype can't be updated. " + linkedDataNb + " linked data");
            }
        }
        return super.update(instance);
    }

    /*
        * Read each orderBy of orderByList and then :
        * <pre>
        *     - Append an ORDER BY {@link Expr} into the orderByExprList if the given orderBy field name
        *     is one of {@link #varsByVarName}
        *     - Else append the given orderBy into the unmatchingOrderByList
        * </pre>
        *
        * @param orderByList           the initial {@link OrderBy} list to read
        * @param orderByExprMap        the new list of ORDER BY {@link Expr}.
        *                              An {@link Expr} is created for each {@link OrderBy} which have a field name present in {@link #varsByVarName}
        * @param unmatchingOrderByList the new  list of {@link OrderBy} which doesn't have a field name present in {@link #varsByVarName}
        * @see OrderBy#getFieldName()
    */
    private void appendSpecificOrderBy(List<OrderBy> orderByList, Map<Expr, Order> orderByExprMap, List<OrderBy> unmatchingOrderByList) {

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

        for (OrderBy orderBy : orderByList) {
            /* We need to map field fieldName to _field_name : which is the variable name returned by SPARQLClassObjectMapper.getObjectNameVarName.
               Else because of OrderBY snake_case to camelCase transformation, the direct mapping between orderBy field and SPARQLClassObjectMapper.getObjectNameVarName() result is broken
               #TODO : append a cleaner way to do it. Here the solution works only because we known how SPARQLClassObjectMapper.getObjectNameVarName() works */

            String correspondingVarName = "_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, orderBy.getFieldName());
            Var correspondingVar = varsByVarName.get(correspondingVarName);
            if (correspondingVar == null) {
                unmatchingOrderByList.add(orderBy);
            } else {
                Expr orderByExpr = exprFactory.lcase(exprFactory.str(correspondingVar));
                orderByExprMap.put(orderByExpr, orderBy.getOrder());
            }
        }
    }


    /**
        * Search all variables with a name, a long name, an entity name,
        * an entity of interest name, a characteristic name, a method name, an unit name
        * corresponding with the given stringPattern.
        *
        * <br></br>
        * <br> The following SPARQL variables are used  : </br>
        * <pre>
        *     _entity_name : the name of the variable entity
        *     _entity_of_interest_name : the name of the variable entity of interest
        *     _characteristic_name : the name of the variable characteristic
        *     _method_name : the name of the variable method
        *     _unit_name : the name of the variable unit
        * </pre>
        * <p>
        * You can use them into the orderByList
        *
        * @return the list of {@link VariableModel} founds
        * @see VariableModel#getName()
        * @see VariableModel#getAlternativeName()
        * @see EntityModel#getName()
        * @see InterestEntityModel#getName()
        * @see CharacteristicModel#getName()
        * @see MethodModel#getName()
        * @see UnitModel#getName
    */
    public ListWithPagination<VariableModel> search(VariableSearchFilter filter) throws Exception {

        Set<URI> variableUriList = filter.isWithAssociatedData() ? dataDAO.getUsedVariablesByExpeSoDevice(filter.getUserModel(), filter.getExperiments(), filter.getObjects(), filter.getDevices()) : null;
        if(variableUriList != null && variableUriList.isEmpty()) {
            return new ListWithPagination<>(dataDAO.getUsedVariables(filter.getUserModel(), filter.getExperiments(), filter.getObjects(), null,  filter.getDevices()));
        }
        filter.setIncludedUris(variableUriList);

        Map<Expr, Order> orderByExprMap = new HashMap<>();
        List<OrderBy> newOrderByList = new LinkedList<>();
        appendSpecificOrderBy(filter.getOrderByList(), orderByExprMap, newOrderByList);

        ListWithPagination<VariableModel> listWithPagination =  sparql.searchWithPagination(
                defaultGraph,
                VariableModel.class,
                filter.getLang(),

                (SelectBuilder select) -> addFilter(select,filter,orderByExprMap),
                Collections.emptyMap(),
                result -> fetcher.getInstance(result, filter.getUserModel().getLanguage()),
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );

        if(! CollectionUtils.isEmpty(listWithPagination.getList()) && filter.isFetchSpecies()){
            fetchSpecies(listWithPagination.getList(), filter.getLang());
        }

        return listWithPagination;
    }

    @Override
    public List<VariableModel> getList(List<URI> uris, String lang) throws Exception {
        List<VariableModel> models =  super.getList(uris, lang);
        if(CollectionUtils.isEmpty(models)){
            return models;
        }

        fetchSpecies(models,lang);
        return models;
    }

    /**
     *
     * Fetch variables species with one SPARQL query
     * @param models variables
     * @param lang Language code, used to determine associated the translated label of the associated species
     *
     * @throws SPARQLException if SPARQL query evaluation fail
     *
     * @apiNote Example of generated SPARQL query
     *
     * <pre>
     * SELECT ?species  ?species_name
     * (GROUP_CONCAT(DISTINCT ?variable ; separator=',') AS ?variable__opensilex__concat)  WHERE {
     *
     *      GRAPH test:variables{
     *          ?variable a vocabulary:Variable .
    *           ?variable vocabulary:hasSpecies ?species
     *      }
     *      GRAPH test:germplasm {
     *          ?species rdfs:label ?species_name .
     *          FILTER langMatches(lang(?species_name), "en")
     *      }
     *      VALUES ?variable_group {  test:variable1 test:variable2  }
     * }  GROUP BY ?species ?species_name
     * </pre>
     *
     * #TODO : extract this method by developping a many-to-many relationship fetcher
     */
    public void fetchSpecies(List<VariableModel> models, String lang) throws SPARQLException, ParseException {

        if(models.isEmpty()){
            return;
        }

        Map<String, Integer> modelsIndexes = new PatriciaTrie<>();
        for (int i = 0; i < models.size(); i++) {
            VariableModel model = models.get(i);
            modelsIndexes.put(URIDeserializer.formatURIAsStr(model.getUri().toString()), i);
            model.setSpecies(new LinkedList<>());
        }

        Stream<URI> uris = models.stream().map(SPARQLResourceModel::getUri);

        Var variableVar = makeVar("variable");
        Var speciesVar = makeVar("species");
        Var speciesNameVar = makeVar("species_name");
        Node speciesGraph = sparql.getDefaultGraph(SpeciesModel.class);

        SelectBuilder query = new SelectBuilder()
                .addVar(speciesVar)
                .addVar(speciesNameVar)
                .addGraph(defaultGraph, new WhereBuilder()
                        .addWhere(variableVar, RDF.type, Oeso.Variable)
                        .addWhere(variableVar,Oeso.hasSpecies, speciesVar))
                .addGraph(speciesGraph, new WhereBuilder()
                        .addWhere(speciesVar, RDFS.label, speciesNameVar)
                        .addFilter(SPARQLQueryHelper.langFilter(speciesNameVar,lang))
                )
                .addGroupBy(speciesVar)
                .addGroupBy(speciesNameVar);

        // aggregate groups with variables
        SPARQLQueryHelper.appendGroupConcatAggregator(query, variableVar, true);

        SPARQLQueryHelper.addWhereUriValues(query, variableVar.getVarName(), uris, models.size());

        // retrieve variable class from Ontology
        URI speciesClass = URIDeserializer.formatURI(Oeso.Species.getURI());
        ClassModel speciesClassModel = SPARQLModule.getOntologyStoreInstance().getClassModel(speciesClass, null, lang);

        // read variables
        Stream<SPARQLResult> results = sparql.executeSelectQueryAsStream(query);
        results.forEach(result -> {

            SpeciesModel nestedModel = new SpeciesModel();
            nestedModel.setUri(URIDeserializer.formatURI(result.getStringValue(speciesVar.getVarName())));
            nestedModel.setLabel(new SPARQLLabel(result.getStringValue(speciesNameVar.getVarName()), lang));
            nestedModel.setType(speciesClass);
            nestedModel.setTypeLabel(speciesClassModel.getLabel());

            // get groups associated to the variable
            String joiningColumn = result.getStringValue(SPARQLQueryHelper.getConcatVarName(variableVar.getVarName()));
            String[] foreignKeys = joiningColumn.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);

            for (String key : foreignKeys) {
                String shortKey = URIDeserializer.formatURIAsStr(key);
                Integer groupIdx = modelsIndexes.get(shortKey);
                VariableModel variable = models.get(groupIdx);
                variable.getSpecies().add(nestedModel);
            }
        });

    }

    /**
     * Update the given SPARQL query by applying filter
     * @param select SPARQL query builder
     * @param filter Filter object
     * @param orderByExprMap map which contains custom order for entity, entity of interest, characteristic, method and unit
     */
    private void addFilter(SelectBuilder select, VariableSearchFilter filter, Map<Expr, Order> orderByExprMap) throws Exception {

        ExprFactory exprFactory = select.getExprFactory();
        Expr uriStrRegex = exprFactory.str(exprFactory.asVar(SPARQLResourceModel.URI_FIELD));

        if (!StringUtils.isEmpty(filter.getNamePattern())) {

            // append string regex matching on entity, entity of interest, characteristic, method and unit name
            Expr[] regexExprArray = varsByVarName.values().stream()
                    .map(nameVariable -> SPARQLQueryHelper.regexFilter(nameVariable.getVarName(), filter.getNamePattern()))
                    .toArray(Expr[]::new);

            // set the string regex filter on entity, entity of interest, characteristic, method, unit  name,long name and URI
            select.addFilter(
                    SPARQLQueryHelper.or(
                            regexExprArray,
                            SPARQLQueryHelper.regexFilter(SPARQLNamedResourceModel.NAME_FIELD, filter.getNamePattern()),
                            SPARQLQueryHelper.regexFilter(VariableModel.ALTERNATIVE_NAME_FIELD_NAME, filter.getNamePattern()),
                            SPARQLQueryHelper.regexFilter(uriStrRegex, filter.getNamePattern(), null)
                    ));

        }

        // append specific(s) ORDER BY based on entity, entity of interest, characteristic, method and unit
        orderByExprMap.forEach(select::addOrderBy);

        if (filter.getEntity() != null) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.ENTITY_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(filter.getEntity().toString()))));
        }

        if (filter.getInterestEntity() != null) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.ENTITY_OF_INTEREST_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(filter.getInterestEntity().toString()))));
        }

        if (filter.getCharacteristic() != null) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.CHARACTERISTIC_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(filter.getCharacteristic().toString()))));
        }

        if (filter.getMethod() != null) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.METHOD_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(filter.getMethod().toString()))));
        }

        if (filter.getUnit() != null) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.UNIT_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(filter.getUnit().toString()))));
        }

        if (filter.getGroup() != null) {
            select.addWhere(SPARQLDeserializers.nodeURI(filter.getGroup()), RDFS.member, makeVar(SPARQLResourceModel.URI_FIELD));
        }

        if (filter.getDataType() != null) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.DATA_TYPE_FIELD_NAME, NodeFactory.createURI(SPARQLDeserializers.getExpandedURI(filter.getDataType().toString()))));
        }

        if (!StringUtils.isEmpty(filter.getTimeInterval())) {
            select.addFilter(SPARQLQueryHelper.eq(VariableModel.TIME_INTERVAL_FIELD_NAME, filter.getTimeInterval()));
        }

        if (filter.getIncludedUris() != null) {
            SPARQLQueryHelper.addWhereUriValues(select, SPARQLResourceModel.URI_FIELD, filter.getIncludedUris());
        }

        if(!CollectionUtils.isEmpty(filter.getSpecies())){
            //  add ?uri vocabulary:hasSpecies ?species
            select.addWhere(makeVar(SPARQLResourceModel.URI_FIELD),Oeso.hasSpecies,makeVar(VariableModel.SPECIES_FIELD_NAME));

            // logical or -> filter ?species IN (:species_uri1 :species_uri2 )
            select.addFilter(SPARQLQueryHelper.inURIFilter(VariableModel.SPECIES_FIELD_NAME,filter.getSpecies()));

        }


    }
}

