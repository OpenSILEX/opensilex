//******************************************************************************
//                                       ResponseFormStudyDetails.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultStudyDetails;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.StudyDetails;

/**
 * Formating the result of the request on studies-search
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResponseFormStudyDetails extends ResultForm {
    /**
     * Initializes Metadata and Results fields
     * @param pageSize number of results per page
     * @param currentPage requested page
     * @param study the requested study
     * @param paginate 
     */
    public ResponseFormStudyDetails(int pageSize, int currentPage, StudyDetails study, boolean paginate) {
        ArrayList<StudyDetails> list = new ArrayList();
        if (study.getStudyDbId() != null){
            list.add(study);
        }        
        metadata = new Metadata(pageSize, currentPage, 1);
        result = new ResultStudyDetails(list);
    }
}
