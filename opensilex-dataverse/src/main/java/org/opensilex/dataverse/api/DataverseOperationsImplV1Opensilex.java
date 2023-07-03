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

import java.util.Objects;

public class DataverseOperationsImplV1Opensilex extends DataverseOperationsImplV1 {

    public DataverseOperationsImplV1Opensilex(){
    }

    public Identifier createDataset(DatasetFacade facade, String dataverseAlias, String metadataLanguage) {
        String url = this.createV1Url("dataverses", dataverseAlias, "datasets");
        String json = this.getJsonFromFacade(facade, metadataLanguage);
        HttpEntity<String> entity = this.createHttpEntity(json);
        ParameterizedTypeReference<DataverseResponse<Identifier>> type = new ParameterizedTypeReference<DataverseResponse<Identifier>>() {
        };
        ResponseEntity<DataverseResponse<Identifier>> resp = this.template.exchange(url, HttpMethod.POST, entity, type);
        this.handleError(resp);
        return (Identifier)((DataverseResponse<?>) Objects.requireNonNull(resp.getBody())).getData();
    }

    protected String getJsonFromFacade(DatasetFacade facade, String metadataLanguage) {
        Dataset dataset = new DatasetBuilderOpensilex().build(facade, metadataLanguage);
        return marshalDataset(dataset);
    }
}
