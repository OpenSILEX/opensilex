//******************************************************************************
//                                       ResultAnnotation.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 21 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  21 juin 2018
// Subject: extend from Resultat, adapted to the annotation object list
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Annotation;

/**
 * A class which represents the result part in the response form, adapted to the annotation
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResultAnnotation extends Resultat<Annotation> {
    /**
     * @param annotations the annotations of the result 
     */
    public ResultAnnotation(ArrayList<Annotation> annotations) {
        super(annotations);
    }
    
    /**
     * @param annotations
     * @param pagination
     * @param paginate 
     */
    public ResultAnnotation(ArrayList<Annotation> annotations, Pagination pagination, boolean paginate) {
        super(annotations, pagination, paginate);
    }
}
