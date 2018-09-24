//******************************************************************************
//                                       BrapiMultiResultForm.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.manager;

import java.util.List;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.Status;

/**
 * This class provides the format of the output of each brapi service where there is a list of several elements
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiMultiResultForm<T> {
    
    protected Metadata metadata;
    protected BrapiMultiResult result;   
     
    public Metadata getMetadata() {
        return metadata;
    }

    public BrapiMultiResult getResult() {
        return result;
    }
    
    public void setStatus(List<Status> status){
        metadata.setStatus(status);
    }
}
