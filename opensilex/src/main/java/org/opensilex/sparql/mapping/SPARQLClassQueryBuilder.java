//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;
import static org.opensilex.sparql.SPARQLQueryHelper.typeDefVar;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
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

    public SelectBuilder getSelectBuilder(Node graph) {
        if (selectBuilder == null) {
            selectBuilder = new SelectBuilder();

            if (graph != null) {
                selectBuilder.from(graph.toString());
            }

            String uriFieldName = analyzer.getURIFieldName();
            selectBuilder.addVar(uriFieldName);
            selectBuilder.addVar(typeDefVar);
            selectBuilder.addWhere(makeVar(uriFieldName), RDF.type, typeDefVar);
            selectBuilder.addWhere(typeDefVar, Ontology.subClassAny, analyzer.getRDFType());

            analyzer.forEachDataProperty((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field);
            });

            analyzer.forEachObjectProperty((Field field, Property property) -> {
                selectBuilder.addVar(field.getName());
                addSelectProperty(selectBuilder, uriFieldName, property, field);
            });
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

            if (graph != null) {
                countBuilder.from(graph.toString());
            }

            String uriFieldName = analyzer.getURIFieldName();
            try {
                // TODO generate properly count/distinct trought Jena API 
                countBuilder.addVar("(COUNT(DISTINCT ?" + uriFieldName + "))", makeVar(countFieldName));
            } catch (ParseException ex) {
                LOGGER.error("Error while building count query (should never happend)", ex);
                
            }
            countBuilder.addWhere(makeVar(uriFieldName), RDF.type, typeDefVar);
            countBuilder.addWhere(typeDefVar, Ontology.subClassAny, analyzer.getRDFType());

            analyzer.forEachDataProperty((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field);
            });

            analyzer.forEachObjectProperty((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field);
            });
        }

        return countBuilder.clone();
    }

    public <T extends SPARQLResourceModel> UpdateBuilder getCreateBuilder(Node graph, T instance) throws Exception {
        UpdateBuilder create = new UpdateBuilder();
        addCreateBuilder(graph, instance, create);

        return create;
    }

    public <T extends SPARQLResourceModel> void addCreateBuilder(Node graph, T instance, UpdateBuilder create) throws Exception {
        executeOnInstanceTriples(instance, (Triple triple, Boolean isReverse) -> {
            addCreateBuilderHelper(graph, triple, isReverse, create);
        });

        URI uri = instance.getUri();

        for (SPARQLModelRelation relation : instance.getRelations()) {

            Class<?> valueType = relation.getType();
            Node valueNode = SPARQLDeserializers.getForClass(valueType).getNodeFromString(relation.getValue());

            Triple triple = new Triple(Ontology.nodeURI(uri), relation.getProperty().asNode(), valueNode);

            Node relationGraph = graph;
            if (relation.getGraph() != null) {
                relationGraph = Ontology.nodeURI(relation.getGraph());
            }
            addCreateBuilderHelper(relationGraph, triple, relation.getReverse(), create);
        }
    }

    private void addCreateBuilderHelper(Node graph, Triple triple, Boolean isReverse, UpdateBuilder create) {
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
        executeOnInstanceTriples(oldInstance, (Triple triple, Boolean isReverse) -> {
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
        });
        addCreateBuilder(graph, newInstance, update);
    }

    public void addDeleteBuilder(Node graph, Object instance, UpdateBuilder delete) throws Exception {
        executeOnInstanceTriples(instance, (Triple triple, Boolean isReverse) -> {
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
        });
    }

    private void addSelectProperty(SelectBuilder select, String uriFieldName, Property property, Field field) {
        Var uriFieldVar = makeVar(uriFieldName);
        Var propertyFieldVar = makeVar(field.getName());

        if (analyzer.isReverseRelation(field)) {
            if (analyzer.isOptional(field)) {
                select.addOptional(propertyFieldVar, property, uriFieldVar);
            } else {
                select.addWhere(propertyFieldVar, property, uriFieldVar);
            }
        } else {
            if (analyzer.isOptional(field)) {
                select.addOptional(uriFieldVar, property, propertyFieldVar);
            } else {
                select.addWhere(uriFieldVar, property, propertyFieldVar);
            }
        }
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

    private void executeOnInstanceTriples(Object instance, BiConsumer<Triple, Boolean> tripleHandler) throws Exception {
        URI uri = analyzer.getURI(instance);

        tripleHandler.accept(new Triple(Ontology.nodeURI(uri), RDF.type.asNode(), analyzer.getRDFType().asNode()), false);

        for (Field field : analyzer.getDataPropertyFields()) {
            Object fieldValue = analyzer.getGetterFromField(field).invoke(instance);

            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                Property property = analyzer.getDataPropertyByField(field);
                Node fieldNodeValue = SPARQLDeserializers.getForClass(fieldValue.getClass()).getNode(fieldValue);
                tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), fieldNodeValue), analyzer.isReverseRelation(field));
            }
        }

        for (Field field : analyzer.getObjectPropertyFields()) {
            Object fieldValue = analyzer.getGetterFromField(field).invoke(instance);

            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                URI propertyFieldURI = SPARQLClassObjectMapper.getForClass(fieldValue.getClass()).getURI(fieldValue);
                if (propertyFieldURI != null) {
                    Property property = analyzer.getObjectPropertyByField(field);
                    tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), Ontology.nodeURI(propertyFieldURI)), analyzer.isReverseRelation(field));
                } else {
                    // TODO change exception type
                    throw new Exception("Object URI value can't be null: " + field.getName());
                }
            }
        }

        for (Field field : analyzer.getDataListPropertyFields()) {
            List<?> fieldValues = (List<?>) analyzer.getGetterFromField(field).invoke(instance);

            if (fieldValues != null) {
                Property property = analyzer.getDataListPropertyByField(field);

                for (Object listValue : fieldValues) {
                    Node listNodeValue = SPARQLDeserializers.getForClass(listValue.getClass()).getNode(listValue);
                    tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), listNodeValue), analyzer.isReverseRelation(field));
                }
            }
        }

        for (Field field : analyzer.getObjectListPropertyFields()) {
            List<?> fieldValues = (List<?>) analyzer.getGetterFromField(field).invoke(instance);

            if (fieldValues != null) {
                for (Object listValue : fieldValues) {
                    if (listValue == null) {
                        if (!analyzer.isOptional(field)) {
                            // TODO change exception type
                            throw new Exception("Field value can't be null");
                        }
                    } else {
                        URI propertyFieldURI = SPARQLClassObjectMapper.getForClass(listValue.getClass()).getURI(listValue);
                        if (propertyFieldURI != null) {
                            Property property = analyzer.getObjectListPropertyByField(field);
                            tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), Ontology.nodeURI(propertyFieldURI)), analyzer.isReverseRelation(field));
                        } else {
                            // TODO change exception type
                            throw new Exception("Object URI value can't be null");
                        }

                    }
                }
            }
        }
    }
}
