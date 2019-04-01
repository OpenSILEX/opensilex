/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.view.brapi.form;

import java.util.List;
import opensilex.service.view.brapi.Status;

/**
 * 
 * @author A. Charleroy
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
