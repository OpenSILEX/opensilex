/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.view.brapi.form;

import java.util.List;
import phis2ws.service.view.brapi.Status;

/**
 * 
 * @author A. Charleroy
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
