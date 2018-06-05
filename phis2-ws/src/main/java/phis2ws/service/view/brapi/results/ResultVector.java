//******************************************************************************
//                                       ResultVector.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 6 avr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  6 avr. 2018
// Subject: extend from Resultat, adapted to the vectors object list
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Vector;

/**
 * A class which represents the result part in the response form, adapted to the vectors
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultVector extends Resultat<Vector> {
    /**
     * @param vectors the vectors of the result 
     */
    public ResultVector(ArrayList<Vector> vectors) {
        super(vectors);
    }
    
    /**
     * @param vectors
     * @param pagination
     * @param paginate 
     */
    public ResultVector(ArrayList<Vector> vectors, Pagination pagination, boolean paginate) {
        super(vectors, pagination, paginate);
    }
}
