//******************************************************************************
//                                       ResultSpecies.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 déc. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.species.SpeciesDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the species
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultSpecies extends Result<SpeciesDTO> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param measures 
     */
    public ResultSpecies(ArrayList<SpeciesDTO> measures) {
        super(measures);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param measures
     * @param pagination
     * @param paginate 
     */
    public ResultSpecies(ArrayList<SpeciesDTO> measures, Pagination pagination, boolean paginate) {
        super(measures, pagination, paginate);
    }
}
