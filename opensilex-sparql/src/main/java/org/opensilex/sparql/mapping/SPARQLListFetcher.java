package org.opensilex.sparql.mapping;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcatDistinct;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * <pre>
 * Optimization of multi-valued relations fetching for a {@link SPARQLResourceModel}.
 * One SPARQL query with GROUP_CONCAT aggregator is executed for retrieving one or more multi-valued properties.
 *
 * Instead of passing URI returned to the initial query, this class reuse the initial query body.
 * This allow to don't re-send these URI to the multi-valued query, and so to limit the number of I/O with the triplestore.
 * This also make the query size not dependent of the result number and ensure that query will be
 * parsable and can be send to the triplestore
 *
 *
 * Notes : this approach is optimized in most of the case, since I/O between OpenSILEX
 * and the database are often the main bottleneck.
 * But if the bottleneck is during query evaluation (depending of the query complexity, of the quality of the optimisation plan
 * made by the triplestore and of triple store I/O performance), this approach could be less efficient than just adding VALUES clause on lot
 * of URIs.
 * </pre>
 *
 * @param <T> the SPARQL model class
 * @author rcolin
 * @see <a href="https://www.w3.org/TR/sparql11-query/">SPARQL GROUP_CONCAT (section 18.5.1.7)</a>
 */
public class SPARQLListFetcher<T extends SPARQLResourceModel> {

    private final SPARQLService sparql;
    private final SPARQLClassObjectMapper<T> mapper;

    private final Node graph;

    /**
     * The list of initial results. This class will update each item from results, by
     * setting all multivalued properties from the input fieldToFetch Set
     */
    private final List<T> results;

    /**
     * Associate to each concat var name, the corresponding {@link SPARQLClassObjectMapper}
     */
    private final Map<String, SPARQLClassObjectMapper<?>> objectMappersByConcatVarName;

    /**
     * Association between field and concat var name
     */
    private final LinkedHashMap<Field, String> concatVarNameByFields;

    /**
     * List of setters, in the same order as concatVarNameByFields
     */
    private final List<Method> listSetters;

    /**
     * Associate to each concat var name, the corresponding {@link SPARQLDeserializer}
     */
    private final Map<String, SPARQLDeserializer<?>> deserializersByConcatFields;

    private static final String GROUP_CONCAT_SEPARATOR = ",";

    private static final int MAX_RESULTS_SIZE = 65536;

    /**
     * Reserved keyword used for GROUP_CONCAT var naming
     */
    private static final String CONCAT_VAR_SUFFIX = "__opensilex__concat";

    /**
     * @param sparql         {@link SPARQLService} used to run additional SPARQL query
     * @param objectClass    the SPARQL model class
     * @param graph          graph
     * @param fieldsToFetch  The multivalued fields to fetch. Note if redundant element are present in the collection, then the element is only added one inside the generated SPARQL query
     *                       <pre>
     *                                                                                              For each field, indicate if the <b> ?uri,:multi_valued_property,?value> </b> triple must be added or not to the query.
     *                                                                                         </pre>
     * @param initialResults the List of {@link SPARQLResourceModel}, each item from list will be updated by adding values for multivalued properties
     * @see SelectBuilder#build()
     */
    public SPARQLListFetcher(SPARQLService sparql,
                             Class<T> objectClass,
                             Node graph,
                             Collection<String> fieldsToFetch,
                             List<T> initialResults
    ) throws SPARQLDeserializerNotFoundException, SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {

        this.sparql = sparql;
        this.mapper = sparql.getMapperIndex().getForClass(objectClass);
        this.graph = graph;

        Objects.requireNonNull(initialResults);
        if (initialResults.size() >= MAX_RESULTS_SIZE) {
            throw new IllegalArgumentException("initialResults.size() : " + initialResults.size() + " >= to limit : " + MAX_RESULTS_SIZE);
        }
        this.results = initialResults;

        if (CollectionUtils.isEmpty(fieldsToFetch)) {
            throw new IllegalArgumentException("Null or empty fieldsToFetch");
        }
        concatVarNameByFields = new LinkedHashMap<>();

        for (String fieldName : fieldsToFetch) {
            Field field = mapper.classAnalyzer.getFieldFromName(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Unknown custom field " + fieldName + " from SPARQL model : " + mapper.getObjectClass().getName());
            }
            if (mapper.classAnalyzer.getDataListPropertyByField(field) == null && mapper.classAnalyzer.getObjectListPropertyByField(field) == null) {
                throw new IllegalArgumentException("Custom field" + fieldName + " is not a multi-valued data/object property from SPARQL model : " + mapper.getObjectClass().getName());
            }
            concatVarNameByFields.put(field, field.getName() + CONCAT_VAR_SUFFIX);
        }

        // build list Deserializer and setter method in order to update each model
        listSetters = new ArrayList<>(concatVarNameByFields.size());
        deserializersByConcatFields = new HashMap<>();
        objectMappersByConcatVarName = new HashMap<>();

        for (Map.Entry<Field, String> entry : concatVarNameByFields.entrySet()) {
            Field field = entry.getKey();
            String concatFieldName = entry.getValue();

            listSetters.add(mapper.classAnalyzer.getSetterFromField(field));
            Class<?> listGenericType = ClassUtils.getGenericTypeFromField(field);

            try {
                // check if the type is a SPARQL model type known by the service
                // if so, use the URI deserializer and a specific object mapper for new instance creation
                sparql.getMapperIndex().getForClass(listGenericType);
                objectMappersByConcatVarName.put(concatFieldName, sparql.getMapperIndex().getForClass(listGenericType));

                // else just use the type deserializer
            } catch (SPARQLMapperNotFoundException e) {
                deserializersByConcatFields.put(concatFieldName, SPARQLDeserializers.getForClass(listGenericType));
            }
        }
    }

    /**
     * Update {@link #results} by setting all multi-valued properties
     *
     * @throws SPARQLException          if some error is encountered during SPARQL query evaluation
     * @throws IllegalArgumentException if {@link SPARQLListFetcher#results} contains two models with the same URI.
     *                                  This exception if throw because the two SPARQL query used to match list attributes must work on the same unique results
     */
    public void updateModels() throws SPARQLException {

        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        // get a new SELECT with multivalued property fetching according to the given settings
        SelectBuilder selectWithMultivalued = getSelect();

        // compute index between models and models URIs in order to associate in O(n) time complexity, the n results from selectWithMultivalued
        // We need to maintain index since, there
        Map<String, T> modelsByUris = new PatriciaTrie<>();
        int i = 0;
        for (T model : results) {
            String modelURI = URIDeserializer.formatURIAsStr(model.getUri().toString());
            if (modelsByUris.containsKey(modelURI)) {
                throw new IllegalArgumentException(String.format("Multiple results with the same URI (%s) at index %d", modelURI, i));
            }
            modelsByUris.put(modelURI, model);
            i++;
        }


        // execute the query and update model with all data/object list property
        sparql.executeSelectQueryAsStream(selectWithMultivalued).forEach(result -> {
            try {

                // Get the model corresponding to the result
                String rowUri = URIDeserializer.formatURIAsStr(result.getStringValue(SPARQLResourceModel.URI_FIELD));
                SPARQLResourceModel initialModel = modelsByUris.get(rowUri);
                update(result, initialModel);

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /**
     * @return a new SelectBuilder which include multivalued fields to fetch.
     * @apiNote Example: considering the following initial query:
     *
     * <pre>
     * {@code
     * SELECT DISTINCT  ?uri ?rdfType ?rdfTypeName ?creator ?isInstant ?description ?start ?end ?_start__timestamp ?_end__timestamp
     * WHERE{
     *    ?rdfType (rdfs:subClassOf)* oeev:Event
     *     OPTIONAL
     *       { ?rdfType  rdfs:label  ?rdfTypeName
     *         FILTER ( langMatches(lang(?rdfTypeName), "en") || langMatches(lang(?rdfTypeName), "") )
     *       }
     *     ?rdfType (rdfs:subClassOf)* oeev:Move
     *     GRAPH <http://www.opensilex.org/set/event>{
     *     ?uri  a               ?rdfType ;
     *           oeev:isInstant  ?isInstant
     *         OPTIONAL
     *           { ?uri  dc:creator  ?creator}
     *         OPTIONAL
     *           { ?uri  rdfs:comment  ?description}
     *         OPTIONAL
     *           { ?uri    time:hasBeginning     ?start .
     *             ?start  time:inXSDDateTimeStamp  ?_start__timestamp}
     *         OPTIONAL
     *           { ?uri  time:hasEnd           ?end .
     *             ?end  time:inXSDDateTimeStamp  ?_end__timestamp
     *           }
     *       }
     *     FILTER ( ! isBlank(?uri) )
     *   }
     *   ORDER BY DESC(?_end__timestamp)
     *   LIMIT   1000
     * }</pre>
     *
     * <ul><li>If we want to fetch the multi-valued properties <b>oeev:concerns</b> on the field <b>targets</b>, then the new query will be : </li></ul>
     *
     * <pre>
     * {@code
     *
     * SELECT DISTINCT ?uri (GROUP_CONCAT(DISTINCT ?targets ; separator=',') AS ?targets__opensilex__concat)
     * WHERE{
     *    ?uri  oeev:concerns  ?targets
     * }
     * VALUES ?uri ( :event_1 :event_2)
     * LIMIT 1000
     * GROUP BY ?uri
     *
     * }</pre>
     */
    protected SelectBuilder getSelect() {

        Var uriVar = mapper.getURIFieldVar();

        // copy the initial SELECT and replace vars
        SelectBuilder multivaluedSelect = new SelectBuilder()
                .addVar(uriVar) // add ?uri var for matching
                .setDistinct(true)
                .addGroupBy(uriVar);   // append the GROUP BY in order to support the GROUP_CONCAT aggregator

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        ElementGroup graphElemGroup;
        if (graph != null) {
            graphElemGroup = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(multivaluedSelect.getWhereHandler().getClause(), graph);
        } else {
            graphElemGroup = multivaluedSelect.getWhereHandler().getClause();
        }

        // append var and BGP for each multivalued field
        concatVarNameByFields.keySet().forEach(field -> {

            Property property = mapper.classAnalyzer.getDataListPropertyByField(field);
            if (property == null) {
                property = mapper.classAnalyzer.getObjectListPropertyByField(field);
            }

            Var fieldVar = makeVar(field.getName());
            try {
                // Add projection to the multivalued field variable with the GROUP_CONCAT aggregator
                // build the expression ( GROUP_CONCAT(DISTINCT ?multivalued_field ; separator=',') AS ?multivalued_field__opensilex_concat) into outer select
                Aggregator groupConcat = new AggGroupConcatDistinct(exprFactory.asExpr(fieldVar), GROUP_CONCAT_SEPARATOR);
                Var fieldConcatVar = makeVar(concatVarNameByFields.get(field));
                multivaluedSelect.addVar(groupConcat.toString(), fieldConcatVar);

            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }

            // add the BGP (?uri <field_to_fetch_property> ?field_to_fetch)
            Triple triple = mapper.classAnalyzer.isReverseRelation(field)
                    ? new Triple(fieldVar, property.asNode(), uriVar)
                    : new Triple(uriVar, property.asNode(), fieldVar);

            if (mapper.classAnalyzer.isOptional(field)) {
                ElementTriplesBlock elementTriple = new ElementTriplesBlock();
                elementTriple.addTriple(triple);
                graphElemGroup.addElement(new ElementOptional(elementTriple));
            } else {
                graphElemGroup.addTriplePattern(triple);
            }
        });

        SPARQLQueryHelper.addWhereUriStringValues(
                multivaluedSelect,
                SPARQLResourceModel.URI_FIELD,
                results.stream().map(model -> model.getUri().toString()),
                true,
                results.size()
        );
        return multivaluedSelect;
    }

    protected void update(SPARQLResult result, SPARQLResourceModel initialModel) throws Exception {

        int fieldIndex = 0;

        for (String concatFieldName : concatVarNameByFields.values()) {
            // parse multivalued property grouped by a separator
            String parsedValue = result.getStringValue(concatFieldName);

            List<Object> listPropertyValues;

            if (StringUtils.isEmpty(parsedValue)) {
                // An empty string should be treated as an empty list (String::split on an empty string returns
                // a singleton array containing an empty string, instead of an empty array, that's why this case
                // is handled separately)
                listPropertyValues = Collections.emptyList();
            } else {
                String[] stringValues = parsedValue.split(GROUP_CONCAT_SEPARATOR);
                SPARQLClassObjectMapper<?> objectMapper = objectMappersByConcatVarName.get(concatFieldName);

                listPropertyValues = new ArrayList<>(stringValues.length);

                for (String strValue : stringValues) {
                    // if value correspond to some object URI, then create new object
                    if (objectMapper != null) {
                        SPARQLResourceModel object = objectMapper.createInstance(new URI(SPARQLDeserializers.formatURI(strValue)));
                        listPropertyValues.add(object);
                    } else {
                        // else just deserialize value
                        listPropertyValues.add(deserializersByConcatFields.get(concatFieldName).fromString(strValue));
                    }
                }
            }

            // update model
            Method setter = listSetters.get(fieldIndex);
            setter.invoke(initialModel, listPropertyValues);
            fieldIndex++;
        }
    }

}
