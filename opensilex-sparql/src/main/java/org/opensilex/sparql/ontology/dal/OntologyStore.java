/*******************************************************************************
 *                         OntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.dal;

import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author rcolin
 */
public class OntologyStore {

    private final SPARQLService sparql;
    private final URI topDataPropertyUri = URI.create(OWL2.topDataProperty.getURI());
    private final URI topObjectPropertyUri = URI.create(OWL2.topObjectProperty.getURI());

    public OntologyStore(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ClassModel getClassModel(URI rdfClass, URI parentClass, String lang) throws SPARQLException {

        WhereHandler parentHandler = null;
        if (parentClass != null) {
            // Add a WHERE with a subClassOf* path on PARENT field, instead to add it on the end of query
            parentHandler = new WhereHandler();
            parentHandler.addWhere(new TriplePath(makeVar(SPARQLTreeModel.PARENT_FIELD), Ontology.subClassAny, SPARQLDeserializers.nodeURI(parentClass)));
        }

        try {
            ClassModel model = sparql.loadByURI(
                    null, // don't specify a graph, since multiple graph can contain a class definition
                    ClassModel.class,
                    rdfClass,
                    lang,
                    null,
                    parentClass != null ? Collections.singletonMap(SPARQLTreeModel.PARENT_FIELD, parentHandler) : null
            );

            if (model == null) {
                throw new SPARQLInvalidURIException("URI not found", rdfClass);
            }

            buildProperties(model, lang);

            return model;
        } catch (Exception e) {
            throw new SPARQLException(e);
        }

    }

    private void addRestriction(OwlRestrictionModel restriction,
                                Map<URI, OwlRestrictionModel> mergedRestrictions,
                                Map<URI, URI> datatypePropertiesURI,
                                Map<URI, URI> objectPropertiesURI) throws Exception {

        URI propertyURI = restriction.getOnProperty();

        if (mergedRestrictions.containsKey(propertyURI)) {
            OwlRestrictionModel mergedRestriction = mergedRestrictions.get(propertyURI);
            mergedRestriction.setCardinality(restriction.getCardinality());
            mergedRestriction.setMinCardinality(restriction.getMinCardinality());
            mergedRestriction.setMaxCardinality(restriction.getMaxCardinality());
        } else {
            mergedRestrictions.put(propertyURI, restriction);
        }

        if (restriction.getOnDataRange() != null) {
            if (SPARQLDeserializers.existsForDatatype(restriction.getOnDataRange())) {
                datatypePropertiesURI.put(propertyURI, restriction.getOnDataRange());
            }
        } else if (restriction.getOnClass() != null) {
            if (sparql.uriExists(ClassModel.class, restriction.getOnClass())) {
                objectPropertiesURI.put(propertyURI, restriction.getOnClass());
            }
        } else if (restriction.getSomeValuesFrom() != null) {
            URI someValueFrom = restriction.getSomeValuesFrom();
            if (SPARQLDeserializers.existsForDatatype(someValueFrom)) {
                datatypePropertiesURI.put(propertyURI, someValueFrom);
            } else if (sparql.uriExists(ClassModel.class, someValueFrom)) {
                objectPropertiesURI.put(propertyURI, someValueFrom);
            }
        }

    }

    private void buildDataAndObjectProperties(ClassModel model,
                                              String lang,
                                              Map<URI, URI> datatypePropertiesURI,
                                              Map<URI, URI> objectPropertiesURI) throws Exception {

        Map<URI, DatatypePropertyModel> dataPropertiesMap = new HashMap<>();
        List<DatatypePropertyModel> dataPropertiesList = sparql.getListByURIs(DatatypePropertyModel.class,
                datatypePropertiesURI.keySet(), lang);

        MapUtils.populateMap(dataPropertiesMap, dataPropertiesList, pModel -> {
            // don't set parent if parent is TopObjectProperty
            if (pModel.getParent() != null && SPARQLDeserializers.compareURIs(topDataPropertyUri, pModel.getParent().getUri())) {
                pModel.setParent(null);
            }
            pModel.setTypeRestriction(datatypePropertiesURI.get(pModel.getUri()));
            return pModel.getUri();
        });
        model.setDatatypeProperties(dataPropertiesMap);

        Map<URI, ObjectPropertyModel> objectPropertiesMap = new HashMap<>();
        List<ObjectPropertyModel> objectPropertiesList = sparql.getListByURIs(ObjectPropertyModel.class, objectPropertiesURI.keySet(), lang);

        MapUtils.populateMap(objectPropertiesMap, objectPropertiesList, pModel -> {
            pModel.setTypeRestriction(objectPropertiesURI.get(pModel.getUri()));

            // don't set parent if parent is TopObjectProperty
            if (pModel.getParent() != null && SPARQLDeserializers.compareURIs(topObjectPropertyUri, pModel.getParent().getUri())) {
                pModel.setParent(null);
            }
            return pModel.getUri();
        });
        model.setObjectProperties(objectPropertiesMap);
    }


    public void buildProperties(ClassModel model, String lang) throws Exception {

        List<OwlRestrictionModel> restrictions = getOwlRestrictions(model.getUri(), lang);
        if(restrictions.isEmpty()){
            return;
        }

        Map<URI, URI> datatypePropertiesURI = new HashMap<>();
        Map<URI, URI> objectPropertiesURI = new HashMap<>();
        Map<URI, OwlRestrictionModel> mergedRestrictions = new HashMap<>();

        for(OwlRestrictionModel restriction : restrictions){
            addRestriction(restriction, mergedRestrictions, datatypePropertiesURI, objectPropertiesURI);
        }
        model.setRestrictions(mergedRestrictions);

        buildDataAndObjectProperties(model, lang, datatypePropertiesURI, objectPropertiesURI);
    }

    public List<OwlRestrictionModel> getOwlRestrictions(URI rdfClass, String lang) throws Exception {

        return sparql.search(
                null, // don't specify a graph, since multiple graph can contain a restriction definition
                OwlRestrictionModel.class,
                lang,
                (SelectBuilder select) -> {
                    Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
                    Var classUriVar = makeVar("classURI");
                    select.addWhere(classUriVar, RDFS.subClassOf, uriVar);
                    select.addWhere(SPARQLDeserializers.nodeURI(rdfClass), Ontology.subClassAny, classUriVar);
                }
        );

    }
}
