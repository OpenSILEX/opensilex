/*******************************************************************************
 *                         OntologyStoreLoader.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

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
import static org.opensilex.sparql.ontology.store.AbstractOntologyStore.NO_LANG;

/**
 * Class intended to load {@link ClassModel}, {@link PropertyModel}, {@link OwlRestrictionModel} fastly.
 *
 * <pre>
 *    - Classes/Properties are retrieved in a single query (2 query)</li>
 *    - No proxying with SPARQLPRoxy -> no additional query :
 *          - (with proxy) : one query for all translations -> (here) : all lang are included in SELECT/WHERE
 *          - (with proxy) : one query for children fetching -> (here) : children link is build during query results read
 * </pre>
 *
 * @implNote All {@link URI} are generated with {@link URIDeserializer#formatURI(URI)}
 * @author rcolin
 */
class OntologyStoreLoader {

    private final SPARQLService sparql;
    protected static final ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

    private final SelectBuilder ALL_CLASSES_QUERY;
    private final SelectBuilder ALL_PROPERTIES_QUERY;

    protected static final Var URI_VAR = makeVar(SPARQLResourceModel.URI_FIELD);
    protected static final Var PARENT_VAR = makeVar(SPARQLTreeModel.PARENT_FIELD);
    protected static final Var DOMAIN_VAR = makeVar(AbstractPropertyModel.DOMAIN_FIELD);
    protected static final Var RANGE_VAR = makeVar(AbstractPropertyModel.RANGE_FIELD);
    protected static final Var ROOT_PROPERTY_TYPE_VAR = makeVar("propertyType");


    protected final List<String> languages;

    /**
     * The list of comment var according languages
     */
    protected final List<Var> commentVars;

    /**
     * The list of name var according languages
     */
    protected final List<Var> nameVars;


    OntologyStoreLoader(SPARQLService sparql, List<String> languages) {
        this.sparql = sparql;

        this.languages = languages;
        commentVars = languages.stream()
                .map(lang -> getLangVar(TranslatedModel.COMMENT_FIELD, lang))
                .collect(Collectors.toList());

        nameVars = languages.stream()
                .map(lang -> getLangVar(SPARQLNamedResourceModel.NAME_FIELD, lang))
                .collect(Collectors.toList());

        ALL_CLASSES_QUERY = buildGetAllClassesQuery();
        ALL_PROPERTIES_QUERY = buildGetAllPropertiesQuery();
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

        // set domain ClassModel
        String domainStr = result.getStringValue(AbstractPropertyModel.DOMAIN_FIELD);
        if (!StringUtils.isEmpty(domainStr)) {
            ClassModel domainClass = new ClassModel();
            domainClass.setUri(URIDeserializer.formatURI(domainStr));
            model.setDomain(domainClass);
        }

        // set data range
        String rangeStr = result.getStringValue(AbstractPropertyModel.RANGE_FIELD);
        if (!StringUtils.isEmpty(rangeStr)) {
            if (model instanceof DatatypePropertyModel) {
                ((DatatypePropertyModel) model).setRange(URIDeserializer.formatURI(rangeStr));
            } else {
                // set range ClassModel
                ClassModel rangeClass = new ClassModel();
                rangeClass.setUri(URIDeserializer.formatURI(rangeStr));
                ((ObjectPropertyModel) model).setRange(rangeClass);
            }
        }

    }

    private void fromResult(SPARQLResult result, OwlRestrictionModel model) throws Exception {

        // uri, type, and type label
        model.setUri(URIDeserializer.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
        model.setType(AbstractOntologyStore.OWL_ROOT_RESTRICTION_MODEL.getUri());
        model.setTypeLabel(AbstractOntologyStore.OWL_ROOT_RESTRICTION_MODEL.getTypeLabel());

        //  property and restricted class
        model.setOnProperty(URIDeserializer.formatURI(result.getStringValue(OwlRestrictionModel.ON_PROPERTY_FIELD)));
        ClassModel domain = new ClassModel();
        domain.setUri(URIDeserializer.formatURI(result.getStringValue(OwlRestrictionModel.DOMAIN_FIELD)));
        model.setDomain(domain);

        // onDataRange in case of datatype property
        String onDataRange = result.getStringValue(OwlRestrictionModel.ON_DATA_RANGE_FIELD);
        if (!StringUtils.isEmpty(onDataRange)) {
            model.setOnDataRange(URIDeserializer.formatURI(onDataRange));
        }

        // onClass in case of object property
        String onClass = result.getStringValue(OwlRestrictionModel.ON_CLASS);
        if (!StringUtils.isEmpty(onClass)) {
            model.setOnClass(URIDeserializer.formatURI(onClass));
        }

        // default,min and max cardinality
        String minCardinality = result.getStringValue(OwlRestrictionModel.MIN_CARDINALITY_FIELD);
        if (!StringUtils.isEmpty(minCardinality)) {
            model.setMinQualifiedCardinality(SPARQLDeserializers.getForClass(Integer.class).fromString(minCardinality));
        }

        String maxCardinality = result.getStringValue(OwlRestrictionModel.MAX_CARDINALITY_FIELD);
        if (!StringUtils.isEmpty(maxCardinality)) {
            model.setMaxQualifiedCardinality(SPARQLDeserializers.getForClass(Integer.class).fromString(maxCardinality));
        }

        String cardinality = result.getStringValue(OwlRestrictionModel.CARDINALITY_FIELD);
        if (!StringUtils.isEmpty(cardinality)) {
            model.setMaxQualifiedCardinality(SPARQLDeserializers.getForClass(Integer.class).fromString(cardinality));
        }

        // someValuesFrom in case of object property (in case of existential property according OWL2 definition)
        String someValuesFrom = result.getStringValue(OwlRestrictionModel.SOME_VALUES_FROM_FIELD);
        if (!StringUtils.isEmpty(someValuesFrom)) {
            model.setSomeValuesFrom(URIDeserializer.formatURI(someValuesFrom));
        }

    }

    private <T extends VocabularyModel<T>> List<T> getModels(
            SelectBuilder select,
            Function<SPARQLResult, T> modelConstructor,
            BiConsumer<SPARQLResult, T> updateModelFunction) throws SPARQLException {

        List<T> models = new ArrayList<>();
        AtomicReference<String> lastURI = new AtomicReference<>();

        sparql.executeSelectQueryAsStream(select).forEach(result -> {

            T model;
            String uri = URIDeserializer.getShortURI(result.getStringValue(SPARQLResourceModel.URI_FIELD));

            // first row with this URI, create object
            if (!uri.equals(lastURI.get())) {

                // create model, set properties and register it
                model = modelConstructor.apply(result);
                model.setUri(URI.create(uri));
                fromResult(result, model);
                models.add(model);

                lastURI.set(uri);
            } else {
                // two or more row per URI, just reuse the first and unique object created with this uri
                model = models.get(models.size() - 1);
            }

            // update parent
            String parentURI = result.getStringValue(SPARQLTreeModel.PARENT_FIELD);
            if (!StringUtils.isEmpty(parentURI)) {
                T parentModel = modelConstructor.apply(result);
                parentModel.setUri(URIDeserializer.formatURI(parentURI));
                model.getParents().add(parentModel);
            }

            // apply custom update function after objet init
            if (updateModelFunction != null) {
                updateModelFunction.accept(result, model);
            }
        });

        return models;

    }

    List<ClassModel> getClasses() throws SPARQLException {
        return getModels(
                ALL_CLASSES_QUERY,
                result -> new ClassModel(),
                (result, model) -> {
                    model.setType(AbstractOntologyStore.OWL_CLASS_MODEL.getUri());
                    model.setTypeLabel(AbstractOntologyStore.OWL_CLASS_MODEL.getTypeLabel());
                });
    }

    List<AbstractPropertyModel> getProperties() throws SPARQLException {

        return getModels(
                ALL_PROPERTIES_QUERY,
                result -> {
                    String typeUri = result.getStringValue(ROOT_PROPERTY_TYPE_VAR.getVarName());

                    AbstractPropertyModel model;

                    // init datatype property model
                    if (SPARQLDeserializers.compareURIs(typeUri, OWL2.DatatypeProperty.getURI())) {
                        model = new DatatypePropertyModel();
                        model.setType(AbstractOntologyStore.OWL_DATATYPE_PROPERTY_MODEL.getUri());
                        model.setTypeLabel(AbstractOntologyStore.OWL_DATATYPE_PROPERTY_MODEL.getTypeLabel());
                    } else {
                        // init object property model
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
                select -> {
                    select.addFilter(exprFactory.and(
                            exprFactory.isIRI(makeVar(OwlRestrictionModel.ON_PROPERTY_FIELD)),
                            exprFactory.isIRI(DOMAIN_VAR)
                    ));
                },
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
                    .addFilter(SPARQLQueryHelper.langFilter(nameVar, lang))
            );
            select.addOptional(new WhereBuilder()
                    .addWhere(URI_VAR, RDFS.comment, commentVar)
                    .addFilter(SPARQLQueryHelper.langFilter(commentVar, lang))
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
        label.setDefaultLang(NO_LANG);
        label.setDefaultValue(label.getAllTranslations().get(NO_LANG));
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
                .addVar(RANGE_VAR)
                .addVar(DOMAIN_VAR)
                .addWhere(URI_VAR, RDF.type, ROOT_PROPERTY_TYPE_VAR)
                .addFilter(exprFactory.isIRI(URI_VAR))
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, RDFS.subPropertyOf, PARENT_VAR)
                        .addWhere(PARENT_VAR, RDF.type, ROOT_PROPERTY_TYPE_VAR)
                        .addFilter(exprFactory.isIRI(PARENT_VAR))
                )
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, RDFS.domain, DOMAIN_VAR)
                        .addFilter(exprFactory.isIRI(DOMAIN_VAR))
                )
                .addOptional(new WhereBuilder().
                        addWhere(URI_VAR, RDFS.range, RANGE_VAR)
                        .addFilter(exprFactory.isIRI(RANGE_VAR))
                )
                .addOrderBy(URI_VAR);

        appendNamesAndComments(select);
        select.addWhereValueVar(
                ROOT_PROPERTY_TYPE_VAR,
                NodeFactory.createURI(OWL2.ObjectProperty.getURI()),
                NodeFactory.createURI(OWL2.DatatypeProperty.getURI())
        );

        return select;
    }
}
