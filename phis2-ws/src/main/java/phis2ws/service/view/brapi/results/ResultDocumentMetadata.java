//**********************************************************************************************
//                                       ResultDocumentMetadata.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: June 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Document;

/**
 * A class which represents the result part in the response form, adapted to the
 * Documents
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultDocumentMetadata extends Result<Document> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param documentsList 
     */
    public ResultDocumentMetadata(ArrayList<Document> documentsList) {
        super(documentsList);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param documentsList
     * @param pagination
     * @param paginate 
     */
    public ResultDocumentMetadata(ArrayList<Document> documentsList, Pagination pagination, boolean paginate) {
        super(documentsList, pagination, paginate);
    }
}
