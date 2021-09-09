package org.opensilex.core.ontology.dal.cache;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public abstract class AbstractOntologyCache implements OntologyCache {

    protected final OntologyDAO ontologyDAO;
    protected final URI topDataProperty;
    protected final URI topObjectProperty;

    protected static class CachedTranslation {

        protected final Map<String,String> names;
        protected final Map<String,String> descriptions;

        public CachedTranslation(SPARQLLabel name, SPARQLLabel description) {
            this.names = name != null ? name.getAllTranslations() : Collections.emptyMap();
            this.descriptions = description != null ? description.getAllTranslations() : Collections.emptyMap();
        }

        public Map<String, String> getNames() {
            return names;
        }

        public Map<String, String> getDescriptions() {
            return descriptions;
        }
    }

    protected AbstractOntologyCache(SPARQLService sparql) throws URISyntaxException, SPARQLException {


        this.ontologyDAO = new OntologyDAO(sparql);
        this.buildCache();
        topDataProperty = new URI(OWL2.topDataProperty.getURI());
        topObjectProperty = new URI(OWL2.topObjectProperty.getURI());

        List<Resource> rootModelsToLoad = Arrays.asList(
                Oeso.Device,
                Oeev.Event,
                Oeso.InfrastructureFacility,
                Oeso.ScientificObject
        );

        List<URI> classUris = new ArrayList<>();
        for (Resource rootClass : rootModelsToLoad) {
            classUris.add(new URI(rootClass.getURI()));
        }

        this.populate(classUris);
    }

    protected abstract void buildCache();

    @Override
    public void populate(List<URI> classUris) throws SPARQLException {

        Objects.requireNonNull(classUris);

        for (URI classUri : classUris) {
            getSubClassesOf(classUri, null, OpenSilex.DEFAULT_LANGUAGE, false);
        }
    }




    @Override
    public SPARQLTreeListModel<ClassModel> getSubClassesOf(URI classUri, String stringPattern, String lang, boolean ignoreRootClasses) throws SPARQLException {

        try {
            // no caching of search with some string pattern
            if (!StringUtils.isEmpty(stringPattern)) {
                return ontologyDAO.searchSubClasses(classUri, ClassModel.class, stringPattern, lang, false, null);
            }

            ClassModel classModel = getClassModel(classUri, lang);
            if (classModel == null) {
                return null;
            }

            return new SPARQLTreeListModel<>(classModel, ignoreRootClasses, true);

        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public ClassModel getClassModel(URI classUri, URI parentClassUri, String lang) throws SPARQLException {
        URI formattedUri = SPARQLDeserializers.formatURI(classUri);
        ClassModel classModel = getClassFromCache(formattedUri);
        if (classModel == null) {
            classModel = getClassFromDao(classUri, parentClassUri, lang);
            if (classModel == null) {
                return null;
            }
            addClass(formattedUri, classModel, false);
        }

        classModel.visit(model -> {
            applyTranslation(model.getLabel(),lang);
            applyTranslation(model.getComment(),lang);
        });
        return classModel;
    }


    @Override
    public ClassModel getClassModel(URI classUri, String lang) throws SPARQLException {
        return getClassModel(classUri, null, lang);
    }

    private void addClass(URI formattedClassUri, ClassModel classModel, boolean buildProperties) throws SPARQLException {

        if (buildProperties) {
            try {
                ontologyDAO.buildProperties(classModel, OpenSilex.DEFAULT_LANGUAGE);
            } catch (Exception e) {
                throw new SPARQLException(e);
            }
        }

        addClassToCache(formattedClassUri, classModel);
        loadAllTranslations(classModel);

        if (classModel.getChildren() == null) {
            return;
        }

        // repeat caching of class/class props recursively for each class children
        for (ClassModel children : classModel.getChildren()) {
            addClass(SPARQLDeserializers.formatURI(children.getUri()), children, true);
        }
    }

    private SPARQLLabel getLabelWithAllTranslation(SPARQLLabel label) {
        if (label == null) {
            return null;
        }
        SPARQLLabel labelWithAllTranslate = new SPARQLLabel(label.getDefaultValue(), label.getDefaultLang());
        labelWithAllTranslate.setTranslations(label.getAllTranslations());
        return labelWithAllTranslate;
    }

    private void loadAllTranslations(ClassModel classModel){

        classModel.setLabel(getLabelWithAllTranslation(classModel.getLabel()));
        classModel.setComment(getLabelWithAllTranslation(classModel.getComment()));

        if(classModel.getDatatypeProperties() != null){
            classModel.getDatatypeProperties().forEach((uri, datatypePropertyModel) -> {
                datatypePropertyModel.setLabel(getLabelWithAllTranslation(datatypePropertyModel.getLabel()));
                datatypePropertyModel.setComment(getLabelWithAllTranslation(datatypePropertyModel.getComment()));
            });
        }

        if(classModel.getObjectProperties() != null){
            classModel.getObjectProperties().forEach((uri, objectPropertyModel) -> {
                objectPropertyModel.setLabel(getLabelWithAllTranslation(objectPropertyModel.getLabel()));
                objectPropertyModel.setComment(getLabelWithAllTranslation(objectPropertyModel.getComment()));
            });
        }
    }

    private void applyTranslation(SPARQLLabel label, String lang){
        if(label != null && ! StringUtils.isEmpty(lang)){
            Map<String,String> translations = label.getTranslations();
            if(translations.containsKey(lang)){
                label.setDefaultLang(lang);
                label.setDefaultValue(translations.get(lang));
            }
        }
    }

    protected ClassModel getClassFromDao(URI classUri, URI parentClass, String lang) throws SPARQLException {

        try {
            return ontologyDAO.getClassModel(classUri, parentClass, lang);
        } catch (Exception e) {
            throw new SPARQLException(e);
        }
    }

    @Override
    public void removeClass(URI classUris) {
        ClassModel classModel = getClassFromCache(classUris);
        if (classModel != null) {
            removeClass(classModel);
        }
    }

    private void removeClass(ClassModel classModel) {

        URI formattedUri = SPARQLDeserializers.formatURI(classModel.getUri());
        removeClassFromCache(formattedUri);

        if(classModel.getChildren() != null){
            classModel.getChildren().forEach(this::removeClass);
        }
    }

    protected abstract void addClassToCache(URI key, ClassModel classModel) throws SPARQLException;

    protected abstract ClassModel getClassFromCache(URI classUri);

    protected abstract void removeClassFromCache(URI classUri);

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> getDataProperties(URI domain, String lang) throws SPARQLException {
        ClassModel classModel = getClassModel(SPARQLDeserializers.formatURI(domain), lang);
        if (classModel == null) {
            return null;
        }

        List<DatatypePropertyModel> properties = classModel.getDatatypePropertiesWithDomain();
        properties.forEach(property -> {
            applyTranslation(property.getLabel(), lang);
            applyTranslation(property.getComment(), lang);
        });

        return new SPARQLTreeListModel<>(
                properties,
                topDataProperty,
                true,
                true);
    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> getObjectProperties(URI domain, String lang) throws SPARQLException {
        ClassModel classModel = getClassModel(SPARQLDeserializers.formatURI(domain), lang);
        if (classModel == null) {
            return null;
        }

        List<ObjectPropertyModel> properties = classModel.getObjectPropertiesWithDomain();
        properties.forEach(property -> {
            applyTranslation(property.getLabel(), lang);
            applyTranslation(property.getComment(), lang);
        });

        return new SPARQLTreeListModel<>(
                properties,
                topObjectProperty,
                true,
                true);
    }

    @Override
    public void createDataProperty(DatatypePropertyModel property) throws SPARQLException {
        if (property.getDomain() == null) {
            return;
        }
        ClassModel classModel = getClassModel(property.getDomain().getUri(), null);
        if (classModel == null) {
            return;
        }
        classModel.getDatatypeProperties().put(property.getUri(), property);
    }

    @Override
    public void updateDataProperty(DatatypePropertyModel property) throws SPARQLException {
        if (property.getDomain() == null) {
            return;
        }
        ClassModel classModel = getClassModel(property.getDomain().getUri(), null);
        if (classModel == null) {
            return;
        }
        classModel.getDatatypeProperties().replace(property.getUri(), property);
    }

    @Override
    public void deleteDataProperty(URI propertyURI, URI domain) throws SPARQLException {
        ClassModel classModel = getClassModel(domain, null);
        if (classModel == null) {
            return;
        }
        classModel.getDatatypeProperties().remove(propertyURI);
    }

    @Override
    public void createObjectProperty(ObjectPropertyModel property) throws SPARQLException {
        if (property.getDomain() == null) {
            return;
        }
        ClassModel classModel = getClassModel(property.getDomain().getUri(), null);
        if (classModel == null) {
            return;
        }
        classModel.getObjectProperties().put(property.getUri(), property);
    }

    @Override
    public void updateObjectProperty(ObjectPropertyModel property) throws SPARQLException {
        if (property.getDomain() == null) {
            return;
        }
        ClassModel classModel = getClassModel(property.getDomain().getUri(), null);
        if (classModel == null) {
            return;
        }
        classModel.getObjectProperties().replace(property.getUri(), property);
    }


    @Override
    public void deleteObjectProperty(URI propertyURI, URI domain) throws SPARQLException {
        ClassModel classModel = getClassModel(domain, null);
        if (classModel == null) {
            return;
        }
        classModel.getObjectProperties().remove(propertyURI);
    }

    @Override
    public void addRestriction(OwlRestrictionModel restriction) throws SPARQLException {
        if (restriction.getDomain() == null) {
            return;
        }
        ClassModel classModel = getClassModel(restriction.getDomain(), null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().put(restriction.getUri(), restriction);
    }

    @Override
    public void updateRestriction(OwlRestrictionModel restriction) throws SPARQLException {
        if (restriction.getDomain() == null) {
            return;
        }
        ClassModel classModel = getClassModel(restriction.getDomain(), null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().replace(restriction.getUri(), restriction);

    }

    @Override
    public void deleteRestriction(URI restrictionUri, URI domain) throws SPARQLException {

        ClassModel classModel = getClassModel(domain, null);
        if (classModel == null) {
            return;
        }
        classModel.getRestrictions().remove(restrictionUri);
    }


}
