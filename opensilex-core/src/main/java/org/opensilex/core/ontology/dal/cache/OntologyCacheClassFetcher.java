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
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.PropertyModel;
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
    protected final Var rootClassType = makeVar("rootType");

    protected final Node[] topPropertiesNodes = {
            NodeFactory.createURI(OWL2.topDataProperty.toString()),
            NodeFactory.createURI(OWL2.topObjectProperty.toString())
    };

    protected final List<String> languages;
    protected final List<Var> commentVars;
    protected final List<Var> nameVars;

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
                .addWhere(uriVar, RDF.type.asNode(), typeVar)
                .addWhere(uriVar, RDFS.domain.asNode(), domainVar)
                .addWhere(domainVar, Ontology.subClassAny, rootClassType);

        appendNamesAndComments(select);

        SPARQLQueryHelper.appendValueStream(select,typeVar, Arrays.stream(topPropertiesNodes));

        Stream<Node> rootTypesNodes = rootClasses.map(SPARQLDeserializers::nodeURI);
        SPARQLQueryHelper.appendValueStream(select, rootClassType, rootTypesNodes);

        return select;
    }

    private List<ClassEntry> getRootEntries(Collection<URI> uris) throws SPARQLException {

        SelectBuilder select = getClassesQuery(uris);

        return sparql.executeSelectQueryAsStream(select)
                .map(result -> {
                    ClassModel model = new ClassModel();
                    setModel(model, result);

                    ClassEntry entry = new ClassEntry();
                    entry.classModel = model;
                    return entry;
                })
                .collect(Collectors.toList());
    }


    protected void setModel(ClassModel model, SPARQLResult result) {

        try{
            model.setUri(new URI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            model.setType(new URI(OWL2.Class.getURI()));
            model.setTypeLabel(new SPARQLLabel("Class", OpenSilex.DEFAULT_LANGUAGE));

            model.setLabel(new SPARQLLabel());
            model.setComment(new SPARQLLabel());

            for (int i = 0; i < languages.size(); i++) {

                String varName = nameVars.get(i).getVarName();
                String translatedName = result.getStringValue(varName);
                if(!StringUtils.isEmpty(translatedName)){
                    model.getLabel().addTranslation(translatedName, languages.get(i));
                }

                String varComment = commentVars.get(i).getVarName();
                String translatedComment = result.getStringValue(varComment);
                if(!StringUtils.isEmpty(translatedComment)){
                    model.getComment().addTranslation(translatedComment, languages.get(i));
                }
            }

            Map<String,String> labelTranslations = model.getLabel().getTranslations();
            if(labelTranslations.containsKey(OpenSilex.DEFAULT_LANGUAGE)){
                model.getLabel().setDefaultValue(labelTranslations.get(OpenSilex.DEFAULT_LANGUAGE));
                model.getLabel().setDefaultLang(OpenSilex.DEFAULT_LANGUAGE);
            }

            Map<String,String> commentTranslations = model.getComment().getTranslations();
            if(commentTranslations.containsKey(OpenSilex.DEFAULT_LANGUAGE)){
                model.getComment().setDefaultValue(commentTranslations.get(OpenSilex.DEFAULT_LANGUAGE));
                model.getComment().setDefaultLang(OpenSilex.DEFAULT_LANGUAGE);
            }

        }catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    private Map<String,ClassEntry> getEntriesByUri(List<ClassEntry> rootEntries) throws SPARQLException {

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
            setModel(entry.classModel, result);

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

    private PropertyModel getProperty(SPARQLResult result) {
        return null;
    }

    private void fillProperties(List<ClassEntry> rootEntries, Map<String,ClassEntry> classEntryByUri) throws SPARQLException {

        Stream<URI> entriesUris = rootEntries.stream().map(entry -> entry.classModel.getUri());

        SelectBuilder select = getSearchPropertiesQuery(entriesUris);
        sparql.executeSelectQueryAsStream(select).forEach(result -> {

        });
    }


    public Collection<ClassEntry> getClassEntries(Collection<URI> uris) throws SPARQLException {

        if(CollectionUtils.isEmpty(uris)){
            throw new IllegalArgumentException("Null or empty uris : "+uris);
        }
        if(uris.stream().anyMatch(Objects::isNull)){
            throw new IllegalArgumentException("Some element from uris is null. Please provide only valid URI");
        }

        List<ClassEntry> rootEntries = getRootEntries(uris);
        if(rootEntries.isEmpty()){
            return rootEntries;
        }

        Map<String,ClassEntry> classEntryByUri = getEntriesByUri(rootEntries);

        return classEntryByUri.values();
    }

}
