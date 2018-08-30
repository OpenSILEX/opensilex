//**********************************************************************************************
//                                       ResultCall.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Call;

/**
 * A class which represents the result part in the response form, adapted to the
 * calls
 *
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResultCall extends Result<Call> {

    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     *
     * @param callsList List of calls with only one element
     */
    public ResultCall(ArrayList<Call> callsList) {
        super(callsList);
    }

    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     *
     * @param callsList List of calls
     * @param pagination pagination object allowing to sort the calls list
     * @param paginate
     */
    public ResultCall(ArrayList<Call> callsList, Pagination pagination,
            boolean paginate) {
        super(callsList, pagination, paginate);
    }
}
