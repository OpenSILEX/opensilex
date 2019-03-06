//******************************************************************************
//                                       ResponseFormProvenance.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.provenance.ProvenanceDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultProvenance;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about provenance
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormProvenance extends ResultForm<ProvenanceDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormProvenance(int pageSize, int currentPage, ArrayList<ProvenanceDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultProvenance(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultProvenance(list);
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
    public ResponseFormProvenance(int pageSize, int currentPage, ArrayList<ProvenanceDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultProvenance(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultProvenance(list);
        }
    }
}

