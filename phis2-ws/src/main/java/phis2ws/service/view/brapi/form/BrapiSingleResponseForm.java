//******************************************************************************
//                                       BrapiSingleResponseForm.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 22 janv. 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/boizetal/OpenSilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import phis2ws.service.view.brapi.BrapiMetadata;

/**
 *
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiSingleResponseForm<T> {
    protected BrapiMetadata metadata;
    protected T result;
    
    public BrapiSingleResponseForm(T data) {
        metadata = new BrapiMetadata(0,0,0);
        result = data;
    }
    
    public BrapiMetadata getMetadata() {
        return metadata;
    }

    public T getResult() {
        return result;
    }        
    
}
