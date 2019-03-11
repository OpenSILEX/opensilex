//******************************************************************************
//                                       ResultAnnotation.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.annotation.AnnotationDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the annotation
 * @update [Andréas Garcia] 15 Feb 2019: Use AnnotationDTO instead of Model
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResultAnnotation extends Result<AnnotationDTO >{
    /**
     * @param annotations the annotations of the result 
     */
    public ResultAnnotation(ArrayList<AnnotationDTO> annotations) {
        super(annotations);
    }
    
    /**
     * @param annotations
     * @param pagination
     * @param paginate 
     */
    public ResultAnnotation(ArrayList<AnnotationDTO> annotations, Pagination pagination, boolean paginate) {
        super(annotations, pagination, paginate);
    }
}
