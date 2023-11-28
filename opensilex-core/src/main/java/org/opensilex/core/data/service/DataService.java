/*
 *  *************************************************************************************
 *  DataService.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  user@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.core.data.service;

import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataSearchFilter;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.opensilex.core.data.dal.DataModel.VARIABLE_FIELD;

public class DataService {

    private final DataDAO dataDAO;
    private final VariableDAO variableDAO;

    public DataService(SPARQLService sparql, MongoDBService mongodb) {
        this.dataDAO = new DataDAO(mongodb, sparql);
        this.variableDAO = new VariableDAO(sparql,mongodb,null);
    }

    public List<VariableModel> getUsedVariables(DataSearchFilter filter, String lang) throws Exception {
        Set<URI> variableURIs = dataDAO.distinct(VARIABLE_FIELD, URI.class, filter, null);
        return variableDAO.getList(variableURIs, lang);
    }


}
