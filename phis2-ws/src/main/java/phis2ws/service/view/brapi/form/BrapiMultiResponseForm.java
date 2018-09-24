//******************************************************************************
//                                       BrapiMultiResponseForm.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.manager.BrapiMultiResult;
import phis2ws.service.view.manager.BrapiMultiResultForm;

/**
 * Allows the formating of the result of the request about any object T it returns a list of several elements
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiMultiResponseForm<T> extends BrapiMultiResultForm<T>{

    /**
     * Initialize metadata and result fields when there are several elements
     * @param pageSize the number of results per page
     * @param currentPage the requested page
     * @param data list of results
     * @param paginate 
     */
    public BrapiMultiResponseForm(int pageSize, int currentPage, ArrayList<T> data, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, data.size());
        result = new BrapiMultiResult(data,  metadata.getPagination(), paginate);
    }    
}
