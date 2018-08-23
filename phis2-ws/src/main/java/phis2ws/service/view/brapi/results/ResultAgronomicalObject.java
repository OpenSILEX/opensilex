//**********************************************************************************************
//                               ResultAgronomicalObject.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************

package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.AgronomicalObject;

/**
 * A class which represents the result part in the response form, adapted to the
 * agronomical objects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultAgronomicalObject extends Result<AgronomicalObject> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param agronomicalObjectList 
     */
    public ResultAgronomicalObject(ArrayList<AgronomicalObject> agronomicalObjectList) {
        super(agronomicalObjectList);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param agronomicalObjectList
     * @param pagination
     * @param paginate 
     */
    public ResultAgronomicalObject(ArrayList<AgronomicalObject> agronomicalObjectList, Pagination pagination, boolean paginate) {
        super(agronomicalObjectList, pagination, paginate);
    }
}
