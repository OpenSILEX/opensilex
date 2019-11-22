//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql;

import java.net.*;
import java.util.*;
import java.util.function.*;
import org.apache.jena.arq.querybuilder.*;
import org.opensilex.service.*;
import org.opensilex.sparql.exceptions.*;



/**
 *
 * @author Vincent Migot
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

    public void clearGraph(URI graph) throws SPARQLQueryException;

    public void clear() throws SPARQLQueryException;

    public void startTransaction() throws SPARQLTransactionException;

    public void commitTransaction() throws SPARQLTransactionException;

    public void rollbackTransaction() throws SPARQLTransactionException;

}
