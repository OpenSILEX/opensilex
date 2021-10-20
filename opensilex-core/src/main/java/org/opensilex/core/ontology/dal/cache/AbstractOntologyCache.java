package org.opensilex.core.ontology.dal.cache;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.*;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Abstract class which handle caching logic for class {@link ClassModel}, properties {@link PropertyModel} and restrictions ({@link OwlRestrictionModel}. <br>
 * The effective data access primitives must be defined (read and write) into inherited classes.
 *
 * @author rcolin
 */
public abstract class AbstractOntologyCache implements OntologyCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOntologyCache.class);
    protected final SPARQLService sparql;
    protected final OntologyCacheClassFetcher classFetcher;
    protected final OntologyDAO ontologyDAO;

    // define default classes to cache during cache initialisation
    protected static Set<URI> rootModelsToLoad = Sets.newHashSet(
            URIDeserializer.formatURI(Oeso.ScientificObject.toString()),
            URIDeserializer.formatURI(Oeso.Document.toString()),
            URIDeserializer.formatURI(Oeev.Event.toString()),
            URIDeserializer.formatURI(Oeso.InfrastructureFacility.toString()),
            URIDeserializer.formatURI(Oeso.Device.toString())
    );

    private static final String TOP_PROPERTY_NOT_LOADED_ERROR_MSG = "The %s property could not be loaded. " +
            "Please make sure that the OWL2 ontology (" + OWL2.getURI() + ") has been loaded into your RDF repository";

    protected final DatatypePropertyModel topDataTypeProperty;
    protected final ObjectPropertyModel topObjectProperty;


    public static Set<URI> getRootModelsToLoad() {
        return rootModelsToLoad;
    }

    protected AbstractOntologyCache(SPARQLService sparql) throws OntologyCacheException {
        this.sparql = sparql;
        this.ontologyDAO = new OntologyDAO(sparql);

        try {
            topDataTypeProperty = sparql.loadByURI(DatatypePropertyModel.class, ontologyDAO.getTopDataPropertyUri(), OpenSilex.DEFAULT_LANGUAGE, null);
            topObjectProperty = sparql.loadByURI(ObjectPropertyModel.class, ontologyDAO.getTopObjectPropertyUri(), OpenSilex.DEFAULT_LANGUAGE, null);

            if (topDataTypeProperty == null) {
                throw new NotFoundURIException(String.format(TOP_PROPERTY_NOT_LOADED_ERROR_MSG, ontologyDAO.getTopDataPropertyUri()), ontologyDAO.getTopDataPropertyUri());
            }
            if (topObjectProperty == null) {
                throw new NotFoundURIException(String.format(TOP_PROPERTY_NOT_LOADED_ERROR_MSG, ontologyDAO.getTopObjectPropertyUri()), ontologyDAO.getTopObjectPropertyUri());
            }

            classFetcher = new OntologyCacheClassFetcher(sparql,Arrays.asList(OpenSilex.DEFAULT_LANGUAGE,"fr"));

        } catch (Exception e) {
            throw new OntologyCacheException(e);
        }

        this.buildCache();
    }

    protected abstract void buildCache();

    private static final String ONTOLOGY_CACHE_SUCCESS_MSG = "Populate [OK] {duration: %dms ,length: %d classes}";

    @Override
    public void populate(Set<URI> classUris) throws OntologyCacheException {
        Objects.requireNonNull(classUris);

        Instant begin = Instant.now();
        getOrCreateClasses(classUris,OpenSilex.DEFAULT_LANGUAGE);

        String successMsg = String.format(ONTOLOGY_CACHE_SUCCESS_MSG, Duration.between(begin, Instant.now()).toMillis(),length());
        LOGGER.info(successMsg);
    }

    /**
     * @param classUri URI of a {@link ClassModel}
     * @return the {@link ClassEntry} associated with the given classUri, return null if not found
     */
    protected abstract ClassEntry getEntry(URI classUri);

    protected ClassEntry getOrCreateEntry(URI classUri) throws OntologyCacheException {
        ClassEntry entry = getEntry(classUri);
        if (entry != null) {
            return entry;
        }
        ClassModel classModel = getOrCreateClass(classUri, OpenSilex.DEFAULT_LANGUAGE);
        return classModel == null ? null : getEntry(classUri);
    }

    @Override
    public SPARQLTreeListModel<ClassModel> getSubClassesOf(URI classUri, String stringPattern, String lang, boolean ignoreRootClasses) throws OntologyCacheException {

        try {
            // no caching of search with some string pattern
            if (!StringUtils.isEmpty(stringPattern)) {
                return ontologyDAO.searchSubClasses(classUri, ClassModel.class, stringPattern, lang, false, null);
            }

            ClassModel classModel = getOrCreateClass(classUri, lang);
            if (classModel == null) {
                return null;
            }

            return new SPARQLTreeListModel<>(classModel, ignoreRootClasses, true);

        } catch (Exception e) {
            throw new OntologyCacheException(e);
        }
    }

    @Override
    public void updateClass(ClassModel classModel) throws OntologyCacheException {

        URI formattedUri = SPARQLDeserializers.formatURI(classModel.getUri());
        ClassEntry entry = getEntry(formattedUri);
        if (entry == null) {
            return;
        }

        removeClass(entry.classModel);
    }

    @Override
    public void removeClass(URI classUris) {
        ClassModel classModel = getClassFromCache(classUris);
        if (classModel != null) {
            removeClass(classModel);
        }
    }

    protected List<ClassModel> getOrCreateClasses(Set<URI> classesUris, String lang) throws OntologyCacheException {

        List<ClassModel> classModels = new ArrayList<>();
        List<URI> classesToFetchFromDao = new LinkedList<>();

        // compute list of existing ClassModel and list of ClassModel to retrieve from the DAO
        for(URI classUri : classesUris){
            URI formattedUri = SPARQLDeserializers.formatURI(classUri);
            ClassEntry classEntry = getEntry(formattedUri);
            if (classEntry == null) {
                classesToFetchFromDao.add(formattedUri);
            }else{
                classModels.add(classEntry.classModel);
            }
        }

        // retrieve entries from dao
       if(! classesToFetchFromDao.isEmpty()){

           Collection<ClassEntry> entries = getEntriesFromDao(classesToFetchFromDao);
           for(ClassEntry entry : entries){

               // getEntriesFromDao() return the requested classes + theirs children, only add requested class to classModels list
               if(classesUris.contains(entry.classModel.getUri())){
                   classModels.add(entry.classModel);
               }
               addClassToCache(entry.classModel.getUri(),entry);
           }
       }

       // apply translation recursively on each classModel
       classModels.forEach(classModel -> classModel.visit(child -> {
           applyTranslation(child.getLabel(), lang);
           applyTranslation(child.getComment(), lang);
       }));

       return classModels;
    }

    @Override
    public ClassModel getOrCreateClass(URI classUri, URI parentClassUri, String lang) throws OntologyCacheException {

        List<ClassModel> classes = getOrCreateClasses(Collections.singleton(classUri),lang);
        return classes.isEmpty() ? null : classes.get(0);
    }


    @Override
    public ClassModel getOrCreateClass(URI classUri, String lang) throws OntologyCacheException {
        return getOrCreateClass(classUri, null, lang);
    }

    private void applyTranslation(SPARQLLabel label, String lang) {
        if (label != null && !StringUtils.isEmpty(lang)) {
            Map<String, String> translations = label.getTranslations();
            if (translations.containsKey(lang)) {
                label.setDefaultLang(lang);
                label.setDefaultValue(translations.get(lang));
            }
        }
    }

    protected Collection<ClassEntry> getEntriesFromDao(List<URI> classesUri) throws OntologyCacheException {
        try {
            return classFetcher.getClassEntries(classesUri);
        } catch (Exception e) {
            throw new OntologyCacheException(e);
        }
    }

    private void removeClass(ClassModel classModel) {

        URI formattedUri = SPARQLDeserializers.formatURI(classModel.getUri());
        removeClassFromCache(formattedUri);

        if (classModel.getParent() != null) {
            ClassEntry parentEntry = getEntry(SPARQLDeserializers.formatURI(classModel.getParent().getUri()));

            // if parent has entry into cache, then delete link with child
            if (parentEntry != null) {
                parentEntry.classModel.getChildren().removeIf(
                        child -> SPARQLDeserializers.compareURIs(child.getUri(), formattedUri)
                );
            }
        }

        if (classModel.getChildren() != null) {
            classModel.getChildren().forEach(this::removeClass);
        }
    }

    protected abstract void addClassToCache(URI key, ClassEntry entry) throws OntologyCacheException;

    protected ClassModel getClassFromCache(URI classUri) {
        ClassEntry entry = getEntry(classUri);
        if (entry == null) {
            return null;
        }
        return entry.classModel;
    }

    protected abstract void removeClassFromCache(URI classUri);

    protected SPARQLTreeListModel<DatatypePropertyModel> getDataPropertiesOnDomain(URI domain) throws OntologyCacheException {
        ClassEntry entry = getOrCreateEntry(domain);
        if (entry == null) {
            return null;
        }
        return entry.dataPropertiesWithDomain;
    }

    protected SPARQLTreeListModel<ObjectPropertyModel> getObjectPropertiesOnDomain(URI domain) throws OntologyCacheException {
        ClassEntry entry = getOrCreateEntry(domain);
        if (entry == null) {
            return null;
        }
        return entry.objectPropertiesWithDomain;
    }


    protected <PropertyType extends SPARQLTreeModel<PropertyType> & PropertyModel> SPARQLTreeListModel<PropertyType> getPropertiesOnDomain(URI domain, String lang,
                                                                                                                                           ThrowingFunction<URI, SPARQLTreeListModel<PropertyType>, OntologyCacheException> getPropsFunction) throws OntologyCacheException {
        URI formattedDomain = SPARQLDeserializers.formatURI(domain);

        SPARQLTreeListModel<PropertyType> properties = getPropsFunction.apply(formattedDomain);
        if (properties == null) {
            return new SPARQLTreeListModel<>();
        }

        properties.traverse(property -> {
            applyTranslation(property.getLabel(), lang);
            applyTranslation(property.getComment(), lang);
        });

        return properties;
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String lang) throws OntologyCacheException {
        return getPropertiesOnDomain(domain, lang, this::getDataPropertiesOnDomain);

    }

    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String lang) throws OntologyCacheException {
        return getPropertiesOnDomain(domain, lang, this::getObjectPropertiesOnDomain);
    }

    protected <PropertyType extends SPARQLTreeModel<PropertyType> & PropertyModel> void createOrUpdateProperty(PropertyType property,
                                                                                                               BiConsumer<ClassEntry, PropertyType> entryPropertyBiConsumer,
                                                                                                               UnaryOperator<PropertyType> copyPropertyFunction) {
        if (property.getDomain() == null) {
            return;
        }
        URI domain = SPARQLDeserializers.formatURI(property.getDomain().getUri());
        ClassEntry entry = getEntry(domain);
        if (entry == null) {
            return;
        }

        PropertyType copy = copyPropertyFunction.apply(property);
        copy.setDomain(entry.classModel);
        entryPropertyBiConsumer.accept(entry, copy);
    }

    @Override
    public void createDataProperty(DatatypePropertyModel property) throws OntologyCacheException {

        BiConsumer<ClassEntry, DatatypePropertyModel> createPropertyEntryConsumer = (entry, propertyModel) -> {
            entry.classModel.getDatatypeProperties().put(propertyModel.getUri(), propertyModel);
            entry.dataPropertiesWithDomain.addTreeWithParent(propertyModel, propertyModel.getParent());
        };
        createOrUpdateProperty(property, createPropertyEntryConsumer, DatatypePropertyModel::new);
    }

    @Override
    public void createObjectProperty(ObjectPropertyModel property) throws OntologyCacheException {

        BiConsumer<ClassEntry, ObjectPropertyModel> createPropertyEntryConsumer = (entry, propertyModel) -> {
            entry.classModel.getObjectProperties().put(propertyModel.getUri(), propertyModel);
            entry.objectPropertiesWithDomain.addTreeWithParent(propertyModel, propertyModel.getParent());
        };
        createOrUpdateProperty(property, createPropertyEntryConsumer, ObjectPropertyModel::new);
    }

    @Override
    public void updateDataProperty(DatatypePropertyModel property) throws OntologyCacheException {

        BiConsumer<ClassEntry, DatatypePropertyModel> updatePropertyEntryConsumer = (entry, propertyModel) -> {
            entry.classModel.getDatatypeProperties().replace(propertyModel.getUri(), propertyModel);

            entry.dataPropertiesWithDomain.remove(propertyModel.getUri());
            entry.dataPropertiesWithDomain.addTreeWithParent(propertyModel, propertyModel.getParent());
        };
        createOrUpdateProperty(property, updatePropertyEntryConsumer, DatatypePropertyModel::new);
    }

    @Override
    public void updateObjectProperty(ObjectPropertyModel property) throws OntologyCacheException {

        BiConsumer<ClassEntry, ObjectPropertyModel> updatePropertyEntryConsumer = (entry, propertyModel) -> {
            entry.classModel.getObjectProperties().replace(propertyModel.getUri(), propertyModel);

            entry.objectPropertiesWithDomain.remove(propertyModel.getUri());
            entry.objectPropertiesWithDomain.addTreeWithParent(propertyModel, propertyModel.getParent());
        };
        createOrUpdateProperty(property, updatePropertyEntryConsumer, ObjectPropertyModel::new);
    }

    protected void deleteProperty(URI domain, Consumer<ClassEntry> deletePropertyConsumer) {

        URI formattedDomain = SPARQLDeserializers.formatURI(domain);
        ClassEntry entry = getEntry(formattedDomain);
        if (entry == null) {
            return;
        }
        deletePropertyConsumer.accept(entry);
    }

    @Override
    public void deleteDataProperty(URI propertyURI, URI domain) throws OntologyCacheException {

        Consumer<ClassEntry> deletePropertyConsumer = entry -> {
            entry.classModel.getDatatypeProperties().remove(propertyURI);
            entry.dataPropertiesWithDomain.remove(propertyURI);
        };
        deleteProperty(domain, deletePropertyConsumer);
    }

    @Override
    public void deleteObjectProperty(URI propertyURI, URI domain) throws OntologyCacheException {

        Consumer<ClassEntry> deletePropertyConsumer = entry -> {
            entry.classModel.getObjectProperties().remove(propertyURI);
            entry.objectPropertiesWithDomain.remove(propertyURI);
        };

        deleteProperty(domain, deletePropertyConsumer);
    }

    @Override
    public void addRestriction(OwlRestrictionModel restriction) throws OntologyCacheException {
        if (restriction.getDomain() == null) {
            return;
        }
        ClassModel classModel = getOrCreateClass(restriction.getDomain(), null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().put(restriction.getUri(), restriction);
    }

    @Override
    public void updateRestriction(OwlRestrictionModel restriction) throws OntologyCacheException {
        if (restriction.getDomain() == null) {
            return;
        }
        ClassModel classModel = getOrCreateClass(restriction.getDomain(), null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().replace(restriction.getUri(), restriction);
    }

    @Override
    public void deleteRestriction(URI restrictionUri, URI domain) throws OntologyCacheException {

        ClassModel classModel = getOrCreateClass(domain, null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().remove(restrictionUri);
    }

    @Override
    public DatatypePropertyModel getTopDatatypePropertyModel() {
        return topDataTypeProperty;
    }

    @Override
    public ObjectPropertyModel getTopObjectPropertyModel() {
        return topObjectProperty;
    }
}
