//******************************************************************************
//                                       ResultVector.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 6 avr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Vector;

/**
 * A class which represents the result part in the response form, adapted to the
 * vectors
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultVector extends Result<Vector> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param vectors 
     */
    public ResultVector(ArrayList<Vector> vectors) {
        super(vectors);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param vectors
     * @param pagination
     * @param paginate 
     */
    public ResultVector(ArrayList<Vector> vectors, Pagination pagination, boolean paginate) {
        super(vectors, pagination, paginate);
    }
}
