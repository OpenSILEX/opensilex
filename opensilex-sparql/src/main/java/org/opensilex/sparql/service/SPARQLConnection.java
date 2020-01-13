//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.service;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.DescribeBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.opensilex.service.ServiceConnection;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.exceptions.SPARQLTransactionException;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLStatement;



/**
 *
 * @author Vincent Migot
 */
public interface SPARQLConnection extends ServiceConnection {

    public boolean executeAskQuery(AskBuilder ask) throws SPARQLQueryException;

    public List<SPARQLStatement> executeDescribeQuery(DescribeBuilder describe) throws SPARQLQueryException;

    public List<SPARQLStatement> executeConstructQuery(ConstructBuilder construct) throws SPARQLQueryException;

    public List<SPARQLResult> executeSelectQuery(SelectBuilder select, Consumer<SPARQLResult> resultHandler) throws SPARQLQueryException;

    public default List<SPARQLResult> executeSelectQuery(SelectBuilder select) throws SPARQLQueryException {
        return executeSelectQuery(select, null);
    }

    public void executeUpdateQuery(UpdateBuilder update) throws SPARQLQueryException;

    public void executeDeleteQuery(UpdateBuilder update) throws SPARQLQueryException;

    public void clearGraph(URI graph) throws SPARQLQueryException;

    public void clear() throws SPARQLQueryException;

    public void startTransaction() throws SPARQLTransactionException;

    public void commitTransaction() throws SPARQLTransactionException;

    public void rollbackTransaction() throws SPARQLTransactionException;

}
