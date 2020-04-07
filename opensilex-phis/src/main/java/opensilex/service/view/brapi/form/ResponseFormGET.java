//******************************************************************************
//                             ResponseFormGET.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi.form;

import java.util.List;
import opensilex.service.view.brapi.Status;

/**
 * Response form GET.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ResponseFormGET extends AbstractResultForm{

    public ResponseFormGET() {
        super();
    }
    
    public ResponseFormGET(Status status) {
        super(status);
    }
   
    public ResponseFormGET(List<Status> statusList) {
        super(statusList);
    }
    
    @Override
    public int resultSize() {
            return 0;
    }
}
