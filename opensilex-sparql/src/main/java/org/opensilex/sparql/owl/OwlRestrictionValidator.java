/*******************************************************************************
 *                         OwlRestrictionValidator.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.owl;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public abstract class OwlRestrictionValidator<T extends ValidationContext> {

    protected final SPARQLService sparql;
    protected final OntologyStore ontologyStore;

    protected boolean isValid;
    protected Map<String, List<String>> valuesByTypeToCheck;
    protected Map<String, Map<String, List<T>>> contextsByValuesAndTypes;

    protected OwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore) {
        this.sparql = sparql;
        this.ontologyStore = ontologyStore;

        isValid = true;
        valuesByTypeToCheck = new PatriciaTrie<>();
        contextsByValuesAndTypes = new PatriciaTrie<>();
    }

    protected abstract void addUnknownPropertyError(URI classURI, T context);

    protected abstract void addInvalidValueError(String customError, T context);

    protected abstract void addMissingRequiredValue(URI classURI, T context);

    protected abstract void addInvalidDatatypeError(URI datatype, T context, String customError);

    protected abstract void addInvalidURIError(T context, String customError);


    public void validateModel(ClassModel classModel, SPARQLResourceModel model, Supplier<T> contextSupplier) {

        // #TODO update relations model by indexing them by properties ?

        Map<String, List<String>> valuesByProperties = new PatriciaTrie<>();
        for (SPARQLModelRelation relation : model.getRelations()) {
            List<String> values = valuesByProperties.computeIfAbsent(relation.getProperty().toString(), key -> new LinkedList<>());
            values.add(relation.getValue());
        }


        for (OwlRestrictionModel restriction : classModel.getRestrictionsByProperties().values()) {

            String propertyStr = restriction.getOnProperty().toString();
            List<String> values = valuesByProperties.get(propertyStr);

            // check that all required restriction are filled
            if (restriction.isRequired() && values == null) {
                T validationContext = contextSupplier.get();
                validationContext.setValue(null);
                validationContext.setProperty(propertyStr);

                addMissingRequiredValue(classModel.getUri(), validationContext);
            }

            if (restriction.isList()) {
                // check min and max cardinality
//                if(restriction.getMinCardinality() != null && restriction.getMinCardinality() > 1){
//                    if(values == null || values.size() <= 1){
//                        // add custom message
//                        addMissingRequiredValue(null, URI.create(propertyStr), classModel.getUri(),contextSupplier.get());
//                    }
//                }
            } else {
                if (values != null && values.size() > 1) {
                    T validationContext = contextSupplier.get();
                    validationContext.setValue(values.get(1));
                    validationContext.setProperty(propertyStr);
                    addInvalidValueError("Multiple values for mono-valued property", validationContext);
                }
            }

        }
    }


    protected void validateModelRelation(URI graph, ClassModel classModel, SPARQLResourceModel model, URI property, String value, OwlRestrictionModel restriction, Supplier<T> contextSupplier) {

        boolean hasValue = ! StringUtils.isEmpty(value);

        // value for an unknown restriction
        if (restriction == null && hasValue) {
            isValid = false;
            T validationContext = contextSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            addUnknownPropertyError(classModel.getUri(), validationContext);
        } else if (!hasValue) {
            if (restriction != null && restriction.isRequired()) {
                isValid = false;
                T validationContext = contextSupplier.get();
                validationContext.setValue(value);
                validationContext.setProperty(property.toString());
                addMissingRequiredValue(classModel.getUri(), validationContext);
            }
            // no restriction and no value
        } else {
            if (classModel.isDatatypePropertyRestriction(property)) {
                validateDataTypePropertyValue(graph, model, value, property, restriction, contextSupplier);
            } else if (classModel.isObjectPropertyRestriction(property)) {
                validateObjectPropertyValue(graph, model, value, property, restriction, contextSupplier);
            }
        }

    }

    protected void validateDataTypePropertyValue(URI graph, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction, Supplier<T> contextSupplier) {
        try {
            SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getOnDataRange());
            if (!deserializer.validate(value)) {
                isValid = false;
                T validationContext = contextSupplier.get();
                validationContext.setValue(value);
                validationContext.setProperty(property.toString());
                addInvalidDatatypeError(restriction.getOnDataRange(), contextSupplier.get(), null);
            }
            if (isValid) {
                model.addRelation(graph, property, deserializer.getClassType(), value);
            }

        } catch (SPARQLDeserializerNotFoundException e) {
            isValid = false;
            T validationContext = contextSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            addInvalidDatatypeError(restriction.getOnDataRange(), validationContext, e.getMessage());
        }
    }

    protected void validateObjectPropertyValue(URI graph, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction, Supplier<T> contextSupplier) {
        try {
            // check if URI is valid
            new URI(value);

            // get List of each context in which the value is involved
            Map<String, List<T>> validationContextsByValue = contextsByValuesAndTypes.computeIfAbsent(restriction.getOnClass().toString(), key -> new PatriciaTrie<>());
            List<T> validationContexts = validationContextsByValue.computeIfAbsent(value, key -> new ArrayList<>());

            T validationContext = contextSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            validationContexts.add(validationContext);

            model.addRelation(graph, property, URI.class, value);

        } catch (URISyntaxException e) {
            T validationContext = contextSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            addInvalidURIError(validationContext, e.getMessage());
        }
    }

    public boolean validateValuesByType() throws SPARQLException {

        if (valuesByTypeToCheck.isEmpty()) {
            return true;
        }

        AtomicBoolean valid = new AtomicBoolean(true);

        for (Map.Entry<String, Map<String, List<T>>> entry : contextsByValuesAndTypes.entrySet()) {

            String type = entry.getKey();
            Map<String, List<T>> contextsByUris = entry.getValue();

            // build SPARQL query for validating values according type
            SelectBuilder checkQuery = getCheckUriListExistQuery(type, contextsByUris.keySet());

            // iterate over results (TRUE or FALSE).
            // IMPORTANT : the iteration assume that each result returned by the SPARQL repository are in the same order than URI in VALUES clause
            // e.g. if we use VALUES ?uri { :uri_1 :uri_2 :uri_n }, here it's assumed that the results will be [ :uri_1 :uri_2 :uri_n ] in this exact order, regardless or natural/lexicographical order of URI

            Iterator<Map.Entry<String, List<T>>> it = contextsByUris.entrySet().iterator();
            sparql.executeSelectQueryAsStream(checkQuery).forEach(result -> {

                boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));

                if (!uriExists) {
                    valid.set(false);
                    Map.Entry<String, List<T>> contextsByUriEntry = it.next();
                    List<T> contexts = contextsByUriEntry.getValue();

                    contexts.forEach(context -> addInvalidValueError(null, context));
                } else {
                    // just skip this element since we don't need to access contexts
                    it.next();
                }
            });
        }

        return valid.get();

    }

    /**
     * @param type the rdf:type
     * @param uris the {@link Set} of URI to check in {@link String} representation
     * @return a {@link SelectBuilder} which when executed, indicate for each element of uris, if the element is an instance of type
     * @apiNote The produced SPARQL query look likes
     * <code>
     * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     * SELECT  (EXISTS {
     * ?rdfType rdfs:subClassOf* :some_rdf_type.
     * ?uri  a  ?rdfType
     * } AS ?existing)
     * WHERE{
     * VALUES ?uri { :uri_1 :uri_2 :uri_n }
     * }
     * </code>
     */
    private SelectBuilder getCheckUriListExistQuery(String type, Set<String> uris) {

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var existing = makeVar(SPARQLService.EXISTING_VAR);

        WhereBuilder where = new WhereBuilder()
                .addWhere(typeVar, Ontology.subClassAny, NodeFactory.createURI(type))
                .addWhere(uriVar, RDF.type, typeVar);

        // add EXIST {} expression as var of SELECT
        SelectBuilder select = new SelectBuilder()
                .addVar(SPARQLQueryHelper.getExprFactory().exists(where), existing);

        // append VALUES ?uri  :uri_1 ... :uri_n
        SPARQLQueryHelper.addWhereUriStringValues(select, uriVar.getVarName(), uris.stream(), true, uris.size());

        return select;
    }

}
