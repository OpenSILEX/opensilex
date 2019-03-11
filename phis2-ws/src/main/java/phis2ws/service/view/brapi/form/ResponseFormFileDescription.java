//******************************************************************************
//                          ResponseFormData.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 March, 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.data.FileDescriptionDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultFileDescription;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about data file description
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResponseFormFileDescription extends ResultForm<FileDescriptionDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormFileDescription(int pageSize, int currentPage, ArrayList<FileDescriptionDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultFileDescription(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultFileDescription(list);
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
    public ResponseFormFileDescription(int pageSize, int currentPage, ArrayList<FileDescriptionDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultFileDescription(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultFileDescription(list);
        }
    }
}
