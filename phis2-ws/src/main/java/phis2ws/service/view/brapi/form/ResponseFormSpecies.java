//******************************************************************************
//                                       ResponseFormSpecies.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 déc. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.species.SpeciesDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultSpecies;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about species
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormSpecies extends ResultForm<SpeciesDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormSpecies(int pageSize, int currentPage, ArrayList<SpeciesDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultSpecies(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultSpecies(list);
        }
    }
    
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     * @param totalCount number of result
     */
    public ResponseFormSpecies(int pageSize, int currentPage, ArrayList<SpeciesDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultSpecies(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultSpecies(list);
        }
    }
}
