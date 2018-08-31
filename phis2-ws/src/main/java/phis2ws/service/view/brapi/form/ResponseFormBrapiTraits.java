//******************************************************************************
//                                       ResponseFormBrapiTraits.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 31 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultBrapiTrait;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.BrapiTrait;

/**
 * Formating the result of the request on traits list
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResponseFormBrapiTraits extends ResultForm {
     /**
     * Initializes Metadata and Results fields
     * @param pageSize number of results per page
     * @param currentPage requested page
     * @param traits all traits available
     * @param paginate 
     */
    public ResponseFormBrapiTraits(int pageSize, int currentPage, ArrayList<BrapiTrait> traits, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, traits.size());
        if (traits.size() > 1) {
            result = new ResultBrapiTrait(traits, metadata.getPagination(), paginate); 
        } else {
            result = new ResultBrapiTrait(traits);
        }
    }
}
