//******************************************************************************
//                                       ResponseFormEnvironmentMeasure.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 nov. 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.environment.EnvironmentMeasureDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultEnvironmentMeasure;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about Environment Measures
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResponseFormEnvironmentMeasure extends ResultForm<EnvironmentMeasureDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormEnvironmentMeasure(int pageSize, int currentPage, ArrayList<EnvironmentMeasureDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultEnvironmentMeasure(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultEnvironmentMeasure(list);
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
    public ResponseFormEnvironmentMeasure(int pageSize, int currentPage, ArrayList<EnvironmentMeasureDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultEnvironmentMeasure(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultEnvironmentMeasure(list);
        }
    }
}
