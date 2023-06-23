package org.opensilex.dataverse.api;

import com.researchspace.dataverse.api.v1.DataverseConfig;
import com.researchspace.dataverse.http.DataverseAPIImpl;

public class DataverseAPIImplOpensilex extends DataverseAPIImpl {

    protected DataverseOperationsImplV1Opensilex dvOperationsImpl = new DataverseOperationsImplV1Opensilex();
    public DataverseAPIImplOpensilex(){
    }
    @Override
    public void configure(DataverseConfig config) {
        this.dvOperationsImpl.configure(config);
    }

    @Override
    public DataverseOperationsImplV1Opensilex getDataverseOperations() {
        return this.dvOperationsImpl;
    }
}
