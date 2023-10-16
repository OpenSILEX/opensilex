//******************************************************************************
//                          BrAPIv1ObservationSummaryDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.data.dal.DataModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
class BrAPIv1ObservationSummaryDTO {
    private String collector;
    private String observationDbId;
    private String observationTimeStamp;
    private String observationVariableDbId;
    private String observationVariableName;
    private BrAPIv1SeasonDTO season;
    private String value;

    public String getCollector() {
        return collector;
    }

    public void setCollector(String collector) {
        this.collector = collector;
    }

    public String getObservationDbId() {
        return observationDbId;
    }

    public void setObservationDbId(String observationDbId) {
        this.observationDbId = observationDbId;
    }

    public String getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public void setObservationTimeStamp(String observationTimeStamp) {
        this.observationTimeStamp = observationTimeStamp;
    }

    public String getObservationVariableDbId() {
        return observationVariableDbId;
    }

    public void setObservationVariableDbId(String observationVariableDbId) {
        this.observationVariableDbId = observationVariableDbId;
    }

    public String getObservationVariableName() {
        return observationVariableName;
    }

    public void setObservationVariableName(String observationVariableName) {
        this.observationVariableName = observationVariableName;
    }

    public BrAPIv1SeasonDTO getSeason() {
        return season;
    }

    public void setSeason(BrAPIv1SeasonDTO season) {
        this.season = season;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BrAPIv1ObservationSummaryDTO extractFromModel(DataModel dataModel, OntologyDAO ontologyDAO, AccountModel currentUser) throws Exception {

        if (dataModel.getUri() != null) {
            this.setObservationDbId(dataModel.getUri().toString());
        }

        if (dataModel.getDate() != null) {
            this.setObservationTimeStamp(dataModel.getDate().toString());
        }

        if (dataModel.getVariable() != null) {
            this.setObservationVariableDbId(dataModel.getVariable().toString());
            this.setObservationVariableName(ontologyDAO.getURILabel(dataModel.getVariable(), currentUser.getLanguage()));
        }

        if (dataModel.getDate() != null){
            BrAPIv1SeasonDTO season = new BrAPIv1SeasonDTO();
            season.setYear(String.valueOf(LocalDateTime.from(dataModel.getDate().atZone(ZoneId.systemDefault())).getYear()));
            this.setSeason(season);
        }

        if (dataModel.getValue() != null) {
            this.setValue(dataModel.getValue().toString());
        }

        return this;
    }

    public static BrAPIv1ObservationSummaryDTO fromModel(DataModel dataModel, OntologyDAO ontologyDAO, AccountModel currentUser) throws Exception {
        BrAPIv1ObservationSummaryDTO observation = new BrAPIv1ObservationSummaryDTO();
        return observation.extractFromModel(dataModel, ontologyDAO, currentUser);
    }
}
