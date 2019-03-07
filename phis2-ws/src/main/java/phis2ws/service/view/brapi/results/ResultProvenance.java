//******************************************************************************
//                                       ResultProvenance.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.provenance.ProvenanceDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the provenance
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultProvenance extends Result<ProvenanceDTO> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param measures 
     */
    public ResultProvenance(ArrayList<ProvenanceDTO> measures) {
        super(measures);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param measures
     * @param pagination
     * @param paginate 
     */
    public ResultProvenance(ArrayList<ProvenanceDTO> measures, Pagination pagination, boolean paginate) {
        super(measures, pagination, paginate);
    }
}

