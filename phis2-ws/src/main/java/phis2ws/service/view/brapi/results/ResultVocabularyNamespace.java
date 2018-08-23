//******************************************************************************
//                                       ResultVocabularyNamespace.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 15 july 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  15 july 2018
// Subject: extend from Resultat, adapted to the vocabulary namespace object list
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Namespace;

/**
 * A class which represents the result part in the response form, adapted to the vocabulary namespace
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResultVocabularyNamespace extends Result<Namespace> {
    
    /**
     * @param properties the namespaces of the result 
     */
    public ResultVocabularyNamespace(ArrayList<Namespace> properties) {
        super(properties);
    }
    
    /**
     * @param namespaces
     * @param pagination
     * @param paginate 
     */
    public ResultVocabularyNamespace(ArrayList<Namespace> namespaces, Pagination pagination, boolean paginate) {
        super(namespaces, pagination, paginate);
    }
}
