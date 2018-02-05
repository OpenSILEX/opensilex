//**********************************************************************************************
//                                       ResponseFormImageMetadata.java
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 2 janv. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  2 janv. 2018
// Subject: Allows the formating of the result of the reqyest about Image Metadata
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultImageMetadata;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.ImageMetadata;

/**
 * Allows the formating of the result of the request about Image Metadata
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormImageMetadata extends ResultForm<ImageMetadata> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormImageMetadata(int pageSize, int currentPage, ArrayList<ImageMetadata> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultImageMetadata(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultImageMetadata(list);
        }
    }
}
