//******************************************************************************
//                                       ResultBrapiTrait.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 31 Aug, 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.BrapiTrait;

/**
 * A class which represents the result part in the response form, adapted to the
 * traits
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResultBrapiTrait extends Result<BrapiTrait>{
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param traits 
     */
    public ResultBrapiTrait(ArrayList<BrapiTrait> traits) {
        super(traits);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param traits
     * @param pagination
     * @param paginate 
     */
    public ResultBrapiTrait(ArrayList<BrapiTrait> traits, Pagination pagination, 
            boolean paginate) {
        super(traits, pagination, paginate);
    }
}
