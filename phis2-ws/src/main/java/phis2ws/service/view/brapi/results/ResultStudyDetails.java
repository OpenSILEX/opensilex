//******************************************************************************
//                                       ResultStudyDetails.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.StudyDetails;

/**
 * A class which represents the result part in the response form, adapted to the
 * StudyDetails
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResultStudyDetails extends Result<StudyDetails> {
     /**
     * Constructor which calls the mother-class constructor
     * @param study  the study to detail
     */
    public ResultStudyDetails(ArrayList<StudyDetails> study) {
        super(study);
    }
}
