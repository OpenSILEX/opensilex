//******************************************************************************
//                                       ResponseFormDocumentMetadata.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Jun, 2017
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultDocumentMetadata;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Document;

/**
 * Extends ResultForm, adapted to the document list informations used in the documents.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Arnaud Charleroy] 07, September 2018 : add a construtor to take in account the 
 *                                                 total count of element instead of caluculate 
 *                                                 it from the list
 *
 */
public class ResponseFormDocumentMetadata extends ResultForm<Document> {
    
    /**
     * Initiate metadata and result response fields
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate need to be paginate or not
     */
    public ResponseFormDocumentMetadata(int pageSize, int currentPage, ArrayList<Document> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultDocumentMetadata(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultDocumentMetadata(list);
        }
    }
    
    /**
     * Initiate metadata and result response fields
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate need to be paginate or not
     * @param totalCount the number of the items returned by the query
     */
    public ResponseFormDocumentMetadata(int pageSize, int currentPage, ArrayList<Document> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (totalCount > 1) {
            result = new ResultDocumentMetadata(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultDocumentMetadata(list);
        }
    }
}
