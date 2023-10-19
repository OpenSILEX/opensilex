/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.annotations.SPARQLManualLoading;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SPARQLClassObjectMapperIndex {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassObjectMapperIndex.class);

    private Set<Class<? extends SPARQLResourceModel>> classes;

    private Map<Class<?>, SPARQLClassObjectMapper<? extends SPARQLResourceModel>> classesMapper = new HashMap<>();
    private Map<Resource, SPARQLClassObjectMapper<? extends SPARQLResourceModel>> resourcesMapper = new HashMap<>();
    private Map<Class<? extends SPARQLResourceModel>, Map<Class<? extends SPARQLResourceModel>, Field>> reverseRelationIndex = new HashMap<>();

    private final URI baseGraphURI;
    
    private final URI generationPrefixURI;
    

    public SPARQLClassObjectMapperIndex(URI baseGraphURI, URI generationPrefixURI, Set<Class<? extends SPARQLResourceModel>> initClasses) throws SPARQLInvalidClassDefinitionException {
        this.baseGraphURI = baseGraphURI;
        this.generationPrefixURI = generationPrefixURI;
        this.classes = initClasses;
        addClasses(initClasses);
    }

    private <T> Class<? super T> getConcreteClass(Class<T> objectClass) {
        if (SPARQLProxyMarker.class.isAssignableFrom(objectClass)) {
            return getConcreteClass(objectClass.getSuperclass());
        } else {
            return objectClass;
        }
    }

    public void forEach(BiConsumer<Resource, SPARQLClassObjectMapper<?>> lambda) throws SPARQLInvalidClassDefinitionException {
        resourcesMapper.forEach(lambda);
    }

    public void addClasses(Class<? extends SPARQLResourceModel>... newClasses) throws SPARQLInvalidClassDefinitionException {
        List<Class<? extends SPARQLResourceModel>> classList = Arrays.asList(newClasses);
        this.classes.addAll(classList);
        addClasses(classList);
    }

    @SuppressWarnings("unchecked")
    private void addClasses(Collection<Class<? extends SPARQLResourceModel>> newClasses) throws SPARQLInvalidClassDefinitionException {
        newClasses.forEach((clazz) -> {
            LOGGER.debug("SPARQL Resource class found: " + clazz.getCanonicalName());
        });

        newClasses.removeIf((Class<?> resource) -> {
            SPARQLManualLoading manualAnnotation = resource.getAnnotation(SPARQLManualLoading.class);
            return (manualAnnotation != null);
        });

        for (Class<?> sparqlModelClass : newClasses) {
            Class<? extends SPARQLResourceModel> sparqlResourceModelClass = (Class<? extends SPARQLResourceModel>) sparqlModelClass;
            SPARQLClassObjectMapper<?> mapper = new SPARQLClassObjectMapper<>(sparqlResourceModelClass, baseGraphURI, generationPrefixURI, this);
            classesMapper.put(sparqlModelClass, mapper);
        }

        for (SPARQLClassObjectMapper<? extends SPARQLResourceModel> mapperUninit : classesMapper.values()) {
            mapperUninit.init();
            resourcesMapper.put(mapperUninit.getRDFType(), mapperUninit);
        }

        for (SPARQLClassObjectMapper<? extends SPARQLResourceModel> mapperInit : classesMapper.values()) {
            Set<Class<? extends SPARQLResourceModel>> resources = mapperInit.classAnalyzer.getRelatedResources();

            Class<? extends SPARQLResourceModel> currentObjectClass = mapperInit.getObjectClass();
            if (!reverseRelationIndex.containsKey(currentObjectClass)) {
                reverseRelationIndex.put(currentObjectClass, new HashMap<>());
            }

            resources.forEach((relatedObjectClass) -> {
                if (!reverseRelationIndex.containsKey(relatedObjectClass)) {
                    reverseRelationIndex.put(relatedObjectClass, new HashMap<>());
                }
                Map<Class<? extends SPARQLResourceModel>, Field> relatedMap = reverseRelationIndex.get(relatedObjectClass);

                Set<Field> fields = mapperInit.classAnalyzer.getFieldsRelatedTo(relatedObjectClass);

                fields.forEach(field -> {
                    if (!mapperInit.isReverseRelation(field)) {
                        relatedMap.put(currentObjectClass, field);
                    }
                });
            });
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SPARQLResourceModel> SPARQLClassObjectMapper<T> getForClass(Class<?> objectClass) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        Class<T> concreteObjectClass = (Class<T>) getConcreteClass(objectClass);

        if (classesMapper.containsKey(concreteObjectClass)) {
            return (SPARQLClassObjectMapper<T>) classesMapper.get(concreteObjectClass);
        } else {
            throw new SPARQLMapperNotFoundException(concreteObjectClass);
        }
    }

    public Set<Class<?>> getResourceClasses() {
        return Collections.unmodifiableSet(classesMapper.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T extends SPARQLResourceModel> SPARQLClassObjectMapper<T> getForResource(Resource resource) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        if (resourcesMapper.containsKey(resource)) {
            return (SPARQLClassObjectMapper<T>) resourcesMapper.get(resource);
        } else {
            throw new SPARQLMapperNotFoundException(resource);
        }

    }

    public Iterator<Map.Entry<Class<? extends SPARQLResourceModel>, Field>> getReverseReferenceIterator(Class<? extends SPARQLResourceModel> objectClass) {
        return reverseRelationIndex.get(objectClass).entrySet().iterator();
    }

    public boolean existsForClass(Class<?> c) throws SPARQLInvalidClassDefinitionException {
        return classes.contains(c);
    }

    public void reset() {
        classes = null;
        classesMapper = new HashMap<>();
        resourcesMapper = new HashMap<>();
        reverseRelationIndex = new HashMap<>();
    }
}
