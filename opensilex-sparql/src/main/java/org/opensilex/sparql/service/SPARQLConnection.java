//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
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

/**
 *
 * @author Vincent Migot
 */
public interface SPARQLConnection extends Service {

    public boolean executeAskQuery(AskBuilder ask) throws SPARQLException;

    public List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLException;

    public List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLException;

    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLException;

    public default List<SPARQLResult> executeSelectQuery(SelectBuilder select) throws SPARQLException {
        return executeSelectQuery(select, null);
    }

    default Stream<SPARQLResult> executeSelectQueryAsStream(SelectBuilder select) throws SPARQLException{
        return executeSelectQuery(select).stream();
    }

    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLException;

    void executeUpdateQuery(String update) throws SPARQLException;

    public void executeDeleteQuery(UpdateBuilder update) throws SPARQLException;

    public List<SPARQLStatement> getGraphStatement(URI graph) throws SPARQLException;

    public void clearGraph(URI graph) throws SPARQLException;

    void renameGraph(URI oldGraphURI, URI newGraphURI) throws SPARQLException;

    public void clear() throws SPARQLException;

    public void startTransaction() throws SPARQLException;

    public void commitTransaction() throws SPARQLException;

    public default void rollbackTransaction(Exception ex) throws Exception {
        if (ex != null) {
            throw ex;
        }
    }

    public void disableSHACL() throws SPARQLException;

    public void enableSHACL() throws SPARQLException;
    
    public boolean isShaclEnabled();

    public default void loadOntology(URI graph, InputStream ontology, Lang format) throws SPARQLException {
        Node graphNode = NodeFactory.createURI(graph.toString());
        Model model = ModelFactory.createDefaultModel();
        model.read(ontology, null, format.getName());

        UpdateBuilder insertQuery = new UpdateBuilder();
        StmtIterator iterator = model.listStatements();

        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            insertQuery.addInsert(graphNode, statement.asTriple());
        }
        executeUpdateQuery(insertQuery);
    }

    public default void loadOntology(URI graph, String ontology, Lang format) throws SPARQLException {
        ByteArrayInputStream ontologyStream = new ByteArrayInputStream(ontology.getBytes());
        loadOntology(graph, ontologyStream, format);
    }

    public SPARQLClassObjectMapperIndex getMapperIndex();

    public void setMapperIndex(SPARQLClassObjectMapperIndex mapperIndex);

}
