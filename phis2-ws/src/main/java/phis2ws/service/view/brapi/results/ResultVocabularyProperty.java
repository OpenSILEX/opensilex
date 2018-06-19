//******************************************************************************
//                                       ResultVocabularyProperty.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 19 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  19 juin 2018
// Subject: extend from Resultat, adapted to the vocabulary property object list
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.PropertyVocabularyDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;

/**
 * A class which represents the result part in the response form, adapted to the vocabulary property
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultVocabularyProperty extends Resultat<PropertyVocabularyDTO> {
    
    /**
     * @param properties the sensors of the result 
     */
    public ResultVocabularyProperty(ArrayList<PropertyVocabularyDTO> properties) {
        super(properties);
    }
    
    /**
     * @param sensors
     * @param pagination
     * @param paginate 
     */
    public ResultVocabularyProperty(ArrayList<PropertyVocabularyDTO> sensors, Pagination pagination, boolean paginate) {
        super(sensors, pagination, paginate);
    }
}
