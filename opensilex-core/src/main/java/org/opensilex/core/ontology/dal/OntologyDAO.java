/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.OrderBy;

/**
 *
 * @author vince
 */
public final class OntologyDAO {

    private final SPARQLService sparql;

    public OntologyDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final static Map<String, Map<URI, Map<URI, String>>> labelTranslationMap = new HashMap<>();
    private final static Map<URI, Map<URI, Integer>> classPropertiesOrderMap = new HashMap<>();
    private final static List<URI> abstractClasses = new ArrayList<>();

    public static void customizeOntology(ObjectNode ontologyDefinitionOverride) throws URISyntaxException {
        labelTranslationMap.clear();
        classPropertiesOrderMap.clear();
        abstractClasses.clear();
        Iterator<Map.Entry<String, JsonNode>> classesOverride = ontologyDefinitionOverride.fields();
        while (classesOverride.hasNext()) {
            Map.Entry<String, JsonNode> classOverride = classesOverride.next();
            URI classURI = new URI(classOverride.getKey());
            JsonNode classOverrideNode = classOverride.getValue();
            if (classOverrideNode.hasNonNull("properties")) {
                Iterator<Map.Entry<String, JsonNode>> propertiesOverride = classOverrideNode.get("properties").fields();
                while (propertiesOverride.hasNext()) {
                    Map.Entry<String, JsonNode> propertyOverride = propertiesOverride.next();
                    URI propertyURI = new URI(propertyOverride.getKey());
                    JsonNode propertyValues = propertyOverride.getValue();
                    if (propertyValues.hasNonNull("label")) {
                        Iterator<Map.Entry<String, JsonNode>> labelTs = propertyValues.get("label").fields();
                        while (labelTs.hasNext()) {
                            Map.Entry<String, JsonNode> label = labelTs.next();
                            String lang = label.getKey();
                            String ts = label.getValue().asText();
                            addOntologyTranslationLabel(classURI, propertyURI, lang, ts);
                        }
                    }
                    if (propertyValues.hasNonNull("order")) {
                        int propertyOrder = propertyValues.get("order").asInt();
                        addOntologyPropertiesOrder(classURI, propertyURI, propertyOrder);
                    }
                }
            }

            if (classOverrideNode.hasNonNull("abstract")) {
                if (classOverrideNode.get("abstract").asBoolean(false)) {
                    abstractClasses.add(classURI);
                }
            }
        }
    }

    private static String getOntologyTranslationLabel(URI classURI, URI propertyURI, String lang) {
        if (labelTranslationMap.containsKey(lang)) {
            Map<URI, Map<URI, String>> langMap = labelTranslationMap.get(lang);
            if (langMap.containsKey(classURI)) {
                Map<URI, String> classMap = langMap.get(classURI);
                if (classMap.containsKey(propertyURI)) {
                    return classMap.get(propertyURI);
                }
            }
        }

        return null;
    }

    private static void addOntologyTranslationLabel(URI classURI, URI propertyURI, String lang, String value) {
        if (!labelTranslationMap.containsKey(lang)) {
            labelTranslationMap.put(lang, new HashMap<>());
        }

        Map<URI, Map<URI, String>> langMap = labelTranslationMap.get(lang);

        if (!langMap.containsKey(classURI)) {
            langMap.put(classURI, new HashMap<>());
        }

        Map<URI, String> classMap = langMap.get(classURI);
        classMap.put(propertyURI, value);
    }

    private static Map<URI, Integer> getOntologyPropertiesOrder(URI classURI) {
        if (classPropertiesOrderMap.containsKey(classURI)) {
            return classPropertiesOrderMap.get(classURI);
        } else {
            return new HashMap<>();
        }
    }

    private static void addOntologyPropertiesOrder(URI classURI, URI propertyURI, int value) {
        if (!classPropertiesOrderMap.containsKey(classURI)) {
            classPropertiesOrderMap.put(classURI, new HashMap<>());
        }

        classPropertiesOrderMap.get(classURI).put(propertyURI, value);
    }

    private static boolean isAbstractClass(URI classURI) {
        return abstractClasses.contains(classURI);
    }

    public SPARQLTreeListModel<ClassModel> searchSubClasses(URI parent, UserModel user, boolean excludeRoot) throws Exception {
        SPARQLTreeListModel<ClassModel> classTree = sparql.searchResourceTree(
                ClassModel.class,
                user.getLanguage(),
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    if (parent
                    != null) {
                        Var parentVar = makeVar(ClassModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(parent));
                        select.addWhere(makeVar(ClassModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                }
        );

        classTree.traverse((ClassModel model) -> {
            model.setAbstractClass(isAbstractClass(model.getUri()));
        });

        return classTree;
    }

    public SPARQLTreeListModel<PropertyModel> searchSubProperties(URI parent, UserModel user, boolean excludeRoot) throws Exception {
        return sparql.searchResourceTree(
                PropertyModel.class,
                user.getLanguage(),
                parent,
                excludeRoot,
                (SelectBuilder select) -> {
                    Node parentNode = SPARQLDeserializers.nodeURI(parent);
                    if (parentNode
                    != null) {
                        Var parentVar = makeVar(DatatypePropertyModel.PARENT_FIELD);
                        select.addWhere(parentVar, Ontology.subClassAny, parentNode);
                        select.addWhere(makeVar(DatatypePropertyModel.URI_FIELD), RDFS.subClassOf, parentVar);
                    }
                }
        );
    }

    public List<OwlRestrictionModel> getOwlRestrictions(URI rdfClass, String lang) throws Exception {
        List<OrderBy> orderByList = new ArrayList<>();
        orderByList.add(new OrderBy(OwlRestrictionModel.URI_FIELD + "=ASC"));
        return sparql.search(OwlRestrictionModel.class,
                lang, (SelectBuilder select) -> {
                    Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
                    Var classUriVar = makeVar("classURI");

                    select.addWhere(classUriVar, RDFS.subClassOf, uriVar);

                    select.addWhere(SPARQLDeserializers.nodeURI(rdfClass), Ontology.subClassAny, classUriVar);
                }, orderByList);

    }

    public ClassModel getClassModel(URI rdfClass, String lang) throws Exception {
        ClassModel model = sparql.getByURI(ClassModel.class,
                rdfClass, lang);

        if (model == null) {
            throw new NotFoundURIException(rdfClass);
        }

        model.setAbstractClass(isAbstractClass(rdfClass));

        List<OwlRestrictionModel> restrictions = getOwlRestrictions(rdfClass, lang);

        Map<URI, URI> datatypePropertiesURI = new HashMap<>();
        Map<URI, URI> objectPropertiesURI = new HashMap<>();
        Map<URI, OwlRestrictionModel> mergedRestrictions = new HashMap<>();
        for (OwlRestrictionModel restriction : restrictions) {
            URI propertyURI = restriction.getOnProperty();

            if (mergedRestrictions.containsKey(propertyURI)) {
                OwlRestrictionModel mergedRestriction = mergedRestrictions.get(propertyURI);
                if (restriction.getCardinality() != null) {
                    mergedRestriction.setCardinality(restriction.getCardinality());
                }
                if (restriction.getMinCardinality() != null) {
                    mergedRestriction.setMinCardinality(restriction.getMinCardinality());
                }
                if (restriction.getMaxCardinality() != null) {
                    mergedRestriction.setMaxCardinality(restriction.getMaxCardinality());
                }
            } else {
                mergedRestrictions.put(propertyURI, restriction);
            }
            if (restriction.getOnDataRange() != null) {
                if (BuiltInDatatypes.isBuiltInDatatype(restriction.getOnDataRange())) {
                    datatypePropertiesURI.put(propertyURI, restriction.getOnDataRange());
                }
            } else if (restriction.getOnClass() != null) {
                if (sparql.uriExists(ClassModel.class,
                        restriction.getOnClass())) {
                    objectPropertiesURI.put(propertyURI, restriction.getOnClass());
                }
            } else if (restriction.getSomeValuesFrom() != null) {
                URI someValueFrom = restriction.getSomeValuesFrom();
                if (BuiltInDatatypes.isBuiltInDatatype(someValueFrom)) {
                    datatypePropertiesURI.put(propertyURI, someValueFrom);
                } else if (sparql.uriExists(ClassModel.class,
                        someValueFrom)) {
                    objectPropertiesURI.put(propertyURI, someValueFrom);
                }
            }
        }

        model.setRestrictions(mergedRestrictions);

        Map<URI, DatatypePropertyModel> dataPropertiesMap = new HashMap<>();
        List<DatatypePropertyModel> dataPropertiesList = sparql.getListByURIs(DatatypePropertyModel.class,
                datatypePropertiesURI.keySet(), lang);
        MapUtils.populateMap(dataPropertiesMap, dataPropertiesList, (pModel) -> {
            pModel.setName(getRelationLabel(model, pModel, lang));
            pModel.setDatatypeRestriction(datatypePropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setDatatypeProperties(dataPropertiesMap);

        Map<URI, ObjectPropertyModel> objectPropertiesMap = new HashMap<>();
        List<ObjectPropertyModel> objectPropertiesList = sparql.getListByURIs(ObjectPropertyModel.class,
                objectPropertiesURI.keySet(), lang);
        MapUtils.populateMap(objectPropertiesMap, objectPropertiesList, (pModel) -> {
            pModel.setName(getRelationLabel(model, pModel, lang));
            pModel.setObjectClassRestriction(objectPropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setObjectProperties(objectPropertiesMap);

        model.setPropertiesOrder(getPropertiesOrder(model));

        return model;
    }

    private String getRelationLabel(ClassModel model, PropertyModel property, String lang) {
        String ts = OntologyDAO.getOntologyTranslationLabel(model.getUri(), property.getUri(), lang);

        if (ts == null) {
            if (model.getParent() != null) {
                ts = getRelationLabel(model.getParent(), property, lang);
                OntologyDAO.addOntologyTranslationLabel(model.getType(), property.getUri(), lang, ts);
            } else {
                ts = property.getName();
                if (ts == null) {
                    ts = property.getUri().toString();
                }
            }
        }

        return ts;
    }

    private Map<URI, Integer> getPropertiesOrder(ClassModel model) {
        Map<URI, Integer> baseMap = new HashMap<>();

        if (model.getParent() != null) {
            baseMap = getPropertiesOrder(model.getParent());
        }

        baseMap.putAll(OntologyDAO.getOntologyPropertiesOrder(model.getUri()));

        return baseMap;
    }
}
