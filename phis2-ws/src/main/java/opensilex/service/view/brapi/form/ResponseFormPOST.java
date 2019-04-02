//******************************************************************************
//                             ResponseFormPOST.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi.form;

import java.util.List;
import opensilex.service.view.brapi.Status;

/**
 * Response form POST.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResponseFormPOST extends AbstractResultForm{

    public ResponseFormPOST() {
        super();
    }
    
    public ResponseFormPOST(Status status) {
        super(status);
    }
   
    public ResponseFormPOST(List<Status> statusList) {
        super(statusList);
    }
    
    @Override
    public int resultSize() {
            return 0;
    }
}
