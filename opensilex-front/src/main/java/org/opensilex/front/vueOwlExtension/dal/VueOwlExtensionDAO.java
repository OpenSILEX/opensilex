/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.CoreModule;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyType;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author vmigot
 */
public class VueOwlExtensionDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(VueOwlExtensionDAO.class);

    private final SPARQLService sparql;
    private final OntologyDAO ontologyDAO;

    public VueOwlExtensionDAO(SPARQLService sparql) {
        this.sparql = sparql;
        this.ontologyDAO = new OntologyDAO(sparql);
    }

    public void createExtendedClass(ClassModel instance, VueClassExtensionModel instanceExtension) throws Exception {
        try {
            sparql.startTransaction();
            ontologyDAO.create(instance);
            sparql.create(instanceExtension,false); // reuse the same URI as the ClassModel -> no need to check URI
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }

    public void updateExtendedClass(ClassModel instance, VueClassExtensionModel instanceExtension) throws Exception {
        try {
            sparql.startTransaction();
            ontologyDAO.update(instance);
            sparql.update(instanceExtension);

            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }

    public void deleteExtendedClass(URI classURI) throws Exception {
        try {
            VueClassExtensionModel extensionModel = sparql.getByURI(VueClassExtensionModel.class,classURI,null);
            if(extensionModel == null){
                throw new IllegalArgumentException("Could not delete custom class "+classURI+". This class is not found or is not a custom class");
            }

            sparql.startTransaction();
            ontologyDAO.deleteClass(classURI);
            sparql.delete(VueClassExtensionModel.class, classURI);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }

    public List<VueClassExtensionModel> getExtendedClasses(String lang) throws Exception {
        return sparql.search(VueClassExtensionModel.class, lang);
    }

    private static List<VueOntologyDataType> dataTypes;
    private static List<VueOntologyObjectType> objectTypes;
    private static Map<String, VueOntologyType> typesByURI;

    private static void buildTypeLists() {

        dataTypes = new ArrayList<>();
        objectTypes = new ArrayList<>();
        typesByURI = new HashMap<>();

        // maintain a tmp set in order to ensure uniqueness (according getTypeUri()) into data/object types
        Set<String> uniquesTypes = new HashSet<>();

        ServiceLoader.load(VueOntologyType.class, OpenSilex.getClassLoader())
                .forEach((ontologyType -> {
                    if (!ontologyType.isDisabled()) {
                        // associate the VueOntologyType to the type
                        typesByURI.put(SPARQLDeserializers.getShortURI(ontologyType.getTypeUri()), ontologyType);

                        // associate the VueOntologyType to all type aliases
                        ontologyType.getTypeUriAliases().forEach(typeAlias -> typesByURI.put(
                                SPARQLDeserializers.getShortURI(typeAlias),
                                ontologyType
                        ));
                    }
                }));

        for (VueOntologyType ontologyType : typesByURI.values()) {
            if (ontologyType instanceof VueOntologyDataType) {
                if(! uniquesTypes.contains(ontologyType.getTypeUri())){
                    dataTypes.add((VueOntologyDataType) ontologyType);
                    uniquesTypes.add(ontologyType.getTypeUri());
                }
            } else if (ontologyType instanceof VueOntologyObjectType) {
                if(! uniquesTypes.contains(ontologyType.getTypeUri())){
                    objectTypes.add((VueOntologyObjectType) ontologyType);
                    uniquesTypes.add(ontologyType.getTypeUri());
                }
            } else {
                LOGGER.warn("Unexpected Vue ontology type (ignored): " + ontologyType.getClass().getCanonicalName());
            }
        }
    }

    public static List<VueOntologyDataType> getDataTypes() {
        if (dataTypes == null) {
            buildTypeLists();
        }

        return dataTypes;
    }

    public static List<VueOntologyObjectType> getObjectTypes() {
        if (objectTypes == null) {
            buildTypeLists();
        }

        return objectTypes;
    }

    public static VueOntologyType getVueType(URI uri) {
        if (typesByURI == null) {
            buildTypeLists();
        }

        // use same URI format as typesByURI
        return typesByURI.get(SPARQLDeserializers.getShortURI(uri));
    }

    public void setPropertiesOrder(URI classURI, List<URI> propertiesURI, String lang) throws Exception {

        ArrayList<String> extendedPropertiesString = new ArrayList<>();

        for (URI propertyURI : propertiesURI) {
            String propertyURIString = SPARQLDeserializers.getExpandedURI(propertyURI);
            extendedPropertiesString.add(propertyURIString);
        }

        List<VueClassPropertyExtensionModel> models = new ArrayList<>();
        for (String extendedProperty : extendedPropertiesString) {
            VueClassPropertyExtensionModel model = new VueClassPropertyExtensionModel();
            model.setFromOwlClass(classURI);
            model.setToOwlProperty(new URI(extendedProperty));
            model.setHasDisplayOrder(extendedPropertiesString.indexOf(extendedProperty));
            models.add(model);
        }

        try {
            sparql.startTransaction();

            List<URI> existingProperties = sparql.searchURIs(VueClassPropertyExtensionModel.class, lang, (select) -> {
                select.addFilter(SPARQLQueryHelper.eq(VueClassPropertyExtensionModel.OWL_CLASS_FIELD, classURI));
            });
            if (existingProperties.size() > 0) {
                sparql.delete(VueClassPropertyExtensionModel.class, existingProperties);
            }
            if (models.size() > 0) {
                sparql.create(VueClassPropertyExtensionModel.class, models);
            }
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }

    public List<URI> getPropertiesOrder(URI classURI, String lang) throws Exception {

        List<VueClassPropertyExtensionModel> existingProperties = sparql.search(VueClassPropertyExtensionModel.class, lang, (select) -> {
            select.addFilter(SPARQLQueryHelper.eq(VueClassPropertyExtensionModel.OWL_CLASS_FIELD, classURI));
        });
        return existingProperties.stream().sorted(
                (VueClassPropertyExtensionModel a, VueClassPropertyExtensionModel b) -> {
                    if (SPARQLDeserializers.compareURIs(a.getToOwlProperty(), b.getToOwlProperty())) {
                        return 0;
                    }
                    if (SPARQLDeserializers.compareURIs(a.getToOwlProperty(), RDFS.label.getURI())) {
                        return -1;
                    }
                    if (SPARQLDeserializers.compareURIs(b.getToOwlProperty(), RDFS.label.getURI())) {
                        return 1;
                    }
                    return a.getHasDisplayOrder() - b.getHasDisplayOrder();
                }).map(
                        (VueClassPropertyExtensionModel a) -> a.getToOwlProperty()
                ).collect(Collectors.toList());

    }
}
