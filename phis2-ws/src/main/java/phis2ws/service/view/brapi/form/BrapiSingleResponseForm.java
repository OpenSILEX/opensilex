//******************************************************************************
//                                       BrapiSingleResponseForm.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.manager.BrapiSingleResult;
import phis2ws.service.view.manager.BrapiSingleResultForm;

/**
 * Allows the formating of the result of the request about any object T when it returns only one element
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiSingleResponseForm<T> extends BrapiSingleResultForm<T>{

    public BrapiSingleResponseForm(T data) {
        metadata = new Metadata(0, 0, 1);
        result = new BrapiSingleResult(data);
    }
    
}
