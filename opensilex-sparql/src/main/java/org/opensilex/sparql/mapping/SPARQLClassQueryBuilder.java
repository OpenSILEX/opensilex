//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.io.StringWriter;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.utils.SHACL;

/**
 * @author vincent
 */
class SPARQLClassQueryBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLClassQueryBuilder.class);

    private final SPARQLClassAnalyzer analyzer;

    public SPARQLClassQueryBuilder(SPARQLClassAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public SelectBuilder getSelectBuilder(Node graph, String lang) {
        SelectBuilder selectBuilder = new SelectBuilder();
        addSelectUriTypeVars(selectBuilder);
        analyzer.forEachDataProperty((Field field, Property property) -> {
            selectBuilder.addVar(field.getName());
        });

        analyzer.forEachObjectProperty((Field field, Property property) -> {
            selectBuilder.addVar(field.getName());
        });

        analyzer.forEachLabelProperty((Field field, Property property) -> {
            selectBuilder.addVar(field.getName());
        });
        initializeQueryBuilder(selectBuilder, graph, lang);
        return selectBuilder;
    }

    public AskBuilder getAskBuilder(Node graph, String lang) {
        AskBuilder askBuilder = new AskBuilder();
        initializeQueryBuilder(askBuilder, graph, lang);
        return askBuilder;
    }

    public void initializeQueryBuilder(AbstractQueryBuilder<?> builder, Node graph, String lang) {
        String uriFieldName = analyzer.getURIFieldName();
        WhereHandler rootWhereHandler = new WhereHandler();

        addQueryBuilderModelWhereProperties(
                builder,
                rootWhereHandler,
                lang,
                (field, property) -> {
                    addSelectProperty(builder, uriFieldName, property, field, rootWhereHandler, null);
                },
                (field, property) -> {
                    addSelectProperty(builder, uriFieldName, property, field, rootWhereHandler, null);
                },
                (field, property) -> {
                    addSelectProperty(builder, uriFieldName, property, field, rootWhereHandler, lang);
                }
        );

        ExprFactory exprFactory = new ExprFactory();
        Expr noBlankNodeFilter = exprFactory.not(exprFactory.isBlank(makeVar(uriFieldName)));
        rootWhereHandler.addFilter(noBlankNodeFilter);

        // add the rootWhereHandler inside a GRAPH clause
        if (graph != null) {
            ElementNamedGraph elementNamedGraph = new ElementNamedGraph(graph, rootWhereHandler.getElement());
            builder.getWhereHandler().getClause().addElement(elementNamedGraph);
        } else {
            builder.getHandlerBlock().addAll(rootWhereHandler);
        }
    }

    public SelectBuilder getCountBuilder(Node graph, String countFieldName, String lang) {
        String uriFieldName = analyzer.getURIFieldName();

        SelectBuilder countBuilder = new SelectBuilder();

        try {
            // TODO generate properly count/distinct trought Jena API
            countBuilder.addVar("(COUNT(DISTINCT ?" + uriFieldName + "))", makeVar(countFieldName));
        } catch (ParseException ex) {
            LOGGER.error("Error while building count query (should never happend)", ex);
        }

        initializeQueryBuilder(countBuilder, graph, lang);

        return countBuilder;
    }

    private void addSelectUriTypeVars(SelectBuilder select) {
        select.addVar(analyzer.getURIFieldName());
        select.addVar(analyzer.getTypeFieldName());
        select.addVar(analyzer.getTypeLabelFieldName());
    }

    private void addQueryBuilderModelWhereProperties(
            AbstractQueryBuilder<?> builder,
            WhereHandler rootWhereHandler,
            String lang,
            BiConsumer<Field, Property> dataPropertyHandler,
            BiConsumer<Field, Property> objectPropertyHandler,
            BiConsumer<Field, Property> labelPropertyHandler
    ) {
        String uriFieldName = analyzer.getURIFieldName();
        String typeFieldName = analyzer.getTypeFieldName();
        Var typeFieldVar = makeVar(typeFieldName);

        WhereHandler whereHandler = builder.getWhereHandler();
        // WhereHandler used for adding all WHERE clause
        rootWhereHandler.addWhere(builder.makeTriplePath(makeVar(uriFieldName), RDF.type, typeFieldVar));
        whereHandler.addWhere(builder.makeTriplePath(typeFieldVar, Ontology.subClassAny, analyzer.getRDFType()));

        String typeLabelFieldName = analyzer.getTypeLabelFieldName();
        Var typeLabelFieldVar = makeVar(typeLabelFieldName);

        WhereHandler optionalTypeLabelHandler = new WhereHandler();
        optionalTypeLabelHandler.addWhere(builder.makeTriplePath(typeFieldVar, RDFS.label, typeLabelFieldVar));
        addLangFilter(typeLabelFieldName, lang, optionalTypeLabelHandler);
        whereHandler.addOptional(optionalTypeLabelHandler);

        analyzer.forEachDataProperty((Field field, Property property) -> {
            if (dataPropertyHandler != null) {
                dataPropertyHandler.accept(field, property);
            }
        });

        analyzer.forEachObjectProperty((Field field, Property property) -> {
            if (dataPropertyHandler != null) {
                objectPropertyHandler.accept(field, property);
            }
        });

        analyzer.forEachLabelProperty((Field field, Property property) -> {
            if (dataPropertyHandler != null) {
                labelPropertyHandler.accept(field, property);
            }
        });

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

    public <T extends SPARQLResourceModel> UpdateBuilder getDeleteBuilder(Node graph, T instance) throws Exception {
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

    public <T extends SPARQLResourceModel> void addDeleteBuilder(Node graph, T instance, UpdateBuilder delete) throws Exception {
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
    private void addSelectProperty(AbstractQueryBuilder<?> select, String uriFieldName, Property property, Field field,
            WhereHandler handler, String lang) {

        Var uriFieldVar = makeVar(uriFieldName);
        Var propertyFieldVar = makeVar(field.getName());

        boolean isOptional = analyzer.isOptional(field);
        boolean isReverseRelation = analyzer.isReverseRelation(field);

        TriplePath triple;
        if (isReverseRelation) {
            triple = select.makeTriplePath(propertyFieldVar, property, uriFieldVar);

        } else {
            triple = select.makeTriplePath(uriFieldVar, property, propertyFieldVar);
        }

        TriplePath rdtTypeTriple = null;
        if (property.equals(RDFS.subClassOf)) {
            rdtTypeTriple = select.makeTriplePath(propertyFieldVar, RDF.type, OWL.Class);
        }

        if (isOptional) {
            WhereHandler optionalHandler = new WhereHandler();
            optionalHandler.addWhere(triple);
            if (lang != null) {
                addLangFilter(field.getName(), lang, optionalHandler);
            }

            if (rdtTypeTriple != null) {
                optionalHandler.addWhere(rdtTypeTriple);
            }

            handler.addOptional(optionalHandler);
        } else {
            handler.addWhere(triple);
            if (lang != null) {
                addLangFilter(field.getName(), lang, handler);
            }
            if (rdtTypeTriple != null) {
                handler.addWhere(rdtTypeTriple);
            }

        }
    }

    private void addLangFilter(String fieldName, String lang,
            WhereHandler handler) {
        Locale locale = Locale.forLanguageTag(lang);
        handler.addFilter(SPARQLQueryHelper.langFilter(fieldName, locale.getLanguage()));
    }

    private <T extends SPARQLResourceModel> void executeOnInstanceTriples(T instance, BiConsumer<Triple, Field> tripleHandler, boolean ignoreNullFields) throws Exception {
        URI uri = analyzer.getURI(instance);
        Node uriNode = SPARQLDeserializers.nodeURI(uri);

        if (instance.getType() == null) {
            instance.setType(new URI(analyzer.getRDFType().getURI()));
        }

        tripleHandler.accept(new Triple(uriNode, RDF.type.asNode(), SPARQLDeserializers.nodeURI(instance.getType())), analyzer.getURIField());

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

        for (Field field : analyzer.getLabelPropertyFields()) {
            Object fieldValue = analyzer.getFieldValue(field, instance);
            if (fieldValue == null) {
                if (!ignoreNullFields && !analyzer.isOptional(field)) {
                    // TODO change exception type
                    throw new Exception("Field value can't be null: " + field.getName());
                }
            } else {
                SPARQLLabel label = (SPARQLLabel) fieldValue;
                Property property = analyzer.getLabelPropertyByField(field);
                for (Map.Entry<String, String> translation : label.getAllTranslations().entrySet()) {
                    Node translationNode = NodeFactory.createLiteral(translation.getValue(), translation.getKey());
                    tripleHandler.accept(new Triple(SPARQLDeserializers.nodeURI(uri), property.asNode(), translationNode), field);
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

    public String generateSHACL() {
        Model model = ModelFactory.createDefaultModel();

        Resource shape = model.createResource(analyzer.getRDFType() + "_ShapeSHACL");

        shape.addProperty(RDF.type, SHACL.NodeShape);
        shape.addProperty(SHACL.targetClass, analyzer.getRDFType());

        analyzer.forEachDataProperty((field, property) -> {
            Seq seq = model.createSeq();

            seq.addProperty(SHACL.path, property);
            XSDDatatype dataType = analyzer.getFieldDatatype(field);
            if (dataType.equals(XSDDatatype.XSDanyURI)) {
                seq.addProperty(SHACL.nodeKind, SHACL.IRI);
            } else {
                seq.addProperty(SHACL.datatype, model.createResource(dataType.getURI()));
            }
            seq.addProperty(SHACL.maxCount, "1", XSDDatatype.XSDinteger);

            if (analyzer.isOptional(field)) {
                seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
            } else {
                seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
            }

            shape.addProperty(SHACL.property, seq);
        });

        analyzer.forEachObjectProperty((field, property) -> {
            Seq seq = model.createSeq();
            seq.addProperty(SHACL.path, property);

            seq.addProperty(SHACL.classProperty, analyzer.getFieldRDFType(field));
            seq.addProperty(SHACL.maxCount, "1", XSDDatatype.XSDinteger);

            if (analyzer.isOptional(field)) {
                seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
            } else {
                seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
            }

            shape.addProperty(SHACL.property, seq);
        });

        analyzer.forEachLabelProperty((field, property) -> {

            Seq seq = model.createSeq();
            seq.addProperty(SHACL.path, property);

            seq.addProperty(SHACL.uniqueLang, "true", XSDDatatype.XSDboolean);

            if (analyzer.isOptional(field)) {
                seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
            } else {
                seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
            }

            shape.addProperty(SHACL.property, seq);
        });

        analyzer.forEachDataPropertyList((field, property) -> {
            Seq seq = model.createSeq();
            seq.addProperty(SHACL.path, property);

            XSDDatatype dataType = analyzer.getFieldListDatatype(field);
            if (dataType.equals(XSDDatatype.XSDanyURI)) {
                seq.addProperty(SHACL.nodeKind, SHACL.IRI);
            } else {
                seq.addProperty(SHACL.datatype, model.createResource(dataType.getURI()));
            }

            if (analyzer.isOptional(field)) {
                seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
            } else {
                seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
            }

            shape.addProperty(SHACL.property, seq);
        });

        analyzer.forEachObjectPropertyList((field, property) -> {
            Seq seq = model.createSeq();
            seq.addProperty(SHACL.path, property);

            seq.addProperty(SHACL.classProperty, analyzer.getFieldListRDFType(field));

            if (analyzer.isOptional(field)) {
                seq.addProperty(SHACL.minCount, "0", XSDDatatype.XSDinteger);
            } else {
                seq.addProperty(SHACL.minCount, "1", XSDDatatype.XSDinteger);
            }

            shape.addProperty(SHACL.property, seq);
        });

        StringWriter str = new StringWriter();
        model.write(str, "TURTLE");

        return str.toString();
    }

}
