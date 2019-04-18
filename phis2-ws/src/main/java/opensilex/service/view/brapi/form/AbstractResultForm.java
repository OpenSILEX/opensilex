//******************************************************************************
//                          AbstractResultForm.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Aug. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi.form;

import java.util.List;
import opensilex.service.view.brapi.Metadata;
import opensilex.service.view.brapi.Status;

/**
 * Abstract result form.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
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
