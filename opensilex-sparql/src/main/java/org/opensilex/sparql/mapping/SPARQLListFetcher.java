package org.opensilex.sparql.mapping;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggGroupConcat;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Optimization of multi-valued relations fetching for a {@link SPARQLResourceModel}.
 * One SPARQL query with GROUP_CONCAT aggregator is executed for retrieving multi-valued properties.
 * The SPARQL query use the same body as the initial SPARQL query (including additional filters).
 *
 * @param <T> the SPARQL model class
 */
public class SPARQLListFetcher<T extends SPARQLResourceModel> {

    private final SPARQLService sparql;
    private final SPARQLClassObjectMapper<T> mapper;
    private final Node graph;
    private final Map<String, Boolean> fieldsToFetch;

    private final SelectBuilder initialSelect;
    private final List<T> results;

    private final Map<String,SPARQLClassObjectMapper<?>> objectMappers;

    private final LinkedHashMap<Field, String> concatVarNameByFields;
    private final List<Method> listSetters;
    private final Map<String,SPARQLDeserializer<?>> deserializersByConcatFields;

    private static final String GROUP_CONCAT_SEPARATOR = ",";
    private static final String CONCAT_VAR_SUFFIX = "__concat";

    /**
     * @param sparql               {@link SPARQLService} used to run additional SPARQL query
     * @param objectClass           the SPARQL model class
     * @param graph                 graph
     * @param fieldsToFetch The multi-valued fields to fetch
     *                              <pre>
     *                              For each field, indicate if the <b> ?uri,:multi_valued_property,?value> </b> triple must be added or not to the query.
     *                              </pre>
     * @param initialSelect         the initial builder
     * @param initialResults        the List of {@link SPARQLResourceModel}, each item from list will be updated by adding values for multi-valued properties
     */
    public SPARQLListFetcher(SPARQLService sparql,
                             Class<T> objectClass,
                             Node graph,
                             Map<String, Boolean> fieldsToFetch,
                             SelectBuilder initialSelect,
                             List<T> initialResults) throws SPARQLDeserializerNotFoundException, SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {

        this.sparql = sparql;
        this.mapper = sparql.getMapperIndex().getForClass(objectClass);
        this.graph = graph;
        this.initialSelect = initialSelect;
        this.results = initialResults;

        if (fieldsToFetch == null || fieldsToFetch.isEmpty()) {
            throw new IllegalArgumentException("Null or empty fieldsToFetch");
        }

        this.fieldsToFetch = fieldsToFetch;
        concatVarNameByFields = new LinkedHashMap<>();

        for (String fieldName : fieldsToFetch.keySet()) {
            Field field = mapper.classAnalizer.getFieldFromName(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Unknown custom field" + fieldName + "from SPARQL model : " + mapper.getObjectClass().getName());
            }
            if(mapper.classAnalizer.getDataListPropertyByField(field) == null && mapper.classAnalizer.getObjectListPropertyByField(field) == null){
                throw new IllegalArgumentException("Custom field" + fieldName + " is not a multi-valued data/object property from SPARQL model : " + mapper.getObjectClass().getName());
            }
            concatVarNameByFields.put(field, field.getName() + CONCAT_VAR_SUFFIX);
        }

        // build list Deserializer and setter method in order to update each model
        listSetters = new ArrayList<>(concatVarNameByFields.size());
        deserializersByConcatFields = new HashMap<>();
        objectMappers = new HashMap<>();

        for (Map.Entry<Field,String> entry : concatVarNameByFields.entrySet()) {
            Field field = entry.getKey();
            String concatFieldName = entry.getValue();

            listSetters.add(mapper.classAnalizer.getSetterFromField(field));
            Class<?> listGenericType = ClassUtils.getGenericTypeFromField(field);

            try {
                // check if the type is a SPARQL model type known by the service
                // if so, use the URI deserializer and a specific object mapper for new instance creation
                sparql.getMapperIndex().getForClass(listGenericType);
                objectMappers.put(concatFieldName,sparql.getMapperIndex().getForClass(listGenericType));

                // else just use the type deserializer
            }catch (SPARQLMapperNotFoundException e){
                deserializersByConcatFields.put(concatFieldName,SPARQLDeserializers.getForClass(listGenericType));
            }
        }
    }

    public void updateModels() throws SPARQLException {

        if (results.isEmpty() || concatVarNameByFields.isEmpty()) {
            return;
        }

        // get a new SELECT with multivalued property fetching according to the given settings
        SelectBuilder selectWithMultivalued = getSelect();

        AtomicInteger readResultNb = new AtomicInteger(0);

        // execute the query and update model with all data/object list property
        sparql.executeSelectQueryAsStream(selectWithMultivalued).forEach(result -> {
            try {

                if(readResultNb.get() > results.size()){
                    throw new IllegalArgumentException("Too large SPARQL results size");
                }

                // Get the model corresponding to the result
                SPARQLResourceModel initialModel = results.get(readResultNb.getAndIncrement());
                update(result, initialModel);

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    protected SelectBuilder getSelect() {

        Var uriVar = mapper.getURIFieldVar();
        SelectBuilder outerSelect = new SelectBuilder().addVar(uriVar);

        // copy the initial SELECT and replace vars
        SelectBuilder innerSelect = initialSelect.clone();
        innerSelect.setDistinct(false);
        innerSelect.getVars().clear();
        innerSelect.addVar(uriVar);

        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        ElementGroup innerGraphElemGroup;
        if(graph != null){
            innerGraphElemGroup = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(innerSelect.getWhereHandler().getClause(), graph);
        }else{
            innerGraphElemGroup = innerSelect.getWhereHandler().getClause();
        }

        // append var and BGP for each multivalued field
        concatVarNameByFields.keySet().forEach(field -> {

            Property property = mapper.classAnalizer.getDataListPropertyByField(field);
            if(property == null){
                property = mapper.classAnalizer.getObjectListPropertyByField(field);
            }

            Var fieldVar = makeVar(field.getName());
            try {
                // Add projection to the multivalued field variable with the GROUP_CONCAT aggregator
                // build the expression ( GROUP_CONCAT(?multivalued_field ; separator=',') AS ?multivalued_field__concat)
                AggGroupConcat groupConcat = new AggGroupConcat(exprFactory.asExpr(fieldVar), GROUP_CONCAT_SEPARATOR);
                Var fieldConcatVar = makeVar(concatVarNameByFields.get(field));

                outerSelect.addVar(groupConcat.toString(), fieldConcatVar);
                innerSelect.addVar(fieldVar);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }

            boolean appendTriple = fieldsToFetch.get(field.getName());
            if (appendTriple) {
                // add the BGP (?uri <field_to_fetch_property> ?field_to_fetch)
                Triple triple = new Triple(uriVar, property.asNode(), fieldVar);

                if (mapper.classAnalizer.isOptional(field)) {
                    ElementTriplesBlock elementTriple = new ElementTriplesBlock();
                    elementTriple.addTriple(triple);
                    innerGraphElemGroup.addElement(new ElementOptional(elementTriple));
                } else {
                    innerGraphElemGroup.addTriplePattern(triple);
                }
            }
        });

        SelectBuilder select = outerSelect
                .addSubQuery(innerSelect)
                .addGroupBy(uriVar);     // append the GROUP BY in order to support the GROUP_CONCAT aggregator

        // add VALUES manually, else VALUES clause are not copied from clone()
        select.getValuesHandler().addAll(innerSelect.getValuesHandler());
        return select;

    }

    protected void update(SPARQLResult result, SPARQLResourceModel initialModel) throws Exception {

        int fieldIndex = 0;

        for(String concatFieldName : concatVarNameByFields.values()) {

            // parse multivalued property grouped by a separator
            String parsedValue = result.getStringValue(concatFieldName);
            if (StringUtils.isEmpty(parsedValue)) {
                continue;
            }
            String[] stringValues = parsedValue.split(GROUP_CONCAT_SEPARATOR);
            if (stringValues.length == 0) {
                continue;
            }

            SPARQLClassObjectMapper<?> objectMapper = objectMappers.get(concatFieldName);

            List<Object> listPropertyValues = new ArrayList<>(stringValues.length);

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

            // update model
            Method setter = listSetters.get(fieldIndex);
            setter.invoke(initialModel, listPropertyValues);
            fieldIndex++;
        }
    }

}
