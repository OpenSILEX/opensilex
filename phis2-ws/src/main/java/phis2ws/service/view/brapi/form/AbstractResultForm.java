/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.view.brapi.form;

import java.util.List;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.Status;

/**
 * 
 * @author A. CHARLEROY
 */
public abstract class AbstractResultForm {

    protected Metadata metadata;

    public AbstractResultForm() {
       metadata = new Metadata();
    }
    
    public AbstractResultForm(Status status) {
       metadata = new Metadata(status);
    }
   
   
    public AbstractResultForm(List<Status> statusList) {
        metadata = new Metadata(statusList);
    }
    

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }


    public Metadata getMetadata() {
        return metadata;
    }
    
    abstract int resultSize();


    
    
}
