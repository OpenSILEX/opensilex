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
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class OwlRestrictionValidator<T extends ValidationContext> {

    protected final SPARQLService sparql;
    protected final OntologyStore ontologyStore;

    protected int nbError;
    protected int nbErrorLimit;
    protected Map<String, Map<String, List<T>>> validationByTypesAndValues;

    protected OwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore, int nbErrorLimit) {

        this.sparql = sparql;
        this.ontologyStore = ontologyStore;

        this.validationByTypesAndValues = new PatriciaTrie<>();
        this.nbError = 0;

        assert (nbErrorLimit > 0);
        this.nbErrorLimit = nbErrorLimit;
    }

    protected abstract void addUnknownPropertyError(T context);

    protected abstract void addInvalidValueError(T context);

    protected abstract void addMissingRequiredValue(T context);

    protected abstract void addInvalidDatatypeError(T context, URI datatype);

    protected abstract void addInvalidURIError(T context);

    public boolean isValid() {
        return nbError == 0;
    }

    public int getNbError() {
        return nbError;
    }

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

                nbError++;
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

                    nbError++;
                }
            }

        }
    }


    protected void validateModelRelation(URI graph, ClassModel classModel, SPARQLResourceModel model, URI property, String value, OwlRestrictionModel restriction, Supplier<T> contextSupplier) {

        boolean hasValue = !StringUtils.isEmpty(value);

        // value for an unknown restriction
        if (restriction == null && hasValue) {
            T validationContext = contextSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            validationContext.setValue(classModel.getUri().toString());
            validationContext.setMessage("Unknown property "+ property +" for type " + classModel.getUri());
            addUnknownPropertyError(validationContext);

            nbError++;
        } else if (!hasValue) {
            if (restriction != null && restriction.isRequired()) {

                T validationContext = contextSupplier.get();
                validationContext.setValue(value);
                validationContext.setProperty(property.toString());
                validationContext.setMessage(classModel.getUri().toString());
                addMissingRequiredValue(validationContext);

                nbError++;
            }
        } else {  // no restriction and no value
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
                T validation = validationSupplier.get();
                validation.setValue(value);
                validation.setProperty(property.toString());
                addInvalidDatatypeError(validation, restriction.getOnDataRange());

                nbError++;
            } else {
                model.addRelation(graph, property, deserializer.getClassType(), value);

                if (SPARQLDeserializers.compareURIs(property.toString(), RDFS.label.getURI())) {
                    if (SPARQLNamedResourceModel.class.isAssignableFrom(model.getClass())) {
                        ((SPARQLNamedResourceModel<?>) model).setName(value);
                    }
                }
            }

        } catch (SPARQLDeserializerNotFoundException e) {
            T validationContext = validationSupplier.get();
            validationContext.setValue(value);
            validationContext.setProperty(property.toString());
            validationContext.setMessage(e.getMessage());
            addInvalidDatatypeError(validationContext, restriction.getOnDataRange());

            nbError++;
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
                    .computeIfAbsent(restriction.getOnClass().toString(), key -> new PatriciaTrie<>())
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

            nbError++;
        }
    }

    public void validateValuesByType() throws SPARQLException {

        if (validationByTypesAndValues.isEmpty()) {
            return;
        }

        // read each types <-> values and run a SPARQL query per type to check if values exists or not
        validationByTypesAndValues.forEach((type, validationByValue) -> {

            Set<String> valuesForType = validationByValue.keySet();

            if (nbError >= nbErrorLimit || valuesForType.isEmpty()) {
                return;
            }

            // build SPARQL query for validating values according type
            SelectBuilder checkQuery = sparql.getCheckUriListExistQuery(type, valuesForType.stream(), valuesForType.size());

            // Use iterator to lookup over SPARQL results by keeping match with map values
            Iterator<Map.Entry<String, List<T>>> validationsByValue = validationByValue.entrySet().iterator();

             /* this iteration assume that each result of the SPARQL query are
            returned by the repository in the same order as incoming URI from VALUES clause
            e.g. : VALUES ?uri (:uri_1 :uri_2) -> [ (:uri_1,true/false),(:uri_2,true/false) ]
            */
            try {
                sparql.executeSelectQueryAsStream(checkQuery).forEach((SPARQLResult result) -> {

                    boolean uriExists = Boolean.parseBoolean(result.getStringValue(SPARQLService.EXISTING_VAR));
                    if (!uriExists) {
                        int maxValidationNb = nbErrorLimit - nbError;
                        List<T> validations = validationsByValue.next().getValue();

                        // limit the number of validation nb to read/add
                        validations.stream()
                                .limit(maxValidationNb)
                                .forEach(validation -> {
                                    validation.setMessage("Unknown " + type);
                                    this.addInvalidValueError(validation);
                                });

                        // limit reached -> just set to nbErrorLimit
                        if (nbError + validations.size() > nbErrorLimit) {
                            nbError = nbErrorLimit;
                        } else {
                            nbError += validations.size();
                        }
                    } else {
                        // just skip this element since we don't need to access validation contexts (no error)
                        validationsByValue.next();
                    }
                });
            } catch (SPARQLException e) {
                throw new RuntimeException(e);
            }

        });

    }


}
