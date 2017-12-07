//**********************************************************************************************
//                                       ResultatDocumentType.java 
//
// Author(s): Arnaud CHARLEROY, Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October 2016
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2017
// Subject: extends Resultat. Adapted to a labelView list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;

public class ResultatDocumentType extends Resultat<String> {
    public ResultatDocumentType(ArrayList<String> list) {
        super(list);
    }

    public ResultatDocumentType(ArrayList<String> list, Pagination pagination, boolean paginate) {
        super(list, pagination, paginate);
    }
}
