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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Optimization of multi-valued relations fetching for a {@link SPARQLResourceModel}.
 * One SPARQL query with GROUP_CONCAT aggregator is executed for retrieving multi-valued properties.
 * The SPARQL query use the same body as the initial SPARQL query (including additional filters).
 *
 * @param <T> the SPARQL model class
 */
public class SPARQLDataListFetcher<T extends SPARQLResourceModel> {

    private final SPARQLService service;
    private final SPARQLClassObjectMapper<T> mapper;
    private final SelectBuilder initialSelect;
    private final List<T> results;
    private final Node graph;
    private final Map<String, Boolean> dataListFieldsToFetch;

    private final LinkedHashMap<Field, String> concatVarNameByFields;
    private final List<Method> dataListSetters;
    private final List<SPARQLDeserializer<?>> deserializers;

    private final static String GROUP_CONCAT_SEPARATOR = ",";

    /**
     * @param service               {@link SPARQLService} used to run additional SPARQL query
     * @param objectClass           the SPARQL model class
     * @param graph                 graph
     * @param dataListFieldsToFetch The multi-valued fields to fetch
     *                              <pre>
     *                              For each field, indicate if the <b> ?uri,:multi_valued_property,?value> </b> triple must be added or not to the query.
     *                              </pre>
     * @param initialSelect         the initial builder
     * @param initialResults        the List of {@link SPARQLResourceModel}, each item from list will be updated by adding values for multi-valued properties
     */
    public SPARQLDataListFetcher(SPARQLService service,
                                 Class<T> objectClass,
                                 Node graph,
                                 Map<String, Boolean> dataListFieldsToFetch,
                                 SelectBuilder initialSelect,
                                 List<T> initialResults) throws SPARQLDeserializerNotFoundException, SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {

        this.service = service;
        this.mapper = service.getMapperIndex().getForClass(objectClass);
        this.graph = graph;
        this.initialSelect = initialSelect;
        this.results = initialResults;

        if (dataListFieldsToFetch == null || dataListFieldsToFetch.isEmpty()) {
            throw new IllegalArgumentException("Null or empty dataListFieldsToFetch");
        }

        this.dataListFieldsToFetch = dataListFieldsToFetch;
        concatVarNameByFields = new LinkedHashMap<>();

        for (String fieldName : dataListFieldsToFetch.keySet()) {
            Field field = mapper.classAnalizer.getFieldFromName(fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Unknown custom field" + fieldName + "from SPARQL model : " + mapper.getObjectClass().getName());
            }
            concatVarNameByFields.put(field, field.getName() + "__concat");
        }

        // build list Deserializer and setter method in order to update each model
        dataListSetters = new ArrayList<>(concatVarNameByFields.size());
        deserializers = new ArrayList<>(concatVarNameByFields.size());

        for (Field field : concatVarNameByFields.keySet()) {
            dataListSetters.add(mapper.classAnalizer.getSetterFromField(field));

            Class<?> listGenericType = ClassUtils.getGenericTypeFromField(field);
            deserializers.add(SPARQLDeserializers.getForClass(listGenericType));
        }
    }

    public void updateModels() throws SPARQLException {

        if (results.isEmpty() || concatVarNameByFields.isEmpty()) {
            return;
        }

        // get a new SELECT with multivalued property fetching according to the given settings
        SelectBuilder selectWithMultivalued = getSelect();

        String uriField = mapper.getURIFieldName();
        AtomicInteger readResultNb = new AtomicInteger(0);

        // execute the query and update model with all datalist property
        service.executeSelectQueryAsStream(selectWithMultivalued).forEach(result -> {
            try {

                if(readResultNb.get() >= results.size()){
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
        ElementGroup innerGraphElemGroup = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(innerSelect.getWhereHandler().getClause(), graph);

        // append var and BGP for each multivalued field
        concatVarNameByFields.keySet().forEach(field -> {

            Property property = mapper.classAnalizer.getDataListPropertyByField(field);
            Var fieldVar = makeVar(field.getName());

            try {
                // Add projection to the multivalued field variable with the GROUP_CONCAT aggregator
                // build the expression ( GROUP_CONCAT(?multivalued_field ; separator=',') AS ?multivalued_field__concat)
                AggGroupConcat groupConcat = new AggGroupConcat(exprFactory.asExpr(fieldVar), GROUP_CONCAT_SEPARATOR);
                Var fieldConcatVar = makeVar(field.getName() + "__concat");

                outerSelect.addVar(groupConcat.toString(), fieldConcatVar);
                innerSelect.addVar(fieldVar);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }

            Boolean appendTriple = dataListFieldsToFetch.get(field.getName());
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

        return outerSelect.addSubQuery(innerSelect)
                .addGroupBy(uriVar); // append the GROUP BY in order to support the GROUP_CONCAT aggregator

    }

    protected void update(SPARQLResult result, SPARQLResourceModel initialModel) throws Exception {

        int fieldIndex = 0;

        for (String concatFieldName : concatVarNameByFields.values()) {

            // parse multivalued property grouped by a separator
            String parsedValue = result.getStringValue(concatFieldName);
            if (StringUtils.isEmpty(parsedValue)) {
                continue;
            }
            String[] stringValues = parsedValue.split(GROUP_CONCAT_SEPARATOR);
            if (stringValues.length == 0) {
                continue;
            }

            // deserialize values
            SPARQLDeserializer<?> deserializer = deserializers.get(fieldIndex);
            List<Object> dataListPropValues = new ArrayList<>(stringValues.length);
            for (String strValue : stringValues) {
                dataListPropValues.add(deserializer.fromString(strValue));
            }

            // update model
            Method dataListSetter = dataListSetters.get(fieldIndex);
            dataListSetter.invoke(initialModel, dataListPropValues);

            fieldIndex++;
        }
    }

}
