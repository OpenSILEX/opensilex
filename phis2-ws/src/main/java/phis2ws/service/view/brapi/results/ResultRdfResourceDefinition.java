//******************************************************************************
//                                       ResultRdfResourceDefinition.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Subject: Represents the submitted JSON for a property
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to a generic property list
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResultRdfResourceDefinition extends Result<RdfResourceDefinitionDTO> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param properties 
     */
    public ResultRdfResourceDefinition(ArrayList<RdfResourceDefinitionDTO> properties) {
        super(properties);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param properties
     * @param pagination
     * @param paginate 
     */
    public ResultRdfResourceDefinition(ArrayList<RdfResourceDefinitionDTO> properties, Pagination pagination, boolean paginate) {
        super(properties, pagination, paginate);
    }
}

