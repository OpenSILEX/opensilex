//******************************************************************************
//                     ResponseFormEventSimple.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.event.EventSimpleDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultEventSimple;
import phis2ws.service.view.manager.ResultForm;

/**
 * Do formatting of the result of a request of a Event with basic information
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class ResponseFormEventSimple extends ResultForm {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormEventSimple(int pageSize, int currentPage, ArrayList<EventSimpleDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultEventSimple(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultEventSimple(list);
        }
    }
    
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     * @param totalCount number of result
     */
    public ResponseFormEventSimple(int pageSize, int currentPage, ArrayList<EventSimpleDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultEventSimple(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultEventSimple(list);
        }
    }
}
