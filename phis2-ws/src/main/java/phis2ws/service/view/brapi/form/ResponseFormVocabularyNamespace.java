//******************************************************************************
//                                       ResponseFormVocabularyNamespace.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 19 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  19 juin 2018
// Subject: Allows the formating of the result of the request about Vocabulary - properties
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultVocabularyNamespace;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Namespace;

/**
 * Allows the formating of the result of the request about Vocabulary - namespace
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResponseFormVocabularyNamespace extends ResultForm<Namespace>{
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormVocabularyNamespace(int pageSize, int currentPage, ArrayList<Namespace> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultVocabularyNamespace(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultVocabularyNamespace(list);
        }
    }
}
