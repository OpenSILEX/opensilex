//**********************************************************************************************
//                                       ResultUnit.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 23 November, 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Unit;

/**
 * A class which represents the result part in the response form, adapted to the
 * units
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultUnit extends Result<Unit> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param units 
     */
    public ResultUnit(ArrayList<Unit> units) {
        super(units);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param units
     * @param pagination
     * @param paginate 
     */
    public ResultUnit(ArrayList<Unit> units, Pagination pagination, boolean paginate) {
        super(units, pagination, paginate);
    }
}
