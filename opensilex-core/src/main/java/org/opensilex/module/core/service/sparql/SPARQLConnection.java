/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql;

import java.util.List;
import java.util.function.Consumer;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLTransactionException;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLQueryException;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.opensilex.service.ServiceConnection;

/**
 *
 * @author vincent
 */
public interface SPARQLConnection extends ServiceConnection {

    public boolean executeAskQuery(AskBuilder ask) throws SPARQLQueryException;

    public List<SPARQLResult> executeDescribeQuery(DescribeBuilder describe) throws SPARQLQueryException;

    public List<SPARQLResult> executeConstructQuery(ConstructBuilder construct) throws SPARQLQueryException;

    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLQueryException;

    public default List<SPARQLResult> executeSelectQuery(SelectBuilder select) throws SPARQLQueryException {
        return executeSelectQuery(select, null);
    }

    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLQueryException;

    public void executeDeleteQuery(UpdateBuilder update) throws SPARQLQueryException;

    public void clearGraph(Node graph) throws SPARQLQueryException;

    public void clear() throws SPARQLQueryException;

    public void startTransaction() throws SPARQLTransactionException;

    public void commitTransaction() throws SPARQLTransactionException;

    public void rollbackTransaction() throws SPARQLTransactionException;

}
