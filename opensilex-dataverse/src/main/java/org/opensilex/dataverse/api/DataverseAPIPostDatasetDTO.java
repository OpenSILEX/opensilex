//******************************************************************************
//                          ExperimentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.dataverse.api;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.PersonDTO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.rest.validation.model.OpenSilexLocale;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * @author Gabriel Besombes
 */
public class DataverseAPIPostDatasetDTO {

    public AccountModel getCurrentAccount() {
        return currentAccount;
    }

    public String getDatasetTitle() {
        return datasetTitle;
    }

    public String getTopic() {
        return topic;
    }

    public Date getProductionDate() {
        return productionDate;
    }

    public String getDescription() {
        return description;
    }

    public List<PersonModel> getDatasetAuthors() {
        return datasetAuthors;
    }

    public List<PersonModel> getDatasetContacts() {
        return datasetContacts;
    }

    public OpenSilexLocale getDatasetLanguage() {
        return datasetLanguage;
    }

    public OpenSilexLocale getDatasetMetadataLanguage() {
        return datasetMetadataLanguage;
    }

    private AccountModel currentAccount;
    private String datasetTitle;
    private String topic;
    // Must use this deprecated class because the dataverse client expects this type
    private Date productionDate;
    private String description;
    private List<PersonModel> datasetAuthors;
    private List<PersonModel> datasetContacts;
    private OpenSilexLocale datasetLanguage;
    private OpenSilexLocale datasetMetadataLanguage;

    public DataverseAPIPostDatasetDTO(ExperimentModel experimentModel, AccountModel currentAccount, String datasetTitle, List<PersonModel> datasetAuthors, List<PersonModel> datasetContacts, OpenSilexLocale datasetLanguage, OpenSilexLocale datasetMetadataLanguage, Date productionDate){
        this.datasetTitle = datasetTitle;
        this.description = experimentModel.getDescription();
        this.topic = experimentModel.getObjective();
        this.productionDate = productionDate;
        this.currentAccount = currentAccount;
        this.datasetAuthors = datasetAuthors;
        this.datasetContacts = datasetContacts;
        this.datasetLanguage = datasetLanguage;
        this.datasetMetadataLanguage = datasetMetadataLanguage;
    }
}
