//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.LocaleUtils;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.typeDefVar;

/**
 * @author vincent
 */
public class SPARQLClassQueryBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassQueryBuilder.class);

    private SelectBuilder selectBuilder;
    private AskBuilder askBuilder;
    private SelectBuilder countBuilder;

    private final SPARQLClassAnalyzer analyzer;

    public SPARQLClassQueryBuilder(SPARQLClassAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public SelectBuilder getSelectBuilder(Node graph, String lang) {
        if (selectBuilder == null) {
            selectBuilder = new SelectBuilder();

            String uriFieldName = analyzer.getURIFieldName();
            selectBuilder.addVar(uriFieldName);
            selectBuilder.addVar(typeDefVar);

            // WhereHandler used for adding all WHERE clause
            WhereHandler rootWhereHandler = new WhereHandler();
            rootWhereHandler.addWhere(selectBuilder.makeTriplePath(makeVar(uriFieldName), RDF.type, typeDefVar));
            rootWhereHandler.addWhere(selectBuilder.makeTriplePath(typeDefVar, Ontology.subClassAny, analyzer.getRDFType()));

            analyzer.forEachDataProperty((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field, rootWhereHandler);
            });

            analyzer.forEachObjectProperty((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field, rootWhereHandler);
            });

            analyzer.forEachLabelProperty((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field, rootWhereHandler);
                if (lang != null) {
                    addSelectLangFilter(selectBuilder, field.getName(), lang);
                }
            });

            // add the rootWhereHandler inside a GRAPH clause
            if (graph != null) {
                ElementNamedGraph elementNamedGraph = new ElementNamedGraph(graph, rootWhereHandler.getElement());
                selectBuilder.getWhereHandler().getClause().addElement(elementNamedGraph);
            } else {
                selectBuilder.getHandlerBlock().addAll(rootWhereHandler);
            }
        }

        return selectBuilder.clone();
    }

    public AskBuilder getAskBuilder(Node graph) {
        if (askBuilder == null) {
            askBuilder = new AskBuilder();

            if (graph != null) {
                askBuilder.from(graph.toString());
            }

            String uriFieldName = analyzer.getURIFieldName();
            askBuilder.addWhere(makeVar(uriFieldName), RDF.type, typeDefVar);
            askBuilder.addWhere(typeDefVar, Ontology.subClassAny, analyzer.getRDFType());
            analyzer.forEachDataProperty((Field field, Property property) -> {
                addAskProperty(askBuilder, uriFieldName, property, field);
            });

            analyzer.forEachObjectProperty((Field field, Property property) -> {
                addAskProperty(askBuilder, uriFieldName, property, field);
            });
        }

        return askBuilder.clone();
    }

    public SelectBuilder getCountBuilder(Node graph, String countFieldName) {
        if (countBuilder == null) {
            countBuilder = new SelectBuilder();

            String uriFieldName = analyzer.getURIFieldName();
            try {
                // TODO generate properly count/distinct trought Jena API
                countBuilder.addVar("(COUNT(DISTINCT ?" + uriFieldName + "))", makeVar(countFieldName));
            } catch (ParseException ex) {
                LOGGER.error("Error while building count query (should never happend)", ex);
            }
            WhereHandler rootWhereHandler = new WhereHandler();
            rootWhereHandler.addWhere(countBuilder.makeTriplePath(makeVar(uriFieldName), RDF.type, typeDefVar));
            rootWhereHandler.addWhere(countBuilder.makeTriplePath(typeDefVar, Ontology.subClassAny, analyzer.getRDFType()));

            analyzer.forEachDataProperty((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field, rootWhereHandler);
            });

            analyzer.forEachObjectProperty((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field, rootWhereHandler);
            });

            // add the rootWhereHandler inside a GRAPH clause
            if (graph != null) {
                ElementNamedGraph elementNamedGraph = new ElementNamedGraph(graph, rootWhereHandler.getElement());
                countBuilder.getWhereHandler().getClause().addElement(elementNamedGraph);
            } else {
                countBuilder.getHandlerBlock().addAll(rootWhereHandler);
            }
        }

        return countBuilder.clone();
    }

    public <T extends SPARQLResourceModel> UpdateBuilder getCreateBuilder(Node graph, T instance) throws Exception {
        UpdateBuilder create = new UpdateBuilder();
        addCreateBuilder(graph, instance, create);

        return create;
    }

    public <T extends SPARQLResourceModel> void addCreateBuilder(Node graph, T instance, UpdateBuilder create) throws Exception {
        executeOnInstanceTriples(instance, (Triple triple, Field field) -> {
            addCreateBuilderHelper(graph, triple, field, create);
        }, false);

        URI uri = instance.getUri();

        for (SPARQLModelRelation relation : instance.getRelations()) {

            Class<?> valueType = relation.getType();
            Node valueNode = SPARQLDeserializers.getForClass(valueType).getNodeFromString(relation.getValue());

            Triple triple = new Triple(SPARQLDeserializers.nodeURI(uri), relation.getProperty().asNode(), valueNode);

            Node relationGraph = graph;
            if (relation.getGraph() != null) {
                relationGraph = SPARQLDeserializers.nodeURI(relation.getGraph());
            }
            addCreateBuilderHelper(relationGraph, triple, null, create);
        }
    }

    private void addCreateBuilderHelper(Node graph, Triple triple, Field field, UpdateBuilder create) {
        boolean isReverse = false;
        if (field != null) {
            isReverse = analyzer.isReverseRelation(field);
        }

        if (graph != null) {
            if (isReverse) {
                create.addInsert(graph, triple.getObject(), triple.getPredicate(), triple.getSubject());
            } else {
                create.addInsert(graph, triple);
            }
        } else {
            if (isReverse) {
                create.addInsert(triple.getObject(), triple.getPredicate(), triple.getSubject());
            } else {
                create.addInsert(triple);
            }
        }
    }

    public UpdateBuilder getDeleteBuilder(Node graph, Object instance) throws Exception {
        UpdateBuilder delete = new UpdateBuilder();
        addDeleteBuilder(graph, instance, delete);

        return delete;
    }

    public <T extends SPARQLResourceModel> void addUpdateBuilder(Node graph, T oldInstance, T newInstance, UpdateBuilder update) throws Exception {
        final AtomicInteger i = new AtomicInteger(0);
        executeOnInstanceTriples(oldInstance, (Triple triple, Field field) -> {
            boolean isReverse = false;
            boolean ignoreUpdateIfNull = false;
            if (field != null) {
                isReverse = analyzer.isReverseRelation(field);
                try {
                    Object newFieldValue = analyzer.getFieldValue(field, newInstance);
                    ignoreUpdateIfNull = newFieldValue == null && analyzer.getFieldAnnotation(field).ignoreUpdateIfNull();
                } catch (Exception ex) {
                    LOGGER.warn("Unexpected error (should never happend) while reading field: " + field.getName(), ex);
                }

            }

            if (!ignoreUpdateIfNull) {
                String var = "?x" + i.addAndGet(1);

                if (graph != null) {
                    if (isReverse) {
                        update.addDelete(graph, var, triple.getPredicate(), triple.getSubject());
                        update.addWhere(var, triple.getPredicate(), triple.getSubject());
                    } else {
                        update.addDelete(graph, triple.getSubject(), triple.getPredicate(), var);
                        update.addWhere(triple.getSubject(), triple.getPredicate(), var);
                    }
                } else {
                    if (isReverse) {
                        update.addDelete(var, triple.getPredicate(), triple.getSubject());
                        update.addWhere(var, triple.getPredicate(), triple.getSubject());
                    } else {
                        update.addDelete(triple.getSubject(), triple.getPredicate(), var);
                        update.addWhere(triple.getSubject(), triple.getPredicate(), var);
                    }
                }
            }
        }, true);

        executeOnInstanceTriples(newInstance, (Triple triple, Field field) -> {
            addCreateBuilderHelper(graph, triple, field, update);
        }, true);
    }

    public void addDeleteBuilder(Node graph, Object instance, UpdateBuilder delete) throws Exception {
        executeOnInstanceTriples(instance, (Triple triple, Field field) -> {
            boolean isReverse = false;
            if (field != null) {
                isReverse = analyzer.isReverseRelation(field);
            }

            if (graph != null) {
                if (isReverse) {
                    delete.addDelete(graph, triple.getObject(), triple.getPredicate(), triple.getSubject());
                } else {
                    delete.addDelete(graph, triple);
                }
            } else {
                if (isReverse) {
                    delete.addDelete(triple.getObject(), triple.getPredicate(), triple.getSubject());
                } else {
                    delete.addDelete(triple);
                }
            }
        }, false);
    }

    /**
     * Add the WHERE clause into handler, depending if the given field is
     * optional or not, according {@link #analyzer}
     *
     * @param select the root {@link SelectBuilder}, needed in order to create
     * the {@link TriplePath} to add to the handler
     * @param uriFieldName name of the uri SPARQL variable
     * @param property the {@link Property} to add
     * @param field the property corresponding {@link Field}
     * @param handler the {@link WhereHandler} in which the where clause is
     * added when the field is required
     * @see SPARQLClassAnalyzer#isOptional(Field)
     * @see SelectBuilder#makeTriplePath(Object, Object, Object)
     */
    private void addSelectProperty(SelectBuilder select, String uriFieldName, Property property, Field field,
            WhereHandler handler) {

        Var uriFieldVar = makeVar(uriFieldName);
        Var propertyFieldVar = makeVar(field.getName());

        if (analyzer.isReverseRelation(field)) {
            if (analyzer.isOptional(field)) {
                handler.addOptional(select.makeTriplePath(propertyFieldVar, property, uriFieldVar));
            } else {
                handler.addWhere(select.makeTriplePath(propertyFieldVar, property, uriFieldVar));
            }
        } else {
            if (analyzer.isOptional(field)) {
                handler.addOptional(select.makeTriplePath(uriFieldVar, property, propertyFieldVar));
            } else {
                handler.addWhere(select.makeTriplePath(uriFieldVar, property, propertyFieldVar));
            }
        }
    }

    private void addSelectLangFilter(SelectBuilder select, String fieldName, String lang) {
        Locale locale = Locale.forLanguageTag(lang);
        select.addFilter(SPARQLQueryHelper.langFilter(fieldName, locale.getLanguage()));
    }

    private void addAskProperty(AskBuilder ask, String uriFieldName, Property property, Field field) {
        Var uriFieldVar = makeVar(uriFieldName);
        Var propertyFieldVar = makeVar(field.getName());

        if (analyzer.isReverseRelation(field)) {
            if (!analyzer.isOptional(field)) {
                ask.addWhere(propertyFieldVar, property, uriFieldVar);
            }
        } else {
            if (!analyzer.isOptional(field)) {
                ask.addWhere(uriFieldVar, property, propertyFieldVar);
            }
        }
    }

    private void executeOnInstanceTriples(Object instance, BiConsumer<Triple, Field> tripleHandler, boolean ignoreNullFields) throws Exception {
        URI uri = analyzer.getURI(instance);
        Node uriNode = SPARQLDeserializers.nodeURI(uri);

        tripleHandler.accept(new Triple(uriNode, RDF.type.asNode(), analyzer.getRDFType().asNode()), analyzer.getURIField());

        for (Field field : analyzer.getDataPropertyFields()) {
            Object fieldValue = analyzer.getFieldValue(field, instance);

            if (fieldValue == null) {
                if (!ignoreNullFields && !analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                Property property = analyzer.getDataPropertyByField(field);
                Node fieldNodeValue = SPARQLDeserializers.getForClass(fieldValue.getClass()).getNode(fieldValue);
                tripleHandler.accept(new Triple(SPARQLDeserializers.nodeURI(uri), property.asNode(), fieldNodeValue), field);
            }
        }

        for (Field field : analyzer.getObjectPropertyFields()) {
            Object fieldValue = analyzer.getFieldValue(field, instance);

            if (fieldValue == null) {
                if (!ignoreNullFields && !analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                URI propertyFieldURI = SPARQLClassObjectMapper.getForClass(fieldValue.getClass()).getURI(fieldValue);
                if (!ignoreNullFields && propertyFieldURI == null) {
                    // TODO change exception type
                    throw new Exception("Object URI value can't be null: " + field.getName());
                } else {
                    Property property = analyzer.getObjectPropertyByField(field);
                    tripleHandler.accept(new Triple(SPARQLDeserializers.nodeURI(uri), property.asNode(), SPARQLDeserializers.nodeURI(propertyFieldURI)), field);
                }
            }
        }

        for (Field field : analyzer.getDataListPropertyFields()) {
            List<?> fieldValues = (List<?>) analyzer.getFieldValue(field, instance);

            if (fieldValues != null) {
                Property property = analyzer.getDataListPropertyByField(field);

                for (Object listValue : fieldValues) {
                    Node listNodeValue = SPARQLDeserializers.getForClass(listValue.getClass()).getNode(listValue);
                    tripleHandler.accept(new Triple(SPARQLDeserializers.nodeURI(uri), property.asNode(), listNodeValue), field);
                }
            }
        }

        for (Field field : analyzer.getObjectListPropertyFields()) {
            List<?> fieldValues = (List<?>) analyzer.getFieldValue(field, instance);

            if (fieldValues != null) {
                for (Object listValue : fieldValues) {
                    if (listValue == null) {
                        if (!ignoreNullFields && !analyzer.isOptional(field)) {
                            // TODO change exception type
                            throw new Exception("Field value can't be null");
                        }
                    } else {
                        URI propertyFieldURI = SPARQLClassObjectMapper.getForClass(listValue.getClass()).getURI(listValue);
                        if (!ignoreNullFields && propertyFieldURI == null) {
                            // TODO change exception type
                            throw new Exception("Object URI value can't be null");
                        } else {
                            Property property = analyzer.getObjectListPropertyByField(field);
                            tripleHandler.accept(new Triple(SPARQLDeserializers.nodeURI(uri), property.asNode(), SPARQLDeserializers.nodeURI(propertyFieldURI)), field);
                        }

                    }
                }
            }
        }
    }
}
