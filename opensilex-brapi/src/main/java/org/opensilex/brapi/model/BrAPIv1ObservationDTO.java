//******************************************************************************
//                          BrAPIv1ObservationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.brapi.responses.BrAPIv1AccessionWarning;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1ObservationDTO {

    private String germplasmDbId;
    private String germplasmName;
    private String observationDbId; 
    private String observationLevel;
    private String observationTimeStamp;
    private String observationUnitDbId;
    private String observationUnitName;
    private String observationVariableDbId;
    private String observationVariableName;
    private String operator;
    private BrAPIv1SeasonDTO season;
    private String studyDbId;
    private String uploadedBy;
    private String value;

    public String getGermplasmDbId() {
        return germplasmDbId;
    }

    public void setGermplasmDbId(String germplasmDbId) {
        this.germplasmDbId = germplasmDbId;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public void setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
    }

    public String getObservationDbId() {
        return observationDbId;
    }

    public void setObservationDbId(String observationDbId) {
        this.observationDbId = observationDbId;
    }

    public String getObservationLevel() {
        return observationLevel;
    }

    public void setObservationLevel(String observationLevel) {
        this.observationLevel = observationLevel;
    }

    public String getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public void setObservationTimeStamp(String observationTimeStamp) {
        this.observationTimeStamp = observationTimeStamp;
    }

    public String getObservationUnitDbId() {
        return observationUnitDbId;
    }

    public void setObservationUnitDbId(String observationUnitDbId) {
        this.observationUnitDbId = observationUnitDbId;
    }

    public String getObservationUnitName() {
        return observationUnitName;
    }

    public void setObservationUnitName(String observationUnitName) {
        this.observationUnitName = observationUnitName;
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

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BrAPIv1SeasonDTO getSeason() {
        return season;
    }

    public void setSeason(BrAPIv1SeasonDTO season) {
        this.season = season;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BrAPIv1ObservationDTO extractFromModel(DataModel dataModel, ExperimentModel expeModel, OntologyDAO ontologyDAO, SPARQLService sparql, AccountModel currentUser, ScientificObjectDAO scientificObjectDAO, GermplasmDAO germplasmDAO) throws Exception {

        Node experimentGraph = NodeFactory.createURI(expeModel.getUri().toString());

        if (dataModel.getUri() != null) {
            this.setObservationDbId(dataModel.getUri().toString());
        }

        if (dataModel.getDate() != null) {
            this.setObservationTimeStamp(dataModel.getDate().toString());
        }
        URI targetURI = dataModel.getTarget();
        if (targetURI != null) {
            this.setObservationUnitDbId(targetURI.toString());

            List<String> targetNameInExperiment = sparql.searchPrimitives(experimentGraph, targetURI, RDFS.label, String.class);
            if (targetNameInExperiment.isEmpty()) {
                this.setObservationUnitName(ontologyDAO.getURILabel(targetURI, currentUser.getLanguage()));
            } else if (targetNameInExperiment.size() == 1){
                // For now a target has one and only one label in a context (here experiment)
                this.setObservationUnitName(targetNameInExperiment.get(0));
            }

            List<URI> targetTypes = sparql.getRdfTypes(targetURI,null);
            this.setObservationLevel(ontologyDAO.getURILabel(targetTypes.get(0), currentUser.getLanguage()));
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

        if (expeModel.getUri() != null) {
            this.setStudyDbId(expeModel.getUri().toString());
        }

        if (dataModel.getValue() != null) {
            this.setValue(dataModel.getValue().toString());
        }
        List<SPARQLNamedResourceModel> uriLabels = ontologyDAO.getURILabels(List.of(dataModel.getTarget()), currentUser.getLanguage(), expeModel.getUri());
        if (!uriLabels.isEmpty() && SPARQLModule.getOntologyStoreInstance().classExist(
                uriLabels.get(0).getType(), new URI(Oeso.ScientificObject.getURI())
        )){
            ScientificObjectModel objectModel = scientificObjectDAO.getObjectByURI(dataModel.getTarget(), expeModel.getUri(), currentUser.getLanguage());
            List<SPARQLModelRelation> germplasms = objectModel.getRelations(Oeso.hasGermplasm).distinct().collect(Collectors.toList());
            if (germplasms.size() >= 1){
                GermplasmModel germplasmModel = germplasmDAO.get(new URI(germplasms.get(0).getValue()), currentUser, false);
                if (SPARQLDeserializers.compareURIs(germplasmModel.getType(), BrAPIv1AccessionWarning.ACCESSION_URI)) {
                    this.setGermplasmDbId(germplasmModel.getUri().toString());
                    this.setGermplasmName(germplasmModel.getName());
                }
            }
        }

        return this;
    }

    public static BrAPIv1ObservationDTO fromModel(DataModel dataModel, ExperimentModel expeModel, OntologyDAO ontologyDAO, SPARQLService sparql, AccountModel currentUser, ScientificObjectDAO scientificObjectDAO, GermplasmDAO germplasmDAO) throws Exception {
        BrAPIv1ObservationDTO observation = new BrAPIv1ObservationDTO();
        return observation.extractFromModel(dataModel, expeModel, ontologyDAO, sparql, currentUser, scientificObjectDAO, germplasmDAO);
    }
}
