//******************************************************************************
//                                       BrapiResponseForm.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.BrapiMetadata;
import phis2ws.service.view.manager.BrapiMultiResult;
import phis2ws.service.view.manager.BrapiResult;
import phis2ws.service.view.manager.BrapiSingleResult;

/**
 * Allows the formating of the result of the request about any object T
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiResponseForm<T> {
    protected BrapiMetadata metadata;
    protected BrapiResult result;
    
    /**
     * Initialize metadata and result fields when there are several elements
     * @param pageSize the number of results per page
     * @param currentPage the requested page
     * @param data list of results
     * @param paginate true : already paginate
     *                 false : needs pagination
     */
    public BrapiResponseForm(int pageSize, int currentPage, ArrayList<T> data, boolean paginate) {
        metadata = new BrapiMetadata(pageSize, currentPage, data.size());
        result = new BrapiMultiResult(data,  metadata.getPagination(), paginate);
    }  

    /**
     * Initialize metadata and result fields when there is only one element
     * In this case, the four fields of the pagination are equal to 0
     * @param data the result of the request
     */
    public BrapiResponseForm(T data) {
        metadata = new BrapiMetadata(0, 0, 0);
        result = new BrapiSingleResult(data);
    }  

    public BrapiMetadata getMetadata() {
        return metadata;
    }

    public BrapiResult getResult() {
        return result;
    }        
}
