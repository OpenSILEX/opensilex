//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata.mongodb;

import java.util.HashMap;
import org.opensilex.bigdata.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.ServiceConnection;

public class MongoDBConnection extends AbstractDataNucleusConnection implements ServiceConnection {

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
