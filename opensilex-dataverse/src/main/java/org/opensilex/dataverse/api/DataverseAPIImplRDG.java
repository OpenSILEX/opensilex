package org.opensilex.dataverse.api;

import com.researchspace.dataverse.api.v1.DataverseConfig;
import com.researchspace.dataverse.api.v1.DataverseOperations;
import com.researchspace.dataverse.http.DataverseAPIImpl;
import com.researchspace.dataverse.http.DataverseOperationsImplV1;

public class DataverseAPIImplRDG extends DataverseAPIImpl {

    protected DataverseOperationsImplV1RDG dvOperationsImpl = new DataverseOperationsImplV1RDG();
    public DataverseAPIImplRDG(){
    }
    @Override
    public void configure(DataverseConfig config) {
        this.dvOperationsImpl.configure(config);
    }

    @Override
    public DataverseOperationsImplV1RDG getDataverseOperations() {
        return this.dvOperationsImpl;
    }
}
