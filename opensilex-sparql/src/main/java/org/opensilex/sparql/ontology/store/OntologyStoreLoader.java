/*******************************************************************************
 *                         OntologyStoreLoader.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.commons.collections4.CollectionUtils;
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
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.ontology.dal.*;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.jena.arq.querybuilder.Converters.makeVar;

/**
 * @author rcolin
 */
class OntologyStoreLoader {

    private final SPARQLService sparql;

    protected static final ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

    protected static final Var URI_VAR = makeVar(SPARQLResourceModel.URI_FIELD);
    protected static final Var PARENT_VAR = makeVar(SPARQLTreeModel.PARENT_FIELD);
    protected static final Var PROPERTY_DOMAIN_VAR = makeVar(AbstractPropertyModel.DOMAIN_FIELD);
    protected static final Var PROPERTY_RANGE_VAR = makeVar(AbstractPropertyModel.RANGE_FIELD);
    protected static final Var ROOT_PROPERTY_TYPE_VAR = makeVar("propertyType");

    protected static final Var ON_PROPERTY_VAR = makeVar(OwlRestrictionModel.ON_PROPERTY_FIELD);
    protected static final Var RESTRICTION_DOMAIN_VAR = makeVar(OwlRestrictionModel.DOMAIN_FIELD);

    private static final Node OWL_DATATYPE_PROPERTY = NodeFactory.createURI(OWL2.DatatypeProperty.getURI());
    private static final Node OWL_OBJECT_PROPERTY = NodeFactory.createURI(OWL2.ObjectProperty.getURI());

    protected final List<String> languages;
    protected final List<Var> commentVars;
    protected final List<Var> nameVars;

    OntologyStoreLoader(SPARQLService sparql, List<String> languages) {
        this.sparql = sparql;

        if (CollectionUtils.isEmpty(languages)) {
            throw new IllegalArgumentException("Languages must not be null or empty");
        }
        this.languages = languages;
        commentVars = languages.stream()
                .map(lang -> getLangVar(TranslatedModel.COMMENT_FIELD, lang))
                .collect(Collectors.toList());

        nameVars = languages.stream()
                .map(lang -> getLangVar(SPARQLNamedResourceModel.NAME_FIELD, lang))
                .collect(Collectors.toList());
    }

    private static Var getLangVar(String field, String lang) {
        String varName = field + "_" + (StringUtils.isEmpty(lang) ? "no_lang" : lang);
        return makeVar(varName);
    }

    private void fromResult(SPARQLResult result, TranslatedModel model) {
        model.setLabel(getLabel(result, nameVars));
        model.setComment(getLabel(result, commentVars));
    }

    private void fromResult(SPARQLResult result, AbstractPropertyModel<?> model) {

        String domainStr = result.getStringValue(AbstractPropertyModel.DOMAIN_FIELD);
        if (!StringUtils.isEmpty(domainStr)) {
            ClassModel domainClass = new ClassModel();
            domainClass.setUri(URIDeserializer.formatURI(domainStr));
            model.setDomain(domainClass);
        }

        String rangeStr = result.getStringValue(AbstractPropertyModel.RANGE_FIELD);
        if (!StringUtils.isEmpty(rangeStr)) {
            if (model instanceof DatatypePropertyModel) {
                ((DatatypePropertyModel) model).setRange(URIDeserializer.formatURI(rangeStr));
            } else {
                ClassModel rangeClass = new ClassModel();
                rangeClass.setUri(URIDeserializer.formatURI(rangeStr));
                ((ObjectPropertyModel) model).setRange(rangeClass);
            }
        }

    }

    private void fromResult(SPARQLResult result, OwlRestrictionModel model) throws Exception {

        model.setUri(URIDeserializer.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
        model.setType(AbstractOntologyStore.OWL_ROOT_RESTRICTION_MODEL.getUri());
        model.setTypeLabel(AbstractOntologyStore.OWL_ROOT_RESTRICTION_MODEL.getTypeLabel());

        model.setOnProperty(URIDeserializer.formatURI(result.getStringValue(OwlRestrictionModel.ON_PROPERTY_FIELD)));
        model.setDomain(URIDeserializer.formatURI(result.getStringValue(OwlRestrictionModel.DOMAIN_FIELD)));

        String onClass = result.getStringValue(OwlRestrictionModel.ON_CLASS);
        if (!StringUtils.isEmpty(onClass)) {
            ClassModel onClassModel = new ClassModel();
            onClassModel.setUri(URIDeserializer.formatURI(onClass));
            model.setOnClass(onClassModel);
        }

        String onDataRange = result.getStringValue(OwlRestrictionModel.ON_DATA_RANGE_FIELD);
        if (!StringUtils.isEmpty(onDataRange)) {
            model.setOnDataRange(URIDeserializer.formatURI(onDataRange));
        }

        String minCardinality = result.getStringValue(OwlRestrictionModel.MIN_CARDINALITY_FIELD);
        if (!StringUtils.isEmpty(minCardinality)) {
            model.setMinCardinality(SPARQLDeserializers.getForClass(Integer.class).fromString(minCardinality));
        }

        String maxCardinality = result.getStringValue(OwlRestrictionModel.MAX_CARDINALITY_FIELD);
        if (!StringUtils.isEmpty(maxCardinality)) {
            model.setMaxCardinality(SPARQLDeserializers.getForClass(Integer.class).fromString(maxCardinality));
        }

        String cardinality = result.getStringValue(OwlRestrictionModel.CARDINALITY_FIELD);
        if (!StringUtils.isEmpty(cardinality)) {
            model.setMaxCardinality(SPARQLDeserializers.getForClass(Integer.class).fromString(cardinality));
        }

        String someValuesFrom = result.getStringValue(OwlRestrictionModel.SOME_VALUES_FROM_FIELD);
        if (!StringUtils.isEmpty(someValuesFrom)) {
            model.setSomeValuesFrom(URIDeserializer.formatURI(someValuesFrom));
        }

    }

    private <T extends VocabularyModel<T>> List<T> getModels(
            SelectBuilder select,
            Function<SPARQLResult, T> modelConstructor,
            BiConsumer<SPARQLResult, T> resultToModel) throws SPARQLException {

        List<T> models = new ArrayList<>();
        AtomicReference<String> lastURI = new AtomicReference<>();

        sparql.executeSelectQueryAsStream(select).forEach(result -> {

            T model;
            String uri = URIDeserializer.getShortURI(result.getStringValue(SPARQLResourceModel.URI_FIELD));

            if (!uri.equals(lastURI.get())) {
                model = modelConstructor.apply(result);
                model.setUri(URI.create(uri));

                fromResult(result, model);
                models.add(model);

                lastURI.set(uri);
            } else {
                model = models.get(models.size() - 1);
            }

            String parentURI = result.getStringValue(SPARQLTreeModel.PARENT_FIELD);
            if (!StringUtils.isEmpty(parentURI)) {
                T parentModel = modelConstructor.apply(result);
                parentModel.setUri(URIDeserializer.formatURI(parentURI));
                model.getParents().add(parentModel);
            }

            if (resultToModel != null) {
                resultToModel.accept(result, model);
            }
        });

        return models;

    }

    List<ClassModel> getClasses() throws SPARQLException {
        return getModels(
                buildGetAllClassesQuery(),
                result -> new ClassModel(),
                (result, model) -> {
                    model.setType(AbstractOntologyStore.OWL_CLASS_MODEL.getUri());
                    model.setTypeLabel(AbstractOntologyStore.OWL_CLASS_MODEL.getTypeLabel());
                });
    }

    List<AbstractPropertyModel> getProperties() throws SPARQLException {

        return getModels(
                buildGetAllPropertiesQuery(),
                result -> {
                    String typeUri = result.getStringValue(ROOT_PROPERTY_TYPE_VAR.getVarName());

                    AbstractPropertyModel model;

                    if (SPARQLDeserializers.compareURIs(typeUri, OWL2.DatatypeProperty.getURI())) {
                        model = new DatatypePropertyModel();
                        model.setType(AbstractOntologyStore.OWL_DATATYPE_PROPERTY_MODEL.getUri());
                        model.setTypeLabel(AbstractOntologyStore.OWL_DATATYPE_PROPERTY_MODEL.getTypeLabel());
                    } else {
                        model = new ObjectPropertyModel();
                        model.setType(AbstractOntologyStore.OWL_OBJECT_PROPERTY_MODEL.getUri());
                        model.setTypeLabel(AbstractOntologyStore.OWL_OBJECT_PROPERTY_MODEL.getTypeLabel());
                    }
                    return model;
                },
                this::fromResult
        );

    }

    List<OwlRestrictionModel> getRestrictions() throws Exception {

        return sparql.searchAsStream(
                null,
                OwlRestrictionModel.class,
                null,
                select -> select.addFilter(exprFactory.and(
                        exprFactory.isIRI(ON_PROPERTY_VAR),
                        exprFactory.isIRI(RESTRICTION_DOMAIN_VAR)
                )),
                Collections.emptyMap(),
                result -> {
                    OwlRestrictionModel model = new OwlRestrictionModel();
                    fromResult(result, model);
                    return model;
                },
                Collections.emptyList(),
                null,
                null
        ).collect(Collectors.toList());

    }

    private void appendNamesAndComments(SelectBuilder select) {

        nameVars.forEach(select::addVar);
        commentVars.forEach(select::addVar);

        for (int i = 0; i < languages.size(); i++) {
            Var nameVar = nameVars.get(i);
            Var commentVar = commentVars.get(i);
            String lang = languages.get(i);

            select.addOptional(new WhereBuilder()
                    .addWhere(URI_VAR, RDFS.label, nameVar)
                    .addFilter(SPARQLQueryHelper.langFilter(nameVar.getVarName(), lang, false))
            );
            select.addOptional(new WhereBuilder()
                    .addWhere(URI_VAR, RDFS.comment, commentVar)
                    .addFilter(SPARQLQueryHelper.langFilter(commentVar.getVarName(), lang, false))
            );
        }
    }

    protected SPARQLLabel getLabel(SPARQLResult result, List<Var> translatedVars) {

        SPARQLLabel label = new SPARQLLabel();

        for (int i = 0; i < languages.size(); i++) {
            String varName = translatedVars.get(i).getVarName();
            String value = result.getStringValue(varName);
            if (!StringUtils.isEmpty(value)) {
                label.addTranslation(value, languages.get(i));
            }
        }

        return label;
    }


    private SelectBuilder buildGetAllClassesQuery() {

        final Node owlClassNode = NodeFactory.createURI(OWL2.Class.getURI());

        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addVar(PARENT_VAR)
                .addWhere(URI_VAR, RDF.type, owlClassNode)
                .addFilter(exprFactory.isIRI(URI_VAR))
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, RDFS.subClassOf, PARENT_VAR)
                        .addWhere(PARENT_VAR, RDF.type, owlClassNode)
                        .addFilter(exprFactory.isIRI(PARENT_VAR))
                ).addOrderBy(URI_VAR);

        appendNamesAndComments(select);

        return select;
    }

    private SelectBuilder buildGetAllPropertiesQuery() {

        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addVar(ROOT_PROPERTY_TYPE_VAR)
                .addVar(PARENT_VAR)
                .addVar(PROPERTY_RANGE_VAR)
                .addVar(PROPERTY_DOMAIN_VAR)
                .addWhere(URI_VAR, RDF.type, ROOT_PROPERTY_TYPE_VAR)
                .addFilter(exprFactory.isIRI(URI_VAR))
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, RDFS.subPropertyOf, PARENT_VAR)
                        .addWhere(PARENT_VAR, RDF.type, ROOT_PROPERTY_TYPE_VAR)
                        .addFilter(exprFactory.isIRI(PARENT_VAR))
                )
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, RDFS.domain, PROPERTY_DOMAIN_VAR)
                        .addFilter(exprFactory.isIRI(PROPERTY_DOMAIN_VAR))
                )
                .addOptional(new WhereBuilder().
                        addWhere(URI_VAR, RDFS.range, PROPERTY_RANGE_VAR)
                        .addFilter(exprFactory.isIRI(PROPERTY_RANGE_VAR))
                )
                .addOrderBy(URI_VAR);

        appendNamesAndComments(select);
        select.addWhereValueVar(ROOT_PROPERTY_TYPE_VAR, OWL_OBJECT_PROPERTY, OWL_DATATYPE_PROPERTY);

        return select;
    }
}
