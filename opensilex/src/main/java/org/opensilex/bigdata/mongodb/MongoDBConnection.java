/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.bigdata.mongodb;

import java.util.HashMap;
import org.opensilex.bigdata.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.ServiceConnection;

/**
 *
 * @author Vincent Migot
 */
public class MongoDBConnection extends AbstractDataNucleusConnection implements ServiceConnection{

    public MongoDBConnection(MongoDBConfig config) {
        super(new HashMap<>());
    }

    @Override
    public void startup() {
// TODO        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutdown() {
// TODO       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
