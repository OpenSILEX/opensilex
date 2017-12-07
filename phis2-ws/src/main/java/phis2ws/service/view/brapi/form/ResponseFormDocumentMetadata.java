//**********************************************************************************************
//                                       ResponseFormDocumentMetadata.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: June 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  June, 2017
// Subject: extends ResultForm. Adapted to the labelViews list informations used in the documents
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatDocumentMetadata;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Document;

public class ResponseFormDocumentMetadata extends ResultForm<Document> {
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */
    public ResponseFormDocumentMetadata(int pageSize, int currentPage, ArrayList<Document> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultatDocumentMetadata(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultatDocumentMetadata(list);
        }
    }
}
