/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.logs.dal;

import org.opensilex.nosql.service.NoSQLService;

/**
 *
 * @author charlero
 */
public class LogsDAO {

    protected final NoSQLService nosql;

    public LogsDAO(NoSQLService nosql) {
        this.nosql = nosql;
    }

    public void create(LogModel instance) throws Exception {
        nosql.create(instance);
    }

}
