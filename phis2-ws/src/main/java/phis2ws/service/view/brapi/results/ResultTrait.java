//**********************************************************************************************
//                                       ResultTrait.java  
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: November, 23 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Trait;

/**
 * A class which represents the result part in the response form, adapted to the
 * traits
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultTrait extends Result<Trait> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param traits 
     */
    public ResultTrait(ArrayList<Trait> traits) {
        super(traits);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param traits
     * @param pagination
     * @param paginate 
     */
    public ResultTrait(ArrayList<Trait> traits, Pagination pagination, boolean paginate) {
        super(traits, pagination, paginate);
    }
}
