package org.opensilex.core.ontology.dal.cache;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.core.ontology.dal.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class OntologyCacheClassFetcher {

    protected final SPARQLService sparql;
    protected static  final ExprFactory exprFactory  = SPARQLQueryHelper.getExprFactory();

    protected static final String COMMENT_VAR_PREFIX = "comment";
    protected final Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
    protected final Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
    protected final Var parentVar = makeVar(SPARQLTreeModel.PARENT_FIELD);
    protected final Var domainVar = makeVar(DatatypePropertyModel.DOMAIN_FIELD);
    protected final Var rangeVar = makeVar(DatatypePropertyModel.RANGE_FIELD);
    protected final Var rootClassType = makeVar("rootType");
    protected final List<String> languages;
    protected final List<Var> commentVars;
    protected final List<Var> nameVars;

    protected final Node[] topPropertiesNodes = {
            NodeFactory.createURI(OWL2.topDataProperty.toString()),
            NodeFactory.createURI(OWL2.topObjectProperty.toString())
    };
    private final static SPARQLLabel defaultClassLabel = new SPARQLLabel(OWL2.Class.getLocalName(), OpenSilex.DEFAULT_LANGUAGE);
    private final static SPARQLLabel defaultDataPropertyLabel = new SPARQLLabel(OWL2.DatatypeProperty.getLocalName(), OpenSilex.DEFAULT_LANGUAGE);
    private final static SPARQLLabel defaultObjectPropertyLabel = new SPARQLLabel(OWL2.ObjectProperty.getLocalName(), OpenSilex.DEFAULT_LANGUAGE);

    public OntologyCacheClassFetcher(SPARQLService sparql, List<String> languages) {
        this.sparql = sparql;

        this.languages = languages;
        commentVars = languages.stream().map(lang -> makeVar(COMMENT_VAR_PREFIX+"_"+ lang)).collect(Collectors.toList());
        nameVars = languages.stream().map(lang -> makeVar(SPARQLNamedResourceModel.NAME_FIELD+"_" + lang)).collect(Collectors.toList());
    }

    private void appendNamesAndComments(SelectBuilder select){

        nameVars.forEach(select::addVar);
        commentVars.forEach(select::addVar);

        for (int i = 0; i < languages.size(); i++) {
            Var nameVar = nameVars.get(i);
            Var commentVar = commentVars.get(i);
            String lang = languages.get(i);

            select.addOptional(new WhereBuilder()
                    .addWhere(uriVar, RDFS.label, nameVar)
                    .addFilter(SPARQLQueryHelper.langFilter(nameVar.getVarName(), lang))
            );
            select.addOptional(new WhereBuilder()
                    .addWhere(uriVar, RDFS.comment, commentVar)
                    .addFilter(SPARQLQueryHelper.langFilter(commentVar.getVarName(), lang))
            );
        }
    }

    private SelectBuilder getClassesQuery(Collection<URI> rootClasses){

        SelectBuilder select = new SelectBuilder()
                .addVar(uriVar)
                .addWhere(uriVar, RDF.type.asNode(), OWL2.Class.asNode())
                .addFilter(exprFactory.not(exprFactory.exists(
                        new WhereBuilder()
                                .addWhere(uriVar, RDFS.subClassOf, parentVar)
                                .addFilter(SPARQLQueryHelper.inURIFilter(parentVar,rootClasses))
                )));

        appendNamesAndComments(select);

        Stream<Node> rootTypesNodes = rootClasses.stream().map(SPARQLDeserializers::nodeURI);
        SPARQLQueryHelper.appendValueStream(select, uriVar, rootTypesNodes);

        return select;
    }

    private SelectBuilder getSearchSubClassesQuery(Stream<URI> rootClasses){

        SelectBuilder select = new SelectBuilder()
                .addVar(uriVar)
                .addVar(parentVar)
                .addWhere(uriVar, Ontology.subClassAny, rootClassType)
                .addWhere(uriVar, RDFS.subClassOf, parentVar)
                .addFilter(exprFactory.isIRI(parentVar));

        appendNamesAndComments(select);

        Stream<Node> rootTypesNodes = rootClasses.map(SPARQLDeserializers::nodeURI);
        SPARQLQueryHelper.appendValueStream(select, rootClassType, rootTypesNodes);

        return select;
    }

    private SelectBuilder getSearchPropertiesQuery(Stream<URI> rootClasses){

        SelectBuilder select = new SelectBuilder()
                .addVar(uriVar)
                .addVar(typeVar)
                .addVar(domainVar)
                .addVar(rootClassType)
                .addVar(rangeVar)
                .addVar(parentVar)
                .addWhere(uriVar, RDF.type.asNode(), typeVar)
                .addWhere(uriVar, RDFS.domain.asNode(), domainVar)
                .addWhere(domainVar, Ontology.subClassAny, rootClassType)
                .addWhere(uriVar,RDFS.range,rangeVar)
                .addOptional(uriVar,RDFS.subPropertyOf,parentVar);

        appendNamesAndComments(select);

        SPARQLQueryHelper.appendValueStream(select,typeVar, Arrays.stream(topPropertiesNodes));

        Stream<Node> rootTypesNodes = rootClasses.map(SPARQLDeserializers::nodeURI);
        SPARQLQueryHelper.appendValueStream(select, rootClassType, rootTypesNodes);

        return select;
    }

    private List<ClassEntry> getUniqueRoots(Collection<URI> uris) throws SPARQLException {

        SelectBuilder select = getClassesQuery(uris);

        return sparql.executeSelectQueryAsStream(select)
                .map(result -> {
                    ClassModel model = new ClassModel();
                    setClass(model, result);

                    ClassEntry entry = new ClassEntry();
                    entry.classModel = model;
                    return entry;
                })
                .collect(Collectors.toList());
    }

    protected SPARQLLabel getLabel(SPARQLResult result, List<Var> translatedVars) {

        SPARQLLabel label = new SPARQLLabel();

        for (int i = 0; i < languages.size(); i++) {
            String var = translatedVars.get(i).getVarName();
            String value = result.getStringValue(var);
            if(!StringUtils.isEmpty(value)){
                label.addTranslation(value, languages.get(i));
            }
        }

        Map<String,String> labelTranslations = label.getTranslations();
        if(labelTranslations.containsKey(OpenSilex.DEFAULT_LANGUAGE)){
            label.setDefaultValue(labelTranslations.get(OpenSilex.DEFAULT_LANGUAGE));
            label.setDefaultLang(OpenSilex.DEFAULT_LANGUAGE);
        }

        return label;
    }

    protected void setClass(ClassModel model, SPARQLResult result) {
        try{
            model.setUri(new URI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            model.setType(new URI(OWL2.Class.getURI()));
            model.setTypeLabel(defaultClassLabel);
            model.setLabel(getLabel(result,nameVars));
            model.setComment(getLabel(result,commentVars));

        }catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    private Map<String,ClassEntry> getClassEntries(List<ClassEntry> rootEntries) throws SPARQLException {

        Map<String, ClassEntry> classEntryByUri = new PatriciaTrie<>();

        rootEntries.forEach(classEntry -> {
            String formattedClassUri = SPARQLDeserializers.formatURI(classEntry.classModel.getUri().toString());
            classEntryByUri.put(formattedClassUri, classEntry);
        });

        Stream<URI> rootUris = rootEntries.stream().map(entry -> entry.classModel.getUri());
        SelectBuilder searchSubClassesOf = getSearchSubClassesQuery(rootUris);

        sparql.executeSelectQueryAsStream(searchSubClassesOf).forEach(result -> {

            String modelUri = SPARQLDeserializers.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD));
            ClassEntry entry = classEntryByUri.get(modelUri);
//
            if (entry == null) {
                entry = new ClassEntry();
                entry.classModel = new ClassModel();
                classEntryByUri.put(modelUri, entry);
            }
            setClass(entry.classModel, result);

            String parentUri = SPARQLDeserializers.formatURI(result.getStringValue(parentVar.getVarName()));
            ClassEntry parentEntry = classEntryByUri.get(parentUri);

            // if parent has not been created yet, then just register it (parent will be filled later)
            if (parentEntry == null) {
                parentEntry = new ClassEntry();
                parentEntry.classModel = new ClassModel();
                classEntryByUri.put(parentUri, parentEntry);
            }
            parentEntry.classModel.getChildren().add(entry.classModel);
            entry.classModel.setParent(parentEntry.classModel);
        });

        return classEntryByUri;
    }


    private void setProperty(PropertyModel property,
                             SPARQLResult result,
                             Map<String,ClassEntry> classEntryByUri) throws URISyntaxException {

        property.setLabel(getLabel(result, nameVars));
        property.setComment(getLabel(result, commentVars));

        String domain = result.getStringValue(domainVar.getVarName());
        ClassEntry domainClassEntry = classEntryByUri.get(domain);
        ClassModel domainClass = domainClassEntry.classModel;
        property.setDomain(domainClassEntry.classModel);

        String range = result.getStringValue(rangeVar.getVarName());

        if (property instanceof DatatypePropertyModel) {
            DatatypePropertyModel dataProperty = (DatatypePropertyModel) property;
            dataProperty.setType(OntologyDAO.topDataPropertyUri);
            dataProperty.setTypeLabel(defaultDataPropertyLabel);
            dataProperty.setRange(new URI(range));
            dataProperty.setTypeRestriction(dataProperty.getRange());

            // update property list for current domain
            domainClass.getDatatypeProperties().put(dataProperty.getUri(), dataProperty);

            // update domain list of sub-properties
            domainClassEntry.dataPropertiesWithDomain.addTree(dataProperty);

            // update domain parent list of sub-properties
            if(domainClass.getParent() != null){
                ClassEntry domainParentEntry = classEntryByUri.get(domainClass.getParent().getUri().toString());
                if(domainParentEntry != null){
                    domainClassEntry.dataPropertiesWithDomain.addTree(dataProperty);
                }
            }

        } else if(property instanceof ObjectPropertyModel) {

            ObjectPropertyModel objectProperty = (ObjectPropertyModel) property;

            objectProperty.setType(OntologyDAO.topObjectPropertyUri);
            objectProperty.setTypeLabel(defaultObjectPropertyLabel);

            ClassEntry rangeClassEntry = classEntryByUri.get(range);
            objectProperty.setRange(rangeClassEntry.classModel);
            objectProperty.setTypeRestriction(rangeClassEntry.classModel.getUri());

            // update property list for current domain
            domainClassEntry.classModel.getObjectProperties().put(objectProperty.getUri(), objectProperty);

            // update domain-based indexes
            domainClassEntry.objectPropertiesWithDomain.addTree(objectProperty);

            // update domain parent list of sub-properties
            if(domainClass.getParent() != null){
                ClassEntry domainParentEntry = classEntryByUri.get(domainClass.getParent().getUri().toString());
                if(domainParentEntry != null){
                    domainClassEntry.objectPropertiesWithDomain.addTree(objectProperty);
                }
            }
        }

    }

    private void fillProperties(List<ClassEntry> rootEntries, Map<String,ClassEntry> classEntryByUri) throws SPARQLException {

        Stream<URI> entriesUris = rootEntries.stream().map(entry -> entry.classModel.getUri());
        Map<String,PropertyModel> propertyByUri = new PatriciaTrie<>();

        SelectBuilder select = getSearchPropertiesQuery(entriesUris);
        sparql.executeSelectQueryAsStream(select).forEach(result -> {

            try {
                String modelUri = SPARQLDeserializers.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD));
                PropertyModel property = propertyByUri.get(modelUri);
                Boolean isDataProperty = null;
                
                if (property == null) {
                    String type = result.getStringValue(SPARQLResourceModel.TYPE_FIELD);
                    isDataProperty = SPARQLDeserializers.compareURIs(type, OntologyDAO.topDataPropertyUri);
                    property = isDataProperty ? new DatatypePropertyModel() : new ObjectPropertyModel();

                    propertyByUri.put(modelUri, property);
                    property.setUri(new URI(modelUri));
                }
                if(isDataProperty == null){
                    isDataProperty = property instanceof DatatypePropertyModel;
                }

                String parentUri = SPARQLDeserializers.formatURI(result.getStringValue(parentVar.getVarName()));
                if (!StringUtils.isEmpty(parentUri)) {
                    PropertyModel parentProperty = propertyByUri.get(parentUri);

                    // if parent has not been created yet, then just register it (parent will be filled later)
                    if (parentProperty == null) {
                        parentProperty = isDataProperty ? new DatatypePropertyModel() : new ObjectPropertyModel();
                        propertyByUri.put(parentUri, parentProperty);
                    }
                    if(parentProperty instanceof DatatypePropertyModel){
                        ((DatatypePropertyModel) parentProperty).getChildren().add((DatatypePropertyModel) property);
                        ((DatatypePropertyModel) property).setParent((DatatypePropertyModel) parentProperty);

                    }else if(parentProperty instanceof ObjectPropertyModel){
                        ((ObjectPropertyModel) parentProperty).getChildren().add((ObjectPropertyModel) property);
                        ((ObjectPropertyModel) property).setParent((ObjectPropertyModel) parentProperty);
                    }
                }

                setProperty(property, result, classEntryByUri);

            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

        });
    }


    public Collection<ClassEntry> getClassEntries(Collection<URI> classUris) throws SPARQLException {

        if(CollectionUtils.isEmpty(classUris)){
            throw new IllegalArgumentException("Null or empty uris : "+classUris);
        }
        if(classUris.stream().anyMatch(Objects::isNull)){
            throw new IllegalArgumentException("Some element from uris is null. Please provide only valid URI");
        }

        List<ClassEntry> rootEntries = getUniqueRoots(classUris);
        if(rootEntries.isEmpty()){
            return rootEntries;
        }

        Map<String,ClassEntry> classEntryByUri = getClassEntries(rootEntries);
        fillProperties(rootEntries,classEntryByUri);

        return classEntryByUri.values();
    }

}
