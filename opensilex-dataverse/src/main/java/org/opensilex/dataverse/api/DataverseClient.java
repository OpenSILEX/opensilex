package org.opensilex.dataverse.api;

import com.researchspace.dataverse.api.v1.*;
import com.researchspace.dataverse.api.v1.DataverseAPI;
import com.researchspace.dataverse.entities.Identifier;
import com.researchspace.dataverse.entities.facade.*;
import com.researchspace.dataverse.http.DataverseAPIImpl;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.experiment.factor.api.FactorAPI;
import org.opensilex.dataverse.DataverseModule;
import org.opensilex.dataverse.OpensilexDataverseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Client for the dataverse API
 * Uses the java dataverse client : https://github.com/IQSS/dataverse-client-java
 */
@Provider
public class DataverseClient {

    protected DataverseAPIImplRDG dataverseExternalAPI = new DataverseAPIImplRDG();
    protected DataverseConfig dataverseConfig;
    protected DataverseOperationsImplV1RDG dataverseOps;

    protected String dataverseBasePath;
    protected String dataverseAlias;
    protected String externalAPIKey;

    public static final Logger LOGGER = LoggerFactory.getLogger(DataverseClient.class);

    public DataverseClient(String dataverseBasePath, String dataverseAlias, String externalAPIKey, OpensilexDataverseConfig opensilexDataverseConfig) throws MalformedURLException, OpenSilexModuleNotFoundException {

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

        this.dataverseConfig = new DataverseConfig(new URL(this.dataverseBasePath), this.externalAPIKey, this.dataverseAlias);
        this.dataverseExternalAPI.configure(this.dataverseConfig);
        this.dataverseOps = this.dataverseExternalAPI.getDataverseOperations();
    }

    public DataverseClient(OpensilexDataverseConfig opensilexDataverseConfig) throws MalformedURLException, OpenSilexModuleNotFoundException{
        this(null, null, null, opensilexDataverseConfig);
    }

    /**
     * Creates a dataset on the given this.dataverse
     * @return a string representation of the created dataset's DOI
     * @see <a href=https://guides.dataverse.org/en/latest/api/native-api.html>Dataverse's Native API</a>
     */
    public Identifier createADataset(DataverseAPIPostDatasetDTO datasetDTO) throws MalformedURLException, URISyntaxException {

        DatasetFacade facade = DatasetFacade.builder()
                .title(datasetDTO.name)
                .contact(
                        DatasetContact.builder()
                                .datasetContactEmail("gabriel.besombes@inrae.fr")
                                .build()
                )
                .author(
                        DatasetAuthor.builder()
                                .authorName("test_authorName")
                                .authorAffiliation("test_authorAffiliation")
                                .authorIdentifierScheme("ORCID")
                                .authorIdentifier("0000-0001-5000-0008")
                                .build()
                )
                .description(
                        DatasetDescription.builder()
                                .description("Test")
                                .build()
                )
                .subject("Agricultural Sciences")
                .topicClassification(
                        DatasetTopicClassification.builder()
                                .topicClassValue(datasetDTO.topic)
                                .topicClassVocab("a topic vocab")
                                .topicClassVocabURI(new URI("https://www.vocab.org"))
                                .build()
                )
                .languages(Arrays.asList("French"))
                .depositor("Besombes, Gabriel")
                .productionDate(datasetDTO.productionDate)
                .build();
        Identifier datasetId = new Identifier();
        try {
            datasetId = this.dataverseOps.createDataset(facade, this.dataverseAlias);
        } catch(Exception e){
            e.printStackTrace();
        }
         //wrong "createDataset" used?
        return datasetId;
    }

    // TODO : I could make a getDataverseMetadataBlocks or get DataverseFields for example
}
