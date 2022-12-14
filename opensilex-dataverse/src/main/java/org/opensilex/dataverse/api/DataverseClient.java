package org.opensilex.dataverse.api;

import com.researchspace.dataverse.api.v1.*;
import com.researchspace.dataverse.api.v1.DataverseAPI;
import com.researchspace.dataverse.entities.Identifier;
import com.researchspace.dataverse.entities.facade.DatasetAuthor;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import com.researchspace.dataverse.entities.facade.DatasetTopicClassification;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.dataverse.DataverseModule;
import org.opensilex.dataverse.OpensilexDataverseConfig;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Client for the dataverse API
 * Uses the java dataverse client : https://github.com/IQSS/dataverse-client-java
 */
@Provider
public class DataverseClient {

    @Inject
    private OpenSilex opensilex;

    protected DataverseAPI dataverseExternalAPI; // How to not confuse DataverseAPI?
    protected DataverseConfig dataverseConfig;
    protected DatasetOperations datasetOps;
    protected DataverseOperations dataverseOps;
    protected MetadataOperations metadataOPs;
    protected InfoOperations infoOps;
    protected SearchOperations searchOps;

    protected String dataverseBasePath;
    protected String dataverseAlias;
    protected String externalAPIKey;

    public DataverseClient(String dataverseBasePath, String dataverseAlias, String externalAPIKey) throws MalformedURLException, OpenSilexModuleNotFoundException {

        OpensilexDataverseConfig opensilexDataverseConfig = opensilex.getModuleConfig(
                DataverseModule.class, OpensilexDataverseConfig.class
        );

        if(StringUtils.isEmpty(dataverseBasePath)){
            this.dataverseBasePath = opensilexDataverseConfig.dataverseBasePath();
        }else{
            this.dataverseBasePath = dataverseBasePath;
        }
        if(StringUtils.isEmpty(dataverseAlias)){
            this.dataverseAlias = opensilexDataverseConfig.dataverseAlias();
        }else{
            this.dataverseAlias = dataverseAlias;
        }
        if(StringUtils.isEmpty(externalAPIKey)){
            this.externalAPIKey = opensilexDataverseConfig.externalAPIKey();
        }else{
            this.externalAPIKey = externalAPIKey;
        }

        this.dataverseConfig = new DataverseConfig(new URL(dataverseBasePath), externalAPIKey, dataverseAlias);
        dataverseExternalAPI.configure(dataverseConfig);
        this.metadataOPs = dataverseExternalAPI.getMetadataOperations();
        this.datasetOps = dataverseExternalAPI.getDatasetOperations();
        this.dataverseOps = dataverseExternalAPI.getDataverseOperations();
    }

    public DataverseClient() throws MalformedURLException, OpenSilexModuleNotFoundException{
        this("", "", "");
    }

    /**
     * Creates a dataset on the given this.dataverse
     * @return a string representation of the created dataset's DOI
     * @see <a href=https://guides.dataverse.org/en/latest/api/native-api.html>Dataverse's Native API</a>
     */
    public Identifier createADataset(DataverseAPIPostDatasetDTO datasetDTO) throws MalformedURLException, URISyntaxException {

        DatasetFacade facade = DatasetFacade.builder()
                .author(
                        DatasetAuthor.builder()
                                .authorName("test_authorName")
                                .authorAffiliation("test_authorAffiliation")
                                .authorIdentifierScheme("test_authorIdentifierScheme")
                                .authorIdentifier("test_authorIdentifier")
                                .build()
                )
                .title(datasetDTO.name)
                .topicClassification(
                        DatasetTopicClassification.builder()
                                .topicClassValue(datasetDTO.topic)
                                .build()
                )
                .productionDate(datasetDTO.productionDate)
                .build();
        Identifier datasetId = dataverseOps.createDataset(facade, dataverseAlias);
        return datasetId;
    }

    // TODO : I could make a getDataverseMetadataBlocks or get DataverseFields for example
}
