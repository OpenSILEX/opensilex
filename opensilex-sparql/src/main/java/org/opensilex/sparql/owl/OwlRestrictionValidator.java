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
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class OwlRestrictionValidator<T extends ValidationContext> {

    protected final SPARQLService sparql;
    protected final OntologyStore ontologyStore;

    protected boolean isValid;
    protected Map<String, Map<String, List<T>>> validationByTypesAndValues;

    protected OwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore) {
        this.sparql = sparql;
        this.ontologyStore = ontologyStore;

        isValid = true;
        validationByTypesAndValues = new PatriciaTrie<>();
    }

    protected abstract void addUnknownPropertyError(T context);

    protected abstract void addInvalidValueError(T context);

    protected abstract void addMissingRequiredValue(T context);

    protected abstract void addInvalidDatatypeError(T context, URI datatype);

    protected abstract void addInvalidURIError(T context);

    public void validateModel(ClassModel classModel, SPARQLResourceModel model, Supplier<T> validationSupplier) {

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
                T validationContext = validationSupplier.get();
                validationContext.setValue(null);
                validationContext.setProperty(propertyStr);
                validationContext.setMessage(classModel.getUri().toString());
                addMissingRequiredValue(validationContext);
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
                    T validationContext = validationSupplier.get();
                    validationContext.setProperty(propertyStr);
                    validationContext.setValue(values.subList(0, 1).toString());
                    validationContext.setMessage("Property is mono-valued : only one value is accepted");
                    addInvalidValueError(validationContext);
                }
            }

        }
    }


    protected void validateModelRelation(URI graph, ClassModel classModel, SPARQLResourceModel model, URI property, String value, OwlRestrictionModel restriction, Supplier<T> contextSupplier) {

        boolean hasValue = !StringUtils.isEmpty(value);

        // value for an unknown restriction
        if (restriction == null && hasValue) {
            isValid = false;
            T validationContext = contextSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            validationContext.setValue(classModel.getUri().toString());
            addUnknownPropertyError(validationContext);
        } else if (!hasValue) {
            if (restriction != null && restriction.isRequired()) {
                isValid = false;
                T validationContext = contextSupplier.get();
                validationContext.setValue(value);
                validationContext.setProperty(property.toString());
                validationContext.setMessage(classModel.getUri().toString());
                addMissingRequiredValue(validationContext);
            }
            // no restriction and no value
        } else {
            if (restriction.getOnDataRange() != null) {
                validateDataTypePropertyValue(graph, model, value, property, restriction, contextSupplier);
            } else if (restriction.getOnClass() != null) {
                validateObjectPropertyValue(graph, model, value, property, restriction, contextSupplier);
            }
        }

    }

    protected void validateDataTypePropertyValue(URI graph, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction, Supplier<T> validationSupplier) {
        try {
            SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getOnDataRange());
            if (!deserializer.validate(value)) {
                isValid = false;
                T validation = validationSupplier.get();
                validation.setValue(value);
                validation.setProperty(property.toString());
                addInvalidDatatypeError(validation, restriction.getOnDataRange());
            }
            if (isValid) {
                model.addRelation(graph, property, deserializer.getClassType(), value);

                if (SPARQLDeserializers.compareURIs(property.toString(), RDFS.label.getURI())) {
                    if (SPARQLNamedResourceModel.class.isAssignableFrom(model.getClass())) {
                        ((SPARQLNamedResourceModel<?>) model).setName(value);
                    }
                }
            }

        } catch (SPARQLDeserializerNotFoundException e) {
            isValid = false;
            T validationContext = validationSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            validationContext.setMessage(e.getMessage());
            addInvalidDatatypeError(validationContext, restriction.getOnDataRange());
        }
    }

    protected void validateObjectPropertyValue(URI graph, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction, Supplier<T> validationSupplier) {
        try {
            // check if URI is valid
            new URI(value);

            T validation = validationSupplier.get();
            validation.setValue(value);
            validation.setProperty(property.toString());

            validationByTypesAndValues
                    // get or create map of <values,validation> by type
                    .computeIfAbsent(restriction.getOnClass().getUri().toString(), key -> new PatriciaTrie<>())
                    // get or create list of validation by value
                    .computeIfAbsent(value, key -> new ArrayList<>())
                    .add(validation);

            model.addRelation(graph, property, URI.class, value);

        } catch (URISyntaxException e) {
            T validationContext = validationSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            validationContext.setMessage(e.getMessage());
            addInvalidURIError(validationContext);
        }
    }

    public boolean validateValuesByType() throws SPARQLException {

        if (validationByTypesAndValues.isEmpty()) {
            return true;
        }
        AtomicBoolean valid = new AtomicBoolean(true);

        for (Map.Entry<String, Map<String, List<T>>> entry : validationByTypesAndValues.entrySet()) {
            String type = entry.getKey();
            Map<String, List<T>> validationByValue = entry.getValue();

            // build SPARQL query for validating values according type
            SelectBuilder checkQuery = sparql.getCheckUriListExistQuery(type, validationByValue.keySet().stream());

            // Use iterator to lookup over SPARQL results by keeping match with map values
            Iterator<Map.Entry<String, List<T>>> validationsByValue = validationByValue.entrySet().iterator();

            /* this iteration assume that each result of the SPARQL query are
            returned by the repository in the same order as incoming URI from VALUES clause
            e.g. : VALUES ?uri (:uri_1 :uri_2) -> [ (:uri_1,true/false),(:uri_2,true/false) ]
            */
            sparql.executeSelectQueryAsStream(checkQuery).forEach((SPARQLResult result) -> {

                boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
                if (!uriExists) {
                    valid.set(false);
                    List<T> validations = validationsByValue.next().getValue();
                    validations.forEach(this::addInvalidValueError);
                } else {
                    // just skip this element since we don't need to access contexts
                    validationsByValue.next();
                }
            });
        }

        return valid.get();
    }


}
