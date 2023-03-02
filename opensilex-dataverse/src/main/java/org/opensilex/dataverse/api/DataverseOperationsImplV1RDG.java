package org.opensilex.dataverse.api;

import com.researchspace.dataverse.entities.Dataset;
import com.researchspace.dataverse.entities.DataverseResponse;
import com.researchspace.dataverse.entities.Identifier;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import com.researchspace.dataverse.http.DataverseOperationsImplV1;
import org.springframework.http.HttpEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class DataverseOperationsImplV1RDG extends DataverseOperationsImplV1 {

    public DataverseOperationsImplV1RDG(){
    }
    @Override
    public Identifier createDataset(DatasetFacade facade, String dataverseAlias) {
        String url = this.createV1Url(new String[]{"dataverses", dataverseAlias, "datasets"});
        String json = this.getJsonFromFacade(facade);
        HttpEntity<String> entity = this.createHttpEntity(json);
        ParameterizedTypeReference<DataverseResponse<Identifier>> type = new ParameterizedTypeReference<DataverseResponse<Identifier>>() {
        };
        ResponseEntity<DataverseResponse<Identifier>> resp = this.template.exchange(url, HttpMethod.POST, entity, type, new Object[0]);
        this.handleError(resp);
        return (Identifier)((DataverseResponse)resp.getBody()).getData();
    }
    @Override
    protected String getJsonFromFacade(DatasetFacade facade) {
        Dataset dataset = new DatasetBuilderRDG().build(facade);
        return marshalDataset(dataset);
    }
}
