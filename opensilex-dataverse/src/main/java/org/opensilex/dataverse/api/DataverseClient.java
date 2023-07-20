package org.opensilex.dataverse.api;

import com.researchspace.dataverse.api.v1.DataverseConfig;
import com.researchspace.dataverse.entities.Identifier;
import com.researchspace.dataverse.entities.facade.DatasetAuthor;
import com.researchspace.dataverse.entities.facade.DatasetContact;
import com.researchspace.dataverse.entities.facade.DatasetDescription;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import org.opensilex.server.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.Provider;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.apache.http.util.TextUtils.isBlank;

/**
 * Client for the dataverse API
 * Uses the java dataverse client : <a href="https://github.com/IQSS/dataverse-client-java">IQSS Dataverse Client</a>
 */
@Provider
public class DataverseClient {

    protected DataverseAPIImplOpensilex dataverseExternalAPI = new DataverseAPIImplOpensilex();
    protected DataverseOperationsImplV1Opensilex dataverseOps;

    protected String dataverseAlias;

    public static final Logger LOGGER = LoggerFactory.getLogger(DataverseClient.class);

    public DataverseClient(URL dataverseBasePath, String dataverseAlias, String externalAPIKey) {
        this.dataverseAlias = dataverseAlias;
        this.dataverseExternalAPI.configure(new DataverseConfig(dataverseBasePath, externalAPIKey, this.dataverseAlias));
        this.dataverseOps = this.dataverseExternalAPI.getDataverseOperations();
    }

    /**
     * Creates a dataset on the given this.dataverse
     * @return a string representation of the created dataset's DOI
     * @see <a href=https://guides.dataverse.org/en/latest/api/native-api.html>Dataverse's Native API</a>
     */
    public Identifier createADataset(DataverseAPIPostDatasetDTO datasetDTO) {

        DatasetFacade facade = createAFacade(datasetDTO);

        Identifier datasetId = this.dataverseOps.createDataset(facade, this.dataverseAlias, datasetDTO.getDatasetMetadataLanguage().toLanguageTag());
        LOGGER.debug("Dataset created : " + datasetId);
        return datasetId;
    }

    public DatasetFacade createAFacade(DataverseAPIPostDatasetDTO datasetDTO){
        String datasetDescription;
        if (!isBlank(datasetDTO.getDescription())){
            datasetDescription = datasetDTO.getDescription();
        } else {
            datasetDescription = datasetDTO.getTopic();
        }
        List<DatasetContact> contacts = datasetDTO.getDatasetContacts().stream().map(contact -> {
            if (contact.getEmail() == null){
                throw new BadRequestException("Dataset contacts must have an email and '" + contact.getUri() + "' doesn't have one");
            }
            return DatasetContact.builder()
                    .datasetContactEmail(contact.getEmail().toString())
                    .datasetContactName(contact.getFirstName() + " " + contact.getLastName().toUpperCase())
                    .build();
        }).collect(Collectors.toList());
        DatasetFacade.DatasetFacadeBuilder facadeBuilder = DatasetFacade.builder()
                .title(datasetDTO.getDatasetTitle())
                .productionDate(datasetDTO.getProductionDate())
                .contacts(
                        contacts
                )
                .authors(
                        datasetDTO.getDatasetAuthors().stream().map(author -> {
                            if (author.getLastName() == null || author.getFirstName() == null){
                                throw new BadRequestException("Dataset authors must have a full name with a first and last name '" + author.getUri() + "' doesn't");
                            }
                            return DatasetAuthor.builder()
                                    .authorName(author.getFirstName() + " " + author.getLastName().toUpperCase())
                                    .build();
                        }).collect(Collectors.toList())
                )
                .description(
                        DatasetDescription.builder()
                                .description(datasetDescription)
                                .build()
                )
                .subject("Agricultural Sciences")
                .languages(Collections.singletonList(datasetDTO.getDatasetLanguage().getDisplayLanguage(Locale.ENGLISH)));

        if(datasetDTO.getCurrentAccount().getLinkedPerson() != null){
            facadeBuilder.depositor(datasetDTO.getCurrentAccount().getLinkedPerson().getFirstName() + " " + datasetDTO.getCurrentAccount().getLinkedPerson().getLastName().toUpperCase());
        }else {
            facadeBuilder.depositor(datasetDTO.getCurrentAccount().getEmail().toString());
        }
        DatasetFacade facade = facadeBuilder.build();

        LOGGER.debug("Facade created : " + facade);
        return facade;
    }
}
