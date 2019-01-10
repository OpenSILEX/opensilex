//**********************************************************************************************
//                               ResultScientificObject.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************

package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.ScientificObject;

/**
 * A class which represents the result part in the response form, adapted to the
 * scientific objects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultScientificObject extends Result<ScientificObject> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param scientificObjectList 
     */
    public ResultScientificObject(ArrayList<ScientificObject> scientificObjectList) {
        super(scientificObjectList);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param scientificObjectList
     * @param pagination
     * @param paginate 
     */
    public ResultScientificObject(ArrayList<ScientificObject> scientificObjectList, Pagination pagination, boolean paginate) {
        super(scientificObjectList, pagination, paginate);
    }
}
