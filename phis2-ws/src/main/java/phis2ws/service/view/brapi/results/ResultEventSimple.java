//******************************************************************************
//                           ResultEventSimple.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.event.EventSimpleDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to 
 * events with basic information
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class ResultEventSimple extends Result<EventSimpleDTO> {
    /**
     * Constructor which calls the mother-class constructor in the case of a 
     * list with only 1 element
     * @param events 
     */
    public ResultEventSimple(ArrayList<EventSimpleDTO> events) {
        super(events);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a list 
     * with several elements
     * @param events
     * @param pagination
     * @param paginate 
     */
    public ResultEventSimple(ArrayList<EventSimpleDTO> events, Pagination pagination, boolean paginate) {
        super(events, pagination, paginate);
    }
}
