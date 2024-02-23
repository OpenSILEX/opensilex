//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.opensilex.service.Service;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author Vincent Migot
 */
public interface SPARQLConnection extends Service {

    boolean executeAskQuery(AskBuilder ask) throws SPARQLException;

    List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLException;

    List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLException;

    List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLException;

    default List<SPARQLResult> executeSelectQuery(SelectBuilder select) throws SPARQLException {
        return executeSelectQuery(select, null);
    }

    default Stream<SPARQLResult> executeSelectQueryAsStream(SelectBuilder select) throws SPARQLException{
        return executeSelectQuery(select).stream();
    }

    void executeUpdateQuery(UpdateBuilder update) throws SPARQLException;

    void executeUpdateQuery(String update) throws SPARQLException;

    void executeDeleteQuery(UpdateBuilder update) throws SPARQLException;

    List<SPARQLStatement> getGraphStatement(URI graph) throws SPARQLException;

    void clearGraph(URI graph) throws SPARQLException;

    void renameGraph(URI oldGraphURI, URI newGraphURI) throws SPARQLException;

    void clear() throws SPARQLException;

    boolean hasActiveTransaction();

    void startTransaction() throws SPARQLException;

    void commitTransaction() throws SPARQLException;

    default void rollbackTransaction(Exception ex) throws Exception {
        if (ex != null) {
            throw ex;
        }
    }

    void disableSHACL() throws SPARQLException;

    void enableSHACL() throws SPARQLException;
    
    boolean isShaclEnabled();

    default void loadOntology(URI graph, InputStream ontology, Lang format) throws SPARQLException {
        Node graphNode = graph != null
                ? NodeFactory.createURI(graph.toString())
                : null;
        Model model = ModelFactory.createDefaultModel();
        model.read(ontology, null, format.getName());

        UpdateBuilder insertQuery = new UpdateBuilder();
        StmtIterator iterator = model.listStatements();

        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            if (graphNode != null) {
                insertQuery.addInsert(graphNode, statement.asTriple());
            } else {
                insertQuery.addInsert(statement.asTriple());
            }
        }
        executeUpdateQuery(insertQuery);
    }

    default void loadOntology(URI graph, String ontology, Lang format) throws SPARQLException {
        ByteArrayInputStream ontologyStream = new ByteArrayInputStream(ontology.getBytes());
        loadOntology(graph, ontologyStream, format);
    }

    SPARQLClassObjectMapperIndex getMapperIndex();

    void setMapperIndex(SPARQLClassObjectMapperIndex mapperIndex);

}
