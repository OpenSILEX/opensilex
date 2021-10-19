package org.opensilex.core.ontology.dal.cache;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.*;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ThrowingFunction;

import java.net.URI;
import java.net.URISyntaxException;
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

    static {
        try {
            rootModelsToLoad = Arrays.asList(
                    new URI(Oeso.ScientificObject.toString())
//                    new URI(Oeso.Document.toString()),
//                    new URI(Oeev.Event.toString()),
//                    new URI(Oeso.InfrastructureFacility.toString()),
//                    new URI(Oeso.Device.toString())
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected final SPARQLService sparql;
    protected final OntologyCacheClassFetcher classFetcher;
    protected final OntologyDAO ontologyDAO;

    // define default classes to cache during cache initialisation
    protected static List<URI> rootModelsToLoad;

    private static final String TOP_PROPERTY_NOT_LOADED_ERROR_MSG = "The %s property could not be loaded. " +
            "Please make sure that the OWL2 ontology (" + OWL2.getURI() + ") has been loaded into your RDF repository";

    protected final DatatypePropertyModel topDataTypeProperty;
    protected final ObjectPropertyModel topObjectProperty;


    public static List<URI> getRootModelsToLoad() {
        return new ArrayList<>(rootModelsToLoad);
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


    @Override
    public void populate(List<URI> classUris) throws OntologyCacheException {
        Objects.requireNonNull(classUris);

        for (URI classUri : classUris) {
            getOrCreateClassModel(classUri,OpenSilex.DEFAULT_LANGUAGE);
        }
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
        ClassModel classModel = getOrCreateClassModel(classUri, OpenSilex.DEFAULT_LANGUAGE);
        return classModel == null ? null : getEntry(classUri);
    }

    @Override
    public SPARQLTreeListModel<ClassModel> getSubClassesOf(URI classUri, String stringPattern, String lang, boolean ignoreRootClasses) throws OntologyCacheException {

        try {
            // no caching of search with some string pattern
            if (!StringUtils.isEmpty(stringPattern)) {
                return ontologyDAO.searchSubClasses(classUri, ClassModel.class, stringPattern, lang, false, null);
            }

            ClassModel classModel = getOrCreateClassModel(classUri, lang);
            if (classModel == null) {
                return null;
            }

            return new SPARQLTreeListModel<>(classModel, ignoreRootClasses, true);

        } catch (Exception e) {
            throw new RuntimeException(e);
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
//        addClass(formattedUri, classModel, true);
    }

    @Override
    public void removeClass(URI classUris) {
        ClassModel classModel = getClassFromCache(classUris);
        if (classModel != null) {
            removeClass(classModel);
        }
    }

    @Override
    public ClassModel getOrCreateClassModel(URI classUri, URI parentClassUri, String lang) throws OntologyCacheException {

        URI formattedUri = SPARQLDeserializers.formatURI(classUri);

        ClassEntry rootClassEntry = getEntry(formattedUri);
        if (rootClassEntry == null) {
            Collection<ClassEntry> entries = getEntriesFromDao(formattedUri);
            if (entries == null) {
                return null;
            }
            for(ClassEntry entry : entries){
                if(entry.classModel.getUri().equals(formattedUri)){
                    rootClassEntry = entry;
                }
                addOrReplaceEntry(entry);
            }
        }

        if(rootClassEntry == null){
            return null;
        }
        ClassModel classModel = rootClassEntry.classModel;

        // apply translation on model and on each model descendent
        classModel.visit(model -> {
            applyTranslation(model.getLabel(), lang);
            applyTranslation(model.getComment(), lang);
        });
        return classModel;
    }


    @Override
    public ClassModel getOrCreateClassModel(URI classUri, String lang) throws OntologyCacheException {
        return getOrCreateClassModel(classUri, null, lang);
    }


    private void addOrReplaceEntry(ClassEntry entry) throws OntologyCacheException {

        ClassEntry oldEntry = getEntry(entry.classModel.getUri());
        if(oldEntry != null){
            removeClassFromCache(entry.classModel.getUri());
        }
        addClassToCache(entry.classModel.getUri(),entry);
    }

    private void linkClassWithExistingParent(ClassModel classModel) {

        // link new class with existing parent into cache
        if (classModel.getParent() != null) {
            URI formattedParentUri = SPARQLDeserializers.formatURI(classModel.getParent().getUri());
            ClassEntry parentEntry = getEntry(formattedParentUri);

            if (parentEntry != null) {
                ClassModel parentFromCache = parentEntry.classModel;
                classModel.setParent(parentFromCache);

                if (!parentFromCache.getChildren().contains(classModel)) {
                    parentFromCache.getChildren().add(classModel);
                }
            }
        }
    }

    private SPARQLLabel getLabelWithAllTranslation(SPARQLLabel label) {
        return label == null ? null : new SPARQLLabel(label);
    }

    private void loadAllTranslations(Collection<? extends PropertyModel> properties) {
        if (properties == null) {
            return;
        }
        properties.forEach(this::loadAllTranslations);
    }

    private void loadAllTranslations(PropertyModel propertyModel) {
        propertyModel.setLabel(getLabelWithAllTranslation(propertyModel.getLabel()));
        propertyModel.setComment(getLabelWithAllTranslation(propertyModel.getComment()));
    }

    private void loadAllTranslations(ClassModel classModel) {

        // load class translation for label and comment
        classModel.setLabel(getLabelWithAllTranslation(classModel.getLabel()));
        classModel.setComment(getLabelWithAllTranslation(classModel.getComment()));

        // load data/object properties translations for label and comment
        loadAllTranslations(classModel.getDatatypeProperties().values());
        loadAllTranslations(classModel.getObjectProperties().values());
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

    protected Collection<ClassEntry> getEntriesFromDao(URI classUri) throws OntologyCacheException {
        try {
            return classFetcher.getClassEntries(Collections.singletonList(classUri));
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

    protected void addDataPropertiesToCache(URI domain, SPARQLTreeListModel<DatatypePropertyModel> properties) {
        ClassEntry entry = getEntry(domain);
        if (entry == null) {
            return;
        }
        entry.dataPropertiesWithDomain = properties;
    }

    protected void addObjectPropertiesToCache(URI domain, SPARQLTreeListModel<ObjectPropertyModel> properties) {
        ClassEntry entry = getEntry(domain);
        if (entry == null) {
            return;
        }
        entry.objectPropertiesWithDomain = properties;
    }

    protected <PropertyType extends SPARQLTreeModel<PropertyType> & PropertyModel> void addProperties(URI rootDomainUri, URI topPropertyUri,
                                                                                                      ThrowingFunction<URI, SPARQLTreeListModel<PropertyType>, Exception> searchPropertiesFunction,
                                                                                                      BiConsumer<URI, SPARQLTreeListModel<PropertyType>> addPropertiesToCacheFunction) throws OntologyCacheException {
        // search properties from dao, return all properties which have a domain which is a subClassOf* of <domain>
        SPARQLTreeListModel<PropertyType> properties;
        try {
            properties = searchPropertiesFunction.apply(rootDomainUri);
            if (properties == null) {
                return;
            }

        } catch (Exception e) {
            throw new OntologyCacheException(e);
        }

        // register properties for the current class
        addPropertiesToCacheFunction.accept(rootDomainUri, properties);

        Map<URI, Set<PropertyType>> propertiesByClasses = new HashMap<>();

        // traverse each property and aggregate these properties by domain (here, domain is the classModel or one of its child)
        properties.traverse(property -> {

            ClassModel domainClass = property.getDomain();
            if (domainClass != null) {
                URI formattedDomainUri = SPARQLDeserializers.formatURI(domainClass.getUri());

                // don't re-put properties for the root class
                if (!formattedDomainUri.equals(rootDomainUri)) {

                    // get or create set of properties for this domain and update it
                    Set<PropertyType> domainProperties = propertiesByClasses.computeIfAbsent(formattedDomainUri, uri -> new HashSet<>());
                    domainProperties.add(property);
                }
            }
            // load each property translations
            loadAllTranslations(property);
        });

        // register domain <-> properties into cache
        propertiesByClasses.forEach((domainUri, domainProperties) -> {
            // build the Tree representation of properties
            SPARQLTreeListModel<PropertyType> subTree = new SPARQLTreeListModel<>(
                    domainProperties,
                    topPropertyUri,
                    true,
                    true
            );

            // register properties for the current class
            addPropertiesToCacheFunction.accept(domainUri, subTree);
        });
    }

    protected void addDataProperties(URI rootDomainUri) throws Exception {
        addProperties(
                rootDomainUri,
                topDataTypeProperty.getUri(),
                domain -> ontologyDAO.searchDataProperties(domain, OpenSilex.DEFAULT_LANGUAGE),
                this::addDataPropertiesToCache
        );

    }

    protected void addObjectProperties(URI rootDomainUri) throws OntologyCacheException {
        addProperties(
                rootDomainUri,
                topObjectProperty.getUri(),
                domain -> ontologyDAO.searchObjectProperties(domain, OpenSilex.DEFAULT_LANGUAGE),
                this::addObjectPropertiesToCache
        );
    }

    protected <PT extends SPARQLTreeModel<PT> & PropertyModel> SPARQLTreeListModel<PT> getPropertiesOnDomain(URI domain, String lang,
                                                                                                             ThrowingFunction<URI, SPARQLTreeListModel<PT>, OntologyCacheException> getPropsFunction) throws OntologyCacheException {

        URI formattedDomain = SPARQLDeserializers.formatURI(domain);

        SPARQLTreeListModel<PT> properties = getPropsFunction.apply(formattedDomain);
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

    protected <PT extends SPARQLTreeModel<PT> & PropertyModel> void createOrUpdateProperty(PT property,
                                                                                           BiConsumer<ClassEntry, PT> entryPropertyBiConsumer,
                                                                                           UnaryOperator<PT> copyPropertyFunction) {
        if (property.getDomain() == null) {
            return;
        }
        URI domain = SPARQLDeserializers.formatURI(property.getDomain().getUri());
        ClassEntry entry = getEntry(domain);
        if (entry == null) {
            return;
        }

        PT copy = copyPropertyFunction.apply(property);
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
        ClassModel classModel = getOrCreateClassModel(restriction.getDomain(), null);
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
        ClassModel classModel = getOrCreateClassModel(restriction.getDomain(), null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().replace(restriction.getUri(), restriction);
    }

    @Override
    public void deleteRestriction(URI restrictionUri, URI domain) throws OntologyCacheException {

        ClassModel classModel = getOrCreateClassModel(domain, null);
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
