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

/**
 * <pre>
 * Validator which can validate and register invalid relation for any {@link SPARQLResourceModel}
 *
 * This validator rely on an {@link OntologyStore} in order to fetch schema information :
 * - Classes
 * - Properties
 * - Link and cardinalities between classes and properties
 *
 * This validator has the following behaviour :
 * - Any validation concerning a datatype property are checked with {@link #validateDataTypePropertyValue(URI, SPARQLResourceModel, String, URI, OwlRestrictionModel, Supplier)}
 * - Check if some object property is linked to some type is performed inside {@link #validateObjectPropertyValue(URI, SPARQLResourceModel, String, URI, OwlRestrictionModel, Supplier)}
 *
 * - Any validation which concerns a full model  (can be useful for multi-valued properties or with some validation which works on multiple relation) : {@link #validateModel(ClassModel, SPARQLResourceModel, Supplier)}
 *
 * - Any validation which performs additional I/O should be performed inside {@link #batchValidation()}
 *      - This case mainly address the problem of checking the existence of N uris of some type T, without performing N request during each {@code validateModel} call
 *      - This is done by collecting the (uri,type) couple during {@code #validateModel} call
 *      - Then SPARQL validation queries are performed with a batch of N uris
 *
 * According these metadata the validator can check
 * - OWL datatype properties : {@link #validateDataTypePropertyValue(URI, SPARQLResourceModel, String, URI, OwlRestrictionModel, Supplier)}
 * - OWL object-type properties : {@link #validateObjectPropertyValue(URI, SPARQLResourceModel, String, URI, OwlRestrictionModel, Supplier)}
 *
 * Depending on the validity of these relations, several kind of error can me handled
 * - Error about property which is not linked to some type :  {@link #addUnknownPropertyError(ValidationContext)}
 * - Error about incorrect or unknown URI : {@link #addInvalidURIError(ValidationContext)}
 * - Error about bad parsing/conversion with a primitive/datatype : {@link #addInvalidDatatypeError(ValidationContext,URI)}
 * - Error about invalid value (can depend on the domain) {@link #addInvalidValueError(ValidationContext)}
 * - Error about a required relation which is not present for a model : {@link #addMissingRequiredValue(ValidationContext)}
 * </pre>
 * @param <T> The validation content type (depend on validation case : JSON API, CSV file)
 *
 * @apiNote
 * <pre>
 * - This class is not thread-safe so concurrent call to {@link #validateModel(ClassModel, SPARQLResourceModel, Supplier)} and {@link #batchValidation()}
 *   could lead to unexpected behavior. Prefer the use of one validator per thread/task.
 * - This class performs validation in a single-threaded way.
 *
 * </pre>
 * @author rcolin
 *
 */
public abstract class OwlRestrictionValidator<T extends ValidationContext> {

    protected final SPARQLService sparql;
    protected final OntologyStore ontologyStore;

    protected int nbError;

    protected int nbErrorLimit;

    /**
     * Store for each root type, a map between subtype and corresponding context error.
     * The context error can be a CSV cell (in case of CSV import), it can be an index inside an array, etc.
     */
    protected Map<String, Map<String, List<T>>> validationByTypesAndValues;

    /**
     * @param sparql SPARQL service (required) Used to perform SPARQL queries for validation
     * @param ontologyStore Ontology store used for schema retrieval
     * @param nbErrorLimit The maximum number of error. If this number IS reached during call to {@link #validateModel(ClassModel, SPARQLResourceModel, Supplier)} or {@link #batchValidation()}, then validation is stopped.
     */
    protected OwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore, int nbErrorLimit) {

        this.sparql = sparql;
        this.ontologyStore = ontologyStore;

        this.validationByTypesAndValues = new PatriciaTrie<>();
        this.nbError = 0;

        assert (nbErrorLimit > 0);
        this.nbErrorLimit = nbErrorLimit;
    }

    /**
     * Define how to handle an error about a property which is not linked to some type
     * @param context error context
     */
    public void addUnknownPropertyError(T context){
        this.nbError++;
    }

    /**
     * Define how to handle an error about any invalid value (can depend on the domain)
     * @param context error context
     */
    public void addInvalidValueError(T context){
        this.nbError++;
    }

    /**
     * Define how to handle an error about a required relation which is not present for a model
     * @param context error context
     */
    public void addMissingRequiredValue(T context){
        this.nbError++;
    }


    /**
     * Define how to handle an object with a URI which is already used
     * @param context error context
     */
    public void addAlreadyExistingURIError(T context) {this.nbError++;}

    /**
     * Define how to handle an error about a bad parsing/conversion with a primitive/datatype
     * @param context error context
     * @param datatype expected datatype URI
     */
    public void addInvalidDatatypeError(T context, URI datatype){
        this.nbError++;
    }

    /**
     * Define how to handle an error about an incorrect or unknown URI
     * @param context error context
     */
    public void addInvalidURIError(T context){
        this.nbError++;
    }

    /**
     * @return true if any validation error has been encountered, false else
     */
    public boolean isValid() {
        return nbError == 0;
    }

    public int getNbError() {
        return nbError;
    }

    /**
     * Validate a single model. This validation step check that all required relation are filled, and that multivalued relation have the good cardinality.
     *
     * @param classModel {@link ClassModel} corresponding with {@code model} type ({@link SPARQLResourceModel#getType()})
     * @param model the model to validate
     * @param validationContextSupplier define the way to generate a context-specific error
     */
    public void validateModel(ClassModel classModel, SPARQLResourceModel model, Supplier<T> validationContextSupplier) {

        //  group values by property
        Map<String, List<String>> valuesByProperties = new PatriciaTrie<>();
        for (SPARQLModelRelation relation : model.getRelations()) {
            List<String> values = valuesByProperties.computeIfAbsent(relation.getProperty().toString(), key -> new LinkedList<>());
            values.add(relation.getValue());
        }

        // performs validation according class OWL restrictions
        for (OwlRestrictionModel restriction : classModel.getRestrictionsByProperties().values()) {

            String propertyStr = restriction.getOnProperty().toString();
            List<String> values = valuesByProperties.get(propertyStr);

            // check that all required restriction are filled
            if (restriction.isRequired() && values == null) {
                T validationContext = validationContextSupplier.get();
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
                    T validationContext = validationContextSupplier.get();
                    validationContext.setProperty(propertyStr);
                    validationContext.setValue(values.subList(0, 1).toString());
                    validationContext.setMessage("Property is mono-valued : only one value is accepted");
                    addInvalidValueError(validationContext);
                }
            }

        }
    }

    private T getValidationContext(Supplier<T> validationContextSupplier, URI property, String value, String msg){
        T validationContext = validationContextSupplier.get();
        validationContext.setValue(value);
        validationContext.setProperty(property.toString());
        validationContext.setMessage(msg);
        return validationContext;
    }

    /**
     *  Check a single model relation
     * @param graph model graph
     * @param classModel {@link ClassModel} corresponding with {@code model} type ({@link SPARQLResourceModel#getType()})
     * @param model the model for which we validate the relation
     * @param property property of the relation to validate
     * @param value value of the relation to validate
     * @param restriction OWL restriction between property and model type
     * @param validationContextSupplier define the way to generate a context-specific error
     */
    protected void validateModelRelation(URI graph, ClassModel classModel, SPARQLResourceModel model, URI property, String value, OwlRestrictionModel restriction, Supplier<T> validationContextSupplier) {

        boolean hasValue = !StringUtils.isEmpty(value);

        // value for an unknown restriction
        if (restriction == null && hasValue) {
            T validationContext = getValidationContext(validationContextSupplier, property, value, "Unknown property " + property + " for type " + classModel.getUri());
            addUnknownPropertyError(validationContext);

        } else if (!hasValue) {
            if (restriction != null && restriction.isRequired()) {
                T validationContext = getValidationContext(validationContextSupplier,property,value,classModel.getUri().toString());
                addMissingRequiredValue(validationContext);
            }

        } else {  // restriction and value
            if (restriction.getOnDataRange() != null) {
                validateDataTypePropertyValue(graph, model, value, property, restriction, validationContextSupplier);
            } else if (restriction.getOnClass() != null) {
                validateObjectPropertyValue(graph, model, value, property, restriction, validationContextSupplier);
            }else{
                // unknown restriction type
                T validationContext = getValidationContext(validationContextSupplier,property,value,"Can't determine if the property is a data or object property");
                addInvalidValueError(validationContext);
            }
        }

    }

    /**
     * Check a single model relation with a datatype value
     * @param graph model graph
     * @param model the model for which we validate the relation
     * @param property property of the relation to validate
     * @param value value of the relation to validate
     * @param restriction OWL restriction between property and model type
     * @param validationContextSupplier define the way to generate a context-specific error
     */
    protected void validateDataTypePropertyValue(URI graph, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction, Supplier<T> validationContextSupplier) {
        try {
            SPARQLDeserializer<?> deserializer = SPARQLDeserializers.getForDatatype(restriction.getOnDataRange());

            if (!deserializer.validate(value)) {
                T validationContext = getValidationContext(validationContextSupplier,property,value,null);
                addInvalidDatatypeError(validationContext, restriction.getOnDataRange());

            } else {
                model.addRelation(graph, property, deserializer.getClassType(), value);

                if (SPARQLDeserializers.compareURIs(property.toString(), RDFS.label.getURI())) {
                    if (SPARQLNamedResourceModel.class.isAssignableFrom(model.getClass())) {
                        ((SPARQLNamedResourceModel<?>) model).setName(value);
                    }
                }
            }

        } catch (SPARQLDeserializerNotFoundException e) {
            T validationContext = getValidationContext(validationContextSupplier,property,value,e.getMessage());
            addInvalidDatatypeError(validationContext, restriction.getOnDataRange());
        }
    }


    /**
     * Check a single model relation with an object-type value
     * @param graph model graph
     * @param model the model for which we validate the relation
     * @param property property of the relation to validate
     * @param value value of the relation to validate
     * @param restriction OWL restriction between property and model type
     * @param validationContextSupplier define the way to generate a context-specific error
     *
     * @apiNote This method register value and model type for a further batch validation (the validation query is not performed during this method call)
     * @see #batchValidation()
     */
    protected void validateObjectPropertyValue(URI graph, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction, Supplier<T> validationContextSupplier) {

        // check if URI is valid and absolute
        try {
            URI uri = new URI(value);
            if(! uri.isAbsolute()){
                addInvalidURIError(getValidationContext(validationContextSupplier, property, value, "Not a valid and absolute URI"));
                return;
            }
        } catch (URISyntaxException e) {
            T validationContext = getValidationContext(validationContextSupplier,property,value,e.getMessage());
            addInvalidURIError(validationContext);
            return;
        }

        T validationContext = validationContextSupplier.get();
        validationContext.setValue(value);
        validationContext.setProperty(property.toString());

        // get or create map of <values,validationContext> by type, then
        // get or create list of validationContext by value
        validationByTypesAndValues
                .computeIfAbsent(restriction.getOnClass().toString(), key -> new PatriciaTrie<>())
                .computeIfAbsent(value, key -> new ArrayList<>())
                .add(validationContext);

        // #TODO find a way to not store full context
        // context is stored in order to be able to retrieve corresponding URI in case of validation fail.
        // This lead to the tmp storage of one context object for each cell/object-property relation

        model.addRelation(graph, property, URI.class, value);
    }

    /**
     * Performs a batch validation depending on the content of {@link #validationByTypesAndValues}
     */
    public void batchValidation() {

        if (validationByTypesAndValues.isEmpty()) {
            return;
        }

        // read each types <-> values and run a SPARQL query per type to check if values exists or not
        validationByTypesAndValues.forEach((type, validationByValue) -> {

            Set<String> valuesForType = validationByValue.keySet();

            if (nbError >= nbErrorLimit || valuesForType.isEmpty()) {
                return;
            }

            // build SPARQL queries for validating values according type
            SelectBuilder checkQuery = sparql.getCheckUriListExistQuery(valuesForType.stream(), valuesForType.size(),type, null);

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
