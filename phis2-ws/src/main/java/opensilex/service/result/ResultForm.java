//******************************************************************************
//                              ResultForm.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: 3 December 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.result;

import java.util.ArrayList;
import java.util.List;
import opensilex.service.view.brapi.Metadata;
import opensilex.service.view.brapi.Status;

/**
 * Result form.
 * @param <T> DTO handled by the form
 * @see Result
 * @author Samuël Chérimont
 */
public class ResultForm<T> {

    protected Metadata metadata;
    protected Result result;

    /**
     * Initializes metadata and result fields.
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResultForm(int pageSize, int currentPage, ArrayList<T> list, boolean paginate) {
    	this(pageSize, currentPage, list, paginate, list.size());
    }
    
    /**
     * Initializes metadata and result fields.
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     * @param totalCount number of result
     */
    public ResultForm(int pageSize, int currentPage, ArrayList<T> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new Result<>(list, metadata.getPagination(), paginate);
        } else {
            result = new Result<>(list);
        }
    }
    
    public int resultSize() {
        return result.dataSize();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Result getResult() {
        return result;
    }
    
    public void setStatus(List<Status> status){
        metadata.setStatus(status);
    }
}
