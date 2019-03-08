//******************************************************************************
//                                       ResponseFormAnnotation.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.annotation.AnnotationDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultAnnotation;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about Annotation
 * @update [Andréas Garcia] 15 Feb 2019: Use AnnotationDTO instead of Model
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResponseFormAnnotation extends ResultForm {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormAnnotation(int pageSize, int currentPage, ArrayList<AnnotationDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultAnnotation(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultAnnotation(list);
        }
    }
    
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     * @param totalCount number of result
     */
    public ResponseFormAnnotation(int pageSize, int currentPage, ArrayList<AnnotationDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultAnnotation(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultAnnotation(list);
        }
    }
}
