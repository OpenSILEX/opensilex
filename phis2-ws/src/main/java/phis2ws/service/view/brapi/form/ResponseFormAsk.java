//**********************************************************************************************
//                                       ResponseFormAsk.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier 30 2018
// Contact: eloan.lagire@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 30, 2018
// Subject: Allows the formating of the result of the Ask queries about Concept
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatAsk;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Ask;
import phis2ws.service.view.model.phis.Uri;

/**
 * Represent the ResponseForm for the Ask type
 *
 * @author Eloan LAGIER
 */
public class ResponseFormAsk extends ResultForm<Ask> {

    /**
     * Initialise les champs metadata et result
     *
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param ask liste des résultats
     * @param paginate
     */

    public ResponseFormAsk(int pageSize, int currentPage, ArrayList<Ask> ask, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, ask.size());
        if (ask.size() > 1) {
            result = new ResultatAsk(ask, metadata.getPagination(), paginate);
        } else {
            result = new ResultatAsk(ask);
        }
    }


}
