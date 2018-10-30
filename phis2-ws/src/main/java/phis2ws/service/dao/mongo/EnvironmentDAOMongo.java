//******************************************************************************
//                                       EnvironmentDAOMongo.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.view.model.phis.Environment;

/**
 * Represents the MongoDB Data Access Object for the environment.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class EnvironmentDAOMongo extends DAOMongo<Environment> {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(EnvironmentDAOMongo.class);

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<Environment> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
