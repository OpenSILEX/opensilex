//******************************************************************************
//                                       ResponseFormFileMetadata.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 3 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.FileMetadataDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultFileMetadata;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formatting of the result of the request about FileMetadata
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormFileMetadata extends ResultForm<FileMetadataDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormFileMetadata(int pageSize, int currentPage, ArrayList<FileMetadataDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultFileMetadata(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultFileMetadata(list);
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
    public ResponseFormFileMetadata(int pageSize, int currentPage, ArrayList<FileMetadataDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultFileMetadata(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultFileMetadata(list);
        }
    }
}
