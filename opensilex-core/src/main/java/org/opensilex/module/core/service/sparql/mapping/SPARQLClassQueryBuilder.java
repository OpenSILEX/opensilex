/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql.mapping;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.utils.deserializer.Deserializers;
import org.opensilex.utils.ontology.Ontology;

/**
 *
 * @author vincent
 */
public class SPARQLClassQueryBuilder {

    private SelectBuilder selectBuilder;
    private SelectBuilder countBuilder;

    private final SPARQLClassAnalyzer analyzer;

    public SPARQLClassQueryBuilder(SPARQLClassAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
    
    public SelectBuilder getSelectBuilder() throws SPARQLInvalidClassDefinitionException {
        if (selectBuilder == null) {
            selectBuilder = new SelectBuilder();

            String uriFieldName = analyzer.getURIFieldName();
            selectBuilder.addVar(uriFieldName);
            selectBuilder.addWhere(makeVar(uriFieldName), RDF.type, analyzer.getRDFType());

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

    public SelectBuilder getCountBuilder(String countFieldName) {
        if (countBuilder == null) {
            countBuilder = new SelectBuilder();

            String uriFieldName = analyzer.getURIFieldName();
            countBuilder.addVar("count(distinct ?" + uriFieldName + ") as " + countFieldName);
            countBuilder.addWhere(uriFieldName, RDF.type, analyzer.getRDFType());

            analyzer.forEachDataProperty((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field);
            });

            analyzer.forEachObjectProperty((Field field, Property property) -> {
                addSelectProperty(countBuilder, uriFieldName, property, field);
            });

            countBuilder.addGroupBy(uriFieldName);
        }

        return countBuilder.clone();
    }

    public UpdateBuilder getCreateBuilder(Object instance) throws Exception {
        UpdateBuilder create = new UpdateBuilder();
        addCreateBuilder(instance, create);

        return create;
    }

    public void addCreateBuilder(Object instance, UpdateBuilder create) throws Exception {
        // TODO generate URI if it's null
        executeOnInstanceTriples(instance, (Triple triple, Boolean isReverse) -> {
            if (isReverse) {
                create.addInsert(triple.getObject(), triple.getPredicate(), triple.getSubject());
            } else {
                create.addInsert(triple);
            }
        });
    }

    public UpdateBuilder getDeleteBuilder(Object instance) throws Exception {
        UpdateBuilder delete = new UpdateBuilder();
        addDeleteBuilder(instance, delete);

        return delete;
    }

    public void addUpdateBuilder(Object oldInstance, Object newInstance, UpdateBuilder update) throws Exception {
        final AtomicInteger i = new AtomicInteger(0);
        executeOnInstanceTriples(oldInstance, (Triple triple, Boolean isReverse) -> {
            String var = "?x" + i.addAndGet(1);
            if (isReverse) {
                update.addDelete(var, triple.getPredicate(), triple.getSubject());
                update.addWhere(var, triple.getPredicate(), triple.getSubject());
            } else {
                update.addDelete(triple.getSubject(), triple.getPredicate(), var);
                update.addWhere(triple.getSubject(), triple.getPredicate(), var);
            }
            
        });
        addCreateBuilder(newInstance, update);
    }

    public void addDeleteBuilder(Object instance, UpdateBuilder delete) throws Exception {
        executeOnInstanceTriples(instance, (Triple triple, Boolean isReverse) -> {
            if (isReverse) {
                delete.addDelete(triple.getObject(), triple.getPredicate(), triple.getSubject());
            } else {
                delete.addDelete(triple);
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
    
    private void executeOnInstanceTriples(Object instance, BiConsumer<Triple, Boolean> tripleHandler) throws Exception {
        URI uri = analyzer.getURI(instance);

        tripleHandler.accept(new Triple(Ontology.nodeURI(uri), RDF.type.asNode(), analyzer.getRDFType().asNode()), false);

        for (Field field : analyzer.getDataPropertyFields()) {
            Object fieldValue = field.get(instance);

            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null");
                }
            } else {
                Property property = analyzer.getDataPropertyByField(field);
                Node fieldNodeValue = Deserializers.getForClass(fieldValue.getClass()).getNode(fieldValue);
                tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), fieldNodeValue), analyzer.isReverseRelation(field));
            }
        }

        for (Field field : analyzer.getObjectPropertyFields()) {
            Object fieldValue = field.get(instance);

            if (fieldValue == null) {
                if (!analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null");
                }
            } else {
                URI propertyFieldURI = SPARQLClassObjectMapper.getForClass(fieldValue.getClass()).getURI(fieldValue);
                if (propertyFieldURI != null) {
                    Property property = analyzer.getObjectPropertyByField(field);
                    tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), Ontology.nodeURI(propertyFieldURI)), analyzer.isReverseRelation(field));
                } else {
                    // TODO change exception type
                    throw new Exception("Object URI value can't be null");
                }

            }
        }

        for (Field field : analyzer.getDataListPropertyFields()) {
            List<?> fieldValues = (List<?>) field.get(instance);

            if (fieldValues != null) {
                Property property = analyzer.getDataListPropertyByField(field);

                for (Object listValue : fieldValues) {
                    Node listNodeValue = Deserializers.getForClass(listValue.getClass()).getNode(listValue);
                    tripleHandler.accept(new Triple(Ontology.nodeURI(uri), property.asNode(), listNodeValue), analyzer.isReverseRelation(field));
                }
            }
        }

        for (Field field : analyzer.getObjectListPropertyFields()) {
            List<?> fieldValues = (List<?>) field.get(instance);

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
