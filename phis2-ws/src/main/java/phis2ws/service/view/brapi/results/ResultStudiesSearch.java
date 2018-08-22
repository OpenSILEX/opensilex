//******************************************************************************
//                                       ResultStudiesSearch.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.StudiesSearch;

/**
 * A class which represents the result part in the response form, adapted to the
 * studies-search
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResultStudiesSearch extends Resultat<StudiesSearch> {
    
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param studiesList List of studies with only one element
     */
    public ResultStudiesSearch(ArrayList<StudiesSearch> studiesList) {
        super(studiesList);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param studiesList List of studies
     * @param pagination pagination object allowing to sort the calls list
     * @param paginate
     */
    public ResultStudiesSearch(ArrayList<StudiesSearch> studiesList, Pagination pagination, 
            boolean paginate) {
        super(studiesList, pagination, paginate);
    }
}
