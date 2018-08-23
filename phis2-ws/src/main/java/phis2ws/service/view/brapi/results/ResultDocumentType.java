//**********************************************************************************************
//                                       ResultDocumentType.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: October 2016
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the
 * documents types
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultDocumentType extends Result<String> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param list 
     */
    public ResultDocumentType(ArrayList<String> list) {
        super(list);
    }

    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param list
     * @param pagination
     * @param paginate 
     */
    public ResultDocumentType(ArrayList<String> list, Pagination pagination, boolean paginate) {
        super(list, pagination, paginate);
    }
}
