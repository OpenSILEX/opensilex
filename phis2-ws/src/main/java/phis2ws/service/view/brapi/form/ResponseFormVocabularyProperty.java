//******************************************************************************
//                                       ResponseFormVocabularyProperty.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 19 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  19 juin 2018
// Subject: Allows the formating of the result of the request about Vocabulary - properties
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.PropertyVocabularyDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultVocabularyProperty;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about Vocabulary - properties
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormVocabularyProperty extends ResultForm<PropertyVocabularyDTO>{
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormVocabularyProperty(int pageSize, int currentPage, ArrayList<PropertyVocabularyDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultVocabularyProperty(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultVocabularyProperty(list);
        }
    }
}
