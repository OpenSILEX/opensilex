//******************************************************************************
//                                       ResultVocabularyProperty.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 19 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.PropertyVocabularyDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the
 * vocabulary properties
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultVocabularyProperty extends Result<PropertyVocabularyDTO> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param properties 
     */
    public ResultVocabularyProperty(ArrayList<PropertyVocabularyDTO> properties) {
        super(properties);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param properties
     * @param pagination
     * @param paginate 
     */
    public ResultVocabularyProperty(ArrayList<PropertyVocabularyDTO> properties, Pagination pagination, boolean paginate) {
        super(properties, pagination, paginate);
    }
}
