/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyObjectType;
import org.opensilex.front.vueOwlExtension.types.VueOntologyType;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class VueOwlExtensionDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(VueOwlExtensionDAO.class);

    private final SPARQLService sparql;

    public VueOwlExtensionDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public void createExtendedClass(ClassModel instance, VueClassExtensionModel instanceExtension) throws Exception {
        try {
            sparql.startTransaction();
            sparql.create(instance);
            sparql.create(instanceExtension);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }

    public void updateExtendedClass(ClassModel instance, VueClassExtensionModel instanceExtension) throws Exception {
        try {
            sparql.startTransaction();
            sparql.update(instance);
            sparql.update(instanceExtension);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }
    
    public void deleteExtendedClass(URI classURI) throws Exception {
        try {
            sparql.startTransaction();
            sparql.delete(ClassModel.class, classURI);
            sparql.delete(VueClassExtensionModel.class, classURI);
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }
    }

    
    
    private static List<VueOntologyDataType> dataTypes;
    private static List<VueOntologyObjectType> objectTypes;
    private static Map<String, VueOntologyType> typesByURI;

    private static void buildTypeLists() {

        dataTypes = new ArrayList<>();
        objectTypes = new ArrayList<>();
        typesByURI = new HashMap<>();

        ServiceLoader.load(VueOntologyType.class, OpenSilex.getClassLoader())
                .forEach((type -> {
                    typesByURI.put(SPARQLDeserializers.getExpandedURI(type.getUri()), type);
                }));

        for (VueOntologyType ontologyType : typesByURI.values()) {
            if (ontologyType instanceof VueOntologyDataType) {
                dataTypes.add((VueOntologyDataType) ontologyType);
            } else if (ontologyType instanceof VueOntologyObjectType) {
                objectTypes.add((VueOntologyObjectType) ontologyType);
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

        return typesByURI.get(SPARQLDeserializers.getExpandedURI(uri));
    }

}
