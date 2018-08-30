//**********************************************************************************************
//                                       ResultVariable.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 23 Novemnber, 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Variable;

/**
 * A class which represents the result part in the response form, adapted to the
 * variables
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultVariable extends Result<Variable> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param variables 
     */
    public ResultVariable(ArrayList<Variable> variables) {
        super(variables);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param variables
     * @param pagination
     * @param paginate 
     */
    public ResultVariable(ArrayList<Variable> variables, Pagination pagination, boolean paginate) {
        super(variables, pagination, paginate);
    }
}
