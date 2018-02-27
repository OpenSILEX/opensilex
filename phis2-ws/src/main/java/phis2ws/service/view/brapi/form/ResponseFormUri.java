//**********************************************************************************************
//                                       ResponseFormUri.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: Feb 26 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Feb 26, 2018
// Subject: Allows the formating of the result of the queries about Uri
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatUri;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Uri;

/**
 * Represente the Response Form Uri
 * @author Eloan LAGIER
 */
public class ResponseFormUri extends ResultForm<Uri>{


    public ResponseFormUri(Integer pageSize, Integer currentPage, ArrayList<Uri> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultatUri(list, metadata.getPagination(), paginate);
            } else {
               result = new ResultatUri(list);
           }

     }



}
