//**********************************************************************************************
//                                       ResultMethod.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 23 November, 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Method;

/**
 * A class which represents the result part in the response form, adapted to the
 * methods
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultMethod extends Result<Method> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param methods 
     */
    public ResultMethod(ArrayList<Method> methods) {
        super(methods);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param methods
     * @param pagination
     * @param paginate 
     */
    public ResultMethod(ArrayList<Method> methods, Pagination pagination, boolean paginate) {
        super(methods, pagination, paginate);
    }
}
