/*******************************************************************************
 *                         StapleModelBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 27/09/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.graphql.staple;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Class that builds a {@link Model} in the expected format of the Staple API, based on the given root URIs and
 * ontology store.
 * </p>
 * <p>
 * See the {@link StapleModelBuilder#build()} method for details about the build algorithm.
 * </p>
 *
 * @author Valentin Rigolle
 */
public class StapleModelBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(StapleModelBuilder.class);
    private static final String SCHEMA_PREFIX = "schema";
    private static final String SCHEMA_NAMESPACE = "http://schema.org/";
    private static final Property SCHEMA_DOMAIN_INCLUDES = ResourceFactory.createProperty("http://schema.org/domainIncludes");
    private static final Property SCHEMA_RANGE_INCLUDES = ResourceFactory.createProperty("http://schema.org/rangeIncludes");
    private static final Set<URI> GRAPHQL_ALLOWED_DATATYPES = Set.of(
            URI.create(XSD.xstring.getURI()),
            URI.create(XSD.xboolean.getURI()),
            URI.create(XSD.decimal.getURI()),
            URI.create(XSD.integer.getURI())
    );
    private static final Set<URI> EXCLUDED_RESOURCES = Set.of(
            URI.create(PROV.PERSON.toString()),
            URI.create(PROV.ENTITY.toString())
    );

    // Parameters for build
    private final Set<URI> rootClassUris;
    private final OntologyStore store;

    // State used during build
    private Model model;
    private Set<URI> classUriSetToAnalyze;

    /**
     * Create a model builder for the Staple API format.
     *
     * @param rootClassUris The starting set of class URIs to add to the staple Model. More classes can be added during
     *                      the build step, for example subclasses of root URIs or ranges of object properties.
     * @param store         The ontology store to perform the ontology and properties analysis.
     */
    public StapleModelBuilder(Collection<URI> rootClassUris, OntologyStore store) {
        this.rootClassUris = new HashSet<>(rootClassUris);
        this.store = store;
    }

    /**
     * <p>
     * Entry point of the model building. Simply put, the algorithm used to build the model is the following :
     * </p>
     *
     * <ol>
     *     <li>Add the root class URIs to the <i>set of URIs to analyze</i></li>
     *     <li>For each URI to analyze, iterate over all subclasses defined in the ontology store (including itself). On
     *     each of the subclasses, perform the following steps :
     *     <ol>
     *         <li>Create a class resource in the model</li>
     *         <li>For each datatype property, create a datatype property resource in the model</li>
     *         <li>For each object property, create an object property resource in the model. If the range of the
     *         property is not in the <i>set of URIs to analyze</i>, add it to the set.</li>
     *         <li>For each restriction, create a corresponding property resource in the model. As for the object
     *         properties, if the range of the restriction is a class model that is not present in <i>the set of URIs
     *         to analyze</i>, add it to the set.</li>
     *     </ol>
     *     </li>
     *     <li>Iterate over the list of statements to detect and correct properties with multiple ranges. Staple only
     *     supports properties that have exactly one type as its range. See
     *     {@link StapleModelBuilder#resolveMultipleRanges()} for details about the range resolution algorithm.</li>
     * </ol>
     *
     * <p>
     *     Note : some of the ressources (classes and properties) are omitted. This behaviour is governed by the
     *     {@link #isResourceExcluded(SPARQLResourceModel)} method.
     * </p>
     *
     * <p>
     *     In reality, there is a bit more complexity induced by the fact that URIs are added to the
     *     set of URIs to analyze <b>during the analysis itself</b>. That is why the algorithm uses two loops and
     *     an intermediate variable called `currentLoopUriSet`.
     * </p>
     *
     * @return A model corresponding to the Staple API format.
     */
    public Model build() throws SPARQLException {
        // Create model
        model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(URIDeserializer.getPrefixes());
        model.setNsPrefix(SCHEMA_PREFIX, SCHEMA_NAMESPACE);

        // Create rdf:type property without domain. Domains will be added as the analysis take place.
        Resource rdfTypePropertyResource = createResource(RDF.type.getURI(), RDF.Property);
        rdfTypePropertyResource.addProperty(RDF.type, OWL.FunctionalProperty);
        rdfTypePropertyResource.addProperty(SCHEMA_RANGE_INCLUDES, XSD.xstring);

        classUriSetToAnalyze = new HashSet<>(rootClassUris);
        while (!classUriSetToAnalyze.isEmpty()) {
            Set<URI> currentLoopUriSet = new HashSet<>(classUriSetToAnalyze);
            classUriSetToAnalyze.clear();
            for (URI rootUri : currentLoopUriSet) {
                SPARQLTreeListModel<ClassModel> classTree = store.searchSubClasses(rootUri, null, null, false);
                classTree.traverse(classModel -> {
                    if (isResourceExcluded(classModel)) {
                        return;
                    }

                    Resource classResource = createBaseClassResource(classModel);

                    //RDF:type property. A class must have at least one property to be detected by the staple API
                    //preprocessor, so we add this property for each class.
                    rdfTypePropertyResource.addProperty(SCHEMA_DOMAIN_INCLUDES, classResource);

                    for (Map.Entry<URI, DatatypePropertyModel> propertyEntry : classModel.getDatatypeProperties().entrySet()) {
                        DatatypePropertyModel propertyModel = propertyEntry.getValue();
                        if (isResourceExcluded(propertyModel)) {
                            continue;
                        }
                        addDatatypeProperty(classResource, propertyModel);
                    }

                    for (Map.Entry<URI, ObjectPropertyModel> propertyEntry : classModel.getObjectProperties().entrySet()) {
                        ObjectPropertyModel propertyModel = propertyEntry.getValue();
                        if (isResourceExcluded(propertyModel)) {
                            continue;
                        }
                        addObjectProperty(classResource, propertyModel);
                    }

                    for (Map.Entry<URI, OwlRestrictionModel> propertyEntry : classModel.getRestrictionsByProperties().entrySet()) {
                        OwlRestrictionModel restrictionModel = propertyEntry.getValue();
                        if (isResourceExcluded(restrictionModel)) {
                            continue;
                        }
                        addPropertyFromRestriction(classResource, restrictionModel);
                    }
                });
            }
            // Filter out already analyzed URIs
            Set<URI> filteredClassUriSetToAnalyze = classUriSetToAnalyze.stream()
                    .filter(uri -> !model.contains(RDF.type, SCHEMA_DOMAIN_INCLUDES, createResource(uri)))
                    .collect(Collectors.toSet());
            classUriSetToAnalyze.clear();
            classUriSetToAnalyze.addAll(filteredClassUriSetToAnalyze);
        }

        resolveMultipleRanges();

        return model;
    }

    //#region Build methods

    private Resource createBaseClassResource(ClassModel classModel) {
        Resource resource = createResource(classModel.getUri(), RDFS.Class);
        Optional.ofNullable(classModel.getParent())
                .map(ClassModel::getUri)
                .ifPresent(parentUri -> resource.addProperty(RDFS.subClassOf, createResource(parentUri)));
        if (classModel.getLabel() != null) {
            for (Map.Entry<String, String> translation : classModel.getLabel().getAllTranslations().entrySet()) {
                Literal literal = model.createLiteral(translation.getValue(), translation.getKey());
                resource.addLiteral(RDFS.label, literal);
            }
        }
        if (classModel.getComment() != null) {
            for (Map.Entry<String, String> translation : classModel.getComment().getAllTranslations().entrySet()) {
                Literal literal = model.createLiteral(translation.getValue(), translation.getKey());
                resource.addLiteral(RDFS.comment, literal);
            }
        }
        return resource;
    }

    private void addDatatypeProperty(Resource classResource, DatatypePropertyModel propertyModel) {
        URI rangeUri = Optional.ofNullable(propertyModel.getRangeURI())
                .orElseGet(() -> {
                    LOGGER.debug("Empty range : <{}> (datatype property)", propertyModel.getUri());
                    return URI.create(XSD.xstring.getURI());
                });

        Resource propertyResource = createResource(propertyModel.getUri(), RDF.Property);
        propertyResource.addProperty(SCHEMA_DOMAIN_INCLUDES, classResource);
        if (GRAPHQL_ALLOWED_DATATYPES.stream().anyMatch(allowed -> SPARQLDeserializers.compareURIs(rangeUri, allowed))) {
            propertyResource.addProperty(SCHEMA_RANGE_INCLUDES, createResource(rangeUri));
        } else { // By default, literals are strings
            propertyResource.addProperty(SCHEMA_RANGE_INCLUDES, XSD.xstring);
        }
        if (propertyModel.getLabel() != null) {
            for (Map.Entry<String, String> translation : propertyModel.getLabel().getAllTranslations().entrySet()) {
                Literal literal = model.createLiteral(translation.getValue(), translation.getKey());
                propertyResource.addLiteral(RDFS.label, literal);
            }
        }
    }

    private void addObjectProperty(Resource classResource, ObjectPropertyModel propertyModel) {
        URI rangeUri = Optional.ofNullable(propertyModel.getRangeURI())
                .orElseGet(() -> {
                    LOGGER.debug("Empty range : <{}> (object property)", propertyModel.getUri());
                    return URI.create(OWL.Thing.getURI());
                });

        Resource propertyResource = createResource(propertyModel.getUri(), RDF.Property);
        propertyResource.addProperty(SCHEMA_DOMAIN_INCLUDES, classResource);
        propertyResource.addProperty(SCHEMA_RANGE_INCLUDES, createResource(rangeUri));
        classUriSetToAnalyze.add(URI.create(URIDeserializer.getExpandedURI(rangeUri)));
        if (propertyModel.getLabel() != null) {
            for (Map.Entry<String, String> translation : propertyModel.getLabel().getAllTranslations().entrySet()) {
                Literal literal = model.createLiteral(translation.getValue(), translation.getKey());
                propertyResource.addLiteral(RDFS.label, literal);
            }
        }
    }

    private void addPropertyFromRestriction(Resource classResource, OwlRestrictionModel restrictionModel) {
        Resource propertyResource = createResource(restrictionModel.getOnProperty(), RDF.Property);
        if (!restrictionModel.isList()) {
            propertyResource.addProperty(RDF.type, OWL.FunctionalProperty);
        }
        propertyResource.addProperty(SCHEMA_DOMAIN_INCLUDES, classResource);
        Resource rangeResource;
        if (restrictionModel.getOnDataRange() != null) {
            if (GRAPHQL_ALLOWED_DATATYPES.stream().anyMatch(allowed -> SPARQLDeserializers.compareURIs(restrictionModel.getOnDataRange(), allowed))) {
                rangeResource = createResource(restrictionModel.getOnDataRange());
            } else {
                rangeResource = XSD.xstring;
            }
        } else if (restrictionModel.getOnClass() != null) {
            rangeResource = createResource(restrictionModel.getOnClass());
            classUriSetToAnalyze.add(URI.create(URIDeserializer.getExpandedURI(restrictionModel.getOnClass())));
        } else {
            throw new RuntimeException("No data range or class");
        }
        propertyResource.addProperty(SCHEMA_RANGE_INCLUDES, rangeResource);
    }

    /**
     * <p>
     * Iterate over the statements in the model to find properties with multiple `rangeIncludes` fields. Because the
     * Staple API does not handle multiple ranges, this method tries to aggregate the different ranges into one. It
     * does that by trying to find a single declared range such as this range is a superclass of all other ranges,
     * so that the semantic of the statements is left unchanged. If no such range exist, the property is deleted so
     * that the model is conform to the API.
     * </p>
     *
     * @throws SPARQLException In the case an error occurs while querying the ontology store
     */
    private void resolveMultipleRanges() throws SPARQLException {
        for (ResIterator it = model.listResourcesWithProperty(RDF.type, RDF.Property); it.hasNext(); ) {
            Resource resource = it.next();
            List<Statement> rangeStatementList = model.listStatements(resource, SCHEMA_RANGE_INCLUDES, (RDFNode) null).toList();
            if (rangeStatementList.size() != 1) {
                List<URI> rangeList = rangeStatementList.stream()
                        .map(statement -> URI.create(statement.getObject().asResource().getURI()))
                        .collect(Collectors.toList());
                LOGGER.info("Properties should have exactly one range ! Property <{}> has {} range(s) : {}",
                        resource.getURI(),
                        rangeStatementList.size(),
                        rangeList
                );
                if (rangeList.size() > 1) {
                    LOGGER.debug("Trying to resolve multiple ranges into one.");
                    Optional<URI> commonAncestor = findCommonAncestor(rangeList);
                    if (commonAncestor.isPresent()) {
                        LOGGER.info("Found common ancestor : <{}>", commonAncestor.get());
                        model.remove(rangeStatementList);
                        model.add(resource, SCHEMA_RANGE_INCLUDES, createResource(commonAncestor.get()));
                        continue;
                    }
                }
                LOGGER.warn(
                        "Could not resolve ranges into one. The property will be deleted from the model (property <{}> with {} range(s) : {}).",
                        resource.getURI(),
                        rangeStatementList.size(),
                        rangeStatementList
                );
                model.remove(
                        model.listStatements(resource, null, (RDFNode) null)
                );
            }
        }
    }

    //#endregion

    //#region Helper methods

    /**
     * Should the given model be excluded from the Staple Model ? This methods works by comparing the expanded URI of
     * the given resource to the {@link StapleModelBuilder#EXCLUDED_RESOURCES} set.
     *
     * @param model The resource model to check
     * @return `true` if the model should be excluded form the Staple Model, `false` otherwise
     */
    private boolean isResourceExcluded(SPARQLResourceModel model) {
        return EXCLUDED_RESOURCES.contains(URI.create(SPARQLDeserializers.getExpandedURI(model.getUri())));
    }

    /**
     * <p>
     *     Attempts to find a common ancestor among a set of RDF classUriCollection. If there exist one URI among the collection
     *     which has a subClassOf* relationship with all URIs of the collection, then this URI is considered a common
     *     ancestor.
     * </p>
     * <p>
     *     The algorithm to perform this operation is pretty straightforward. We iterate over the collection of URIs to
     *     find potential ancestors. For each potential ancestor, we check if all URIs in the collection are subclasses
     *     of the potential ancestor. If that is the case, then the potential ancestor is returned as the common
     *     ancestor. Otherwise, we skip to the next ancestor. If none of the potential ancestors match the condition,
     *     then the empty option is returned.
     * </p>
     *
     * @param classUriCollection The collection of class URIs to check
     * @return An option containing a common ancestor if one was found, an empty option otherwise
     * @throws SPARQLException If an error occurs during the check of subclass relationship
     */
    private Optional<URI> findCommonAncestor(Collection<URI> classUriCollection) throws SPARQLException {
        for (URI potentialAncestor : classUriCollection) {
            if (isCommonAncestor(potentialAncestor, classUriCollection)) {
                return Optional.of(potentialAncestor);
            }
        }
        // No URI matches the requirement for a common ancestor
        return Optional.empty();
    }

    /**
     * @param potentialAncestor The potential common ancestor for all classes of {@code classUriCollection}
     * @param classUriCollection The collection of class URIs to match against the potential ancestor
     * @return {@code true} if and only if {@code potentialAncestor} is targeted by a {@code subClassOf*} relationship
     *         by all classes in {@code classUriCollection}. {@code false} otherwise.
     * @throws SPARQLException If the ontology store throws an exception while checking for the class hierarchy
     */
    private boolean isCommonAncestor(URI potentialAncestor, Collection<URI> classUriCollection) throws SPARQLException {
        for (URI potentialDescendant: classUriCollection) {
            if (!store.classExist(potentialDescendant, potentialAncestor)) {
                return false;
            }
        }
        return true;
    }

    private Resource createResource(URI uri) {
        return createResource(uri.toString());
    }

    private Resource createResource(String uri) {
        return model.createResource(URIDeserializer.getExpandedURI(uri));
    }

    private Resource createResource(URI uri, Resource type) {
        return createResource(uri.toString(), type);
    }

    private Resource createResource(String uri, Resource type) {
        return model.createResource(URIDeserializer.getExpandedURI(uri), type);
    }

    //#endregion
}
