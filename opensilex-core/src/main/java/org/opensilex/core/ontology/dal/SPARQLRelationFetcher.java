package org.opensilex.core.ontology.dal;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.AbstractPropertyModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.dal.PropertyModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Class used to optimize the fetching of multiple {@link SPARQLModelRelation} for a List of T, by retrieving
 * all those relations in one SPARQL query.
 * <p>
 * This class build a SPARQL query which use the same WHERE clause as the initial SPARQL query.
 * Then the query is updated by adding a triple and a var corresponding to each data/object property which is defined into the ontology for
 * the T type.
 * <p>
 * Then, results from the new SPARQL query are linked to the initial T list, in order to update element of T.
 *
 * @param <T> : a type of {@link SPARQLModelRelation}
 * @apiNote This implementation use {@link OntologyStore} in order to retrieve {@link ClassModel} and associated {@link OwlRestrictionModel}.
 * These restriction are used to determine which properties will be fetched by the SPARQL query
 */
public class SPARQLRelationFetcher<T extends SPARQLResourceModel> {

    private final SPARQLService sparql;

    private final Node graph;
    private final URI graphUri;
    private final SelectBuilder initialSelect;
    private final List<T> results;

    private final Set<PropertyModel> typesMonoValuedProperties;
    private final Map<URI, List<URI>> monoValuedPropertiesByType;
    private final Map<URI, List<String>> monoValuedPropertiesByTypeVarNames;

    private final Set<PropertyModel> typesMultiValuedProperties;
    private final Map<URI, List<URI>> multiValuedPropertiesByType;
    private final Map<URI, List<String>> multiValuedPropertiesByTypeVarNames;

    private final Pattern specialCharsPattern;

    public SPARQLRelationFetcher(SPARQLService sparql, Class<T> objectClass, Node graph, SelectBuilder initialSelect, List<T> results) throws URISyntaxException, SPARQLException {

        this.sparql = sparql;
        SPARQLClassObjectMapper<T> mapper = sparql.getMapperIndex().getForClass(objectClass);
        URI rootTypeURI = mapper.getClassAnalizer().getRdfTypeURI();

        this.graph = graph;
        this.graphUri = graph != null ? new URI(graph.getURI()) : null;
        this.initialSelect = initialSelect;
        this.results = results;

        //  #TODO optimize k types extraction from n results ?
        //   most of the time, k is much smaller than n, iteration could be costly with lot of results
        Set<URI> types = results.stream().map(SPARQLResourceModel::getType).collect(Collectors.toSet());

        Set<URI> managedProperties = new HashSet<>();
        for (Property property : mapper.getClassAnalizer().getManagedProperties()) {
            managedProperties.add(URIDeserializer.formatURI(property.toString()));
        }

        this.typesMonoValuedProperties = new HashSet<>();
        this.monoValuedPropertiesByType = new HashMap<>();
        this.monoValuedPropertiesByTypeVarNames = new HashMap<>();

        this.typesMultiValuedProperties = new HashSet<>();
        this.multiValuedPropertiesByType = new HashMap<>();
        this.multiValuedPropertiesByTypeVarNames = new HashMap<>();

        specialCharsPattern = Pattern.compile("[^A-Za-z0-9]");

        // use a local Property cache, in order to minimize the number of extra call to OntologyStore.getProperty().
        // Especially when multiple types share some property
        Map<String, AbstractPropertyModel<?>> localPropertyCache = new PatriciaTrie<>();

        for (URI type : types) {

            // get class from OntologyCache and compute stream of data/object property
            ClassModel classModel = SPARQLModule.getOntologyStoreInstance().getClassModel(type, rootTypeURI, OpenSilex.DEFAULT_LANGUAGE);

            // use class restriction in order to determine which property must be fetched
            Iterable<OwlRestrictionModel> restrictionIt = classModel.getRestrictionsByProperties()
                    .values()
                    .stream()
                    ::iterator;

            for (OwlRestrictionModel restriction : restrictionIt) {
                addPropertyModelToPropertiesIndexes(restriction, classModel, managedProperties, localPropertyCache);
            }
        }
    }

    private void addPropertyModelToPropertiesIndexes(OwlRestrictionModel restriction, ClassModel classModel, Set<URI> managedProperties, Map<String, AbstractPropertyModel<?>> localPropertyCache) throws SPARQLException {

        // try to get AbstractPropertyModel from local index
        AbstractPropertyModel<?> propertyModel = localPropertyCache.get(restriction.getOnProperty().toString());

        // if not present, then use global OntologyStore
        if (propertyModel == null) {
            propertyModel = SPARQLModule.getOntologyStoreInstance().getProperty(restriction.getOnProperty(), null, null, OpenSilex.DEFAULT_LANGUAGE);
            localPropertyCache.put(restriction.getOnProperty().toString(), propertyModel);

            if (propertyModel == null) {
                throw new SPARQLException("Unknown property URI " + restriction.getOnProperty());
            }
        }

        // use short uri (faster ?)
        URI formattedPropertyUri = SPARQLDeserializers.formatURI(propertyModel.getUri());

        // don't handle properties which are initially present into SPARQLModel (not managed by this class)
        if (!managedProperties.contains(formattedPropertyUri)) {

            String propertyName = specialCharsPattern.matcher(propertyModel.getName()).replaceAll("_");

            if (restriction.isList()) {
                typesMultiValuedProperties.add(propertyModel);
                multiValuedPropertiesByType.computeIfAbsent(classModel.getUri(), uri -> new ArrayList<>()).add(formattedPropertyUri);
                multiValuedPropertiesByTypeVarNames.computeIfAbsent(classModel.getUri(), uri -> new ArrayList<>()).add(propertyName);
            } else {
                typesMonoValuedProperties.add(propertyModel);
                monoValuedPropertiesByType.computeIfAbsent(classModel.getUri(), uri -> new ArrayList<>()).add(formattedPropertyUri);
                monoValuedPropertiesByTypeVarNames.computeIfAbsent(classModel.getUri(), uri -> new ArrayList<>()).add(propertyName);
            }
        }
    }

    protected SelectBuilder getRelationSelect(boolean isMultiValued) throws ParseException {

        Set<PropertyModel> properties = isMultiValued ? typesMultiValuedProperties : typesMonoValuedProperties;

        // compute properties SPARQL vars
        List<Var> propertiesVars = properties.stream()
                .map(property -> specialCharsPattern.matcher(property.getName()).replaceAll("_"))
                .map(SPARQLQueryHelper::makeVar)
                .collect(Collectors.toList());


        SelectBuilder select = initialSelect.clone();
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);

        // add ?uri var + each property name as SPARQL var
        select.setDistinct(true);
        select.getVars().clear();
        select.addVar(uriVar);
        propertiesVars.forEach(select::addVar);

        // get or create the graph into the query
        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        ElementGroup innerGraphElemGroup;
        if (graph != null) {
            innerGraphElemGroup = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(select.getWhereHandler().getClause(), graph);
        } else {
            innerGraphElemGroup = select.getWhereHandler().getClause();
        }

        Expr propertyBoundednessOurExpr = null;

        // append triple <?uri :property_uri ?property_name> for each property
        int propertyIdx = 0;
        for (PropertyModel property : properties) {

            Node propertyNode = SPARQLDeserializers.nodeURI(property.getUri());
            Var propertyVar = propertiesVars.get(propertyIdx++);
            Triple triple = new Triple(uriVar, propertyNode, propertyVar);

            ElementTriplesBlock elementTriple = new ElementTriplesBlock();
            elementTriple.addTriple(triple);

            /* append the triple as OPTIONAL even if the property is required
                here we supposed that any SPARQL model with any required prop is valid */
            innerGraphElemGroup.addElement(new ElementOptional(elementTriple));

            // build a BOUND filter : only fetch SPARQLModel with at least one relation
            Expr boundPropertyExpr = exprFactory.bound(propertyVar);
            if (propertyBoundednessOurExpr != null) {
                propertyBoundednessOurExpr = exprFactory.or(propertyBoundednessOurExpr, boundPropertyExpr);
            } else {
                propertyBoundednessOurExpr = boundPropertyExpr;
            }
        }

        if (propertyBoundednessOurExpr != null) {
            innerGraphElemGroup.addElementFilter(new ElementFilter(propertyBoundednessOurExpr));
        }

        if (!isMultiValued) {
            return select;
        }

        // Build upper query
        SelectBuilder multivaluedSelect = new SelectBuilder()
                .setDistinct(true)
                .addVar(uriVar)
                .addSubQuery(select)
                .addGroupBy(uriVar);

        // copy VALUES clause because if VALUES are inserted with SelectBuilder.addValueVar(var,values), then addSubQuery() don't copy VALUES from SelectBuilder.getWhereHandler().getValuesMap()
        // addSubQuery() work if VALUES are inserted with SelectBuilder.addWhereValueVar(var,values), in this case VALUES are copied from SelectBuilder.getValuesHandler()
        multivaluedSelect.getValuesHandler().addAll(select.getValuesHandler());

        // Add projection to the multivalued field with the GROUP_CONCAT aggregator
        for (Var propertyVar : propertiesVars) {
            SPARQLQueryHelper.appendGroupConcatAggregator(multivaluedSelect, propertyVar, true);
        }
        return multivaluedSelect;
    }

    private void updateProperties(Map<String, T> modelsByUris, SelectBuilder select, BiConsumer<SPARQLResult, T> resultToModelConsumer) throws SPARQLException {

        sparql.executeSelectQueryAsStream(select).forEach(result -> {

            // Get the model corresponding to the result
            String rowUri = URIDeserializer.formatURIAsStr(result.getStringValue(SPARQLResourceModel.URI_FIELD));
            T initialModel = modelsByUris.get(rowUri);

            // ensure model is not null, since the query use a FILTER in case of no associated values
            if (initialModel != null) {
                resultToModelConsumer.accept(result, initialModel);
            }
        });
    }

    public void updateModels() throws SPARQLException, ParseException {

        if (typesMonoValuedProperties.isEmpty() && typesMultiValuedProperties.isEmpty()) {
            return;
        }

        // compute index between models and models URIs in order to associate in O(n) time complexity, the n results from selectWithMultivalued
        Map<String, T> modelsByUris = new PatriciaTrie<>();
        results.forEach(result ->
                modelsByUris.put(URIDeserializer.formatURIAsStr(result.getUri().toString()), result)
        );

        if (!typesMonoValuedProperties.isEmpty()) {
            updateProperties(modelsByUris, getRelationSelect(false), this::updateMonoValued);
        }

        if (!typesMultiValuedProperties.isEmpty()) {
            updateProperties(modelsByUris, getRelationSelect(true), this::updateMultiValued);
        }
    }

    protected void updateMonoValued(SPARQLResult result, T initialModel) {

        List<URI> typeProperties = this.monoValuedPropertiesByType.get(initialModel.getType());
        List<String> propertiesNames = this.monoValuedPropertiesByTypeVarNames.get(initialModel.getType());

        for (int i = 0; i < typeProperties.size(); i++) {

            String propertyVarName = propertiesNames.get(i);
            String relationValue = result.getStringValue(propertyVarName);

            if (!StringUtils.isEmpty(relationValue)) {
                URI propertyUri = typeProperties.get(i);
                initialModel.addRelation(graphUri, propertyUri, null, relationValue);
            }
        }
    }

    protected void updateMultiValued(SPARQLResult result, T initialModel) {

        List<URI> typeProperties = this.multiValuedPropertiesByType.get(initialModel.getType());
        List<String> propertiesNames = this.multiValuedPropertiesByTypeVarNames.get(initialModel.getType());

        for (int i = 0; i < typeProperties.size(); i++) {

            String propertyVarName = SPARQLQueryHelper.getConcatVarName(propertiesNames.get(i));
            String unparsedValue = result.getStringValue(propertyVarName);

            if (!StringUtils.isEmpty(unparsedValue)) {
                String[] values = unparsedValue.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);
                if (values.length > 0) {

                    URI propertyUri = typeProperties.get(i);
                    for (String value : values) {
                        initialModel.addRelation(graphUri, propertyUri, null, value);
                    }
                }

            }
        }
    }

}
