//******************************************************************************
//                       ResultEventDetailed.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 15 Feb., 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.event.EventDetailedDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, 
 * adapted to the events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class ResultEventDetailed extends Result<EventDetailedDTO> {
    /**
     * Constructor which calls the mother-class constructor 
     * in the case of a list with only 1 element
     * @param events 
     */
    public ResultEventDetailed(ArrayList<EventDetailedDTO> events) {
        super(events);
    }
    
    /**
     * Contructor which calls the mother-class constructor 
     * in the case of a list with several elements
     * @param events
     * @param pagination
     * @param paginate 
     */
    public ResultEventDetailed(ArrayList<EventDetailedDTO> events, Pagination pagination, boolean paginate) {
        super(events, pagination, paginate);
    }
}
