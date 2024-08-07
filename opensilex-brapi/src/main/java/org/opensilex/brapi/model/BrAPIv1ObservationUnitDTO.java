//******************************************************************************
//                          BrAPIv1ObservationUnitDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.opensilex.brapi.responses.BrAPIv1AccessionWarning;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1ObservationUnitDTO {

    private enum PositionType {
        LONGITUDE("LONGITUDE"),
        LATITUDE("LATITUDE"),
        PLANTED_ROW("PLANTED_ROW"),
        PLANTED_INDIVIDUAl("PLANTED_INDIVIDUAl"),
        GRID_ROW("GRID_ROW"),
        GRID_COL("GRID_COL"),
        MEASURED_ROW("MEASURED_ROW"),
        MEASURED_COL("MEASURED_COL");

        public final String label;

        PositionType(String label){
            this.label = label;
        }
        @Override
        public String toString() {
            return this.label;
        }
    }
    private String blockNumber;         
    private String entryNumber;
    private String entryType;
    private String germplasmDbId;
    private String germplasmName;
    private String locationDbId;
    private String locationName;
    private String observationLevel;
    private String observationUnitDbId;
    private List<BrAPIv1ObservationUnitXrefDTO> observationUnitXref;
    private List<BrAPIv1ObservationSummaryDTO> observations;
    private String pedigree;
    private String plantNumber;
    private String plotNumber;
    private String positionCoordinateX;
    private String positionCoordinateXType; // Use PositionType
    private String positionCoordinateY;
    private String positionCoordinateYType; // Use PositionType
    private String programDbId;
    private String programName;
    private String replicate;
    private String studyDbId;
    private String studyName;
    private List<BrAPIv1ObservationUnitTreatmentDTO> treatments;
    private String trialDbId;
    private String trialName;

    public String getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(String blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getEntryNumber() {
        return entryNumber;
    }

    public void setEntryNumber(String entryNumber) {
        this.entryNumber = entryNumber;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

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

    public String getObservationLevel() {
        return observationLevel;
    }

    public void setObservationLevel(String observationLevel) {
        this.observationLevel = observationLevel;
    }

    public String getObservationUnitDbId() {
        return observationUnitDbId;
    }

    public void setObservationUnitDbId(String observationUnitDbId) {
        this.observationUnitDbId = observationUnitDbId;
    }

    public List<BrAPIv1ObservationUnitXrefDTO> getObservationUnitXref() {
        return observationUnitXref;
    }

    public void setObservationUnitXref(List<BrAPIv1ObservationUnitXrefDTO> observationUnitXref) {
        this.observationUnitXref = observationUnitXref;
    }

    public List<BrAPIv1ObservationSummaryDTO> getObservations() {
        return observations;
    }

    public void setObservations(List<BrAPIv1ObservationSummaryDTO> observations) {
        this.observations = observations;
    }

    public String getPlantNumber() {
        return plantNumber;
    }

    public void setPlantNumber(String plantNumber) {
        this.plantNumber = plantNumber;
    }

    public String getPlotNumber() {
        return plotNumber;
    }

    public void setPlotNumber(String plotNumber) {
        this.plotNumber = plotNumber;
    }

    public String getPositionCoordinateX() {
        return positionCoordinateX;
    }

    public void setPositionCoordinateX(String positionCoordinateX) {
        this.positionCoordinateX = positionCoordinateX;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPositionCoordinateXType() {
        return positionCoordinateXType;
    }

    public void setPositionCoordinateXType(PositionType positionCoordinateXType) {
        this.positionCoordinateXType = positionCoordinateXType.toString();
    }

    public String getPositionCoordinateY() {
        return positionCoordinateY;
    }

    public void setPositionCoordinateY(String positionCoordinateY) {
        this.positionCoordinateY = positionCoordinateY;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPositionCoordinateYType() {
        return positionCoordinateYType;
    }

    public void setPositionCoordinateYType(PositionType positionCoordinateYType) {
        this.positionCoordinateYType = positionCoordinateYType.toString();
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getReplicate() {
        return replicate;
    }

    public void setReplicate(String replicate) {
        this.replicate = replicate;
    }

    public String getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(String studyDbId) {
        this.studyDbId = studyDbId;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public List<BrAPIv1ObservationUnitTreatmentDTO> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<BrAPIv1ObservationUnitTreatmentDTO> treatments) {
        this.treatments = treatments;
    }

    public String getLocationDbId() {
        return locationDbId;
    }

    public void setLocationDbId(String locationDbId) {
        this.locationDbId = locationDbId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getPedigree() {
        return pedigree;
    }

    public void setPedigree(String pedigree) {
        this.pedigree = pedigree;
    }

    public void setPositionCoordinateXType(String positionCoordinateXType) {
        this.positionCoordinateXType = positionCoordinateXType;
    }

    public void setPositionCoordinateYType(String positionCoordinateYType) {
        this.positionCoordinateYType = positionCoordinateYType;
    }

    public String getProgramDbId() {
        return programDbId;
    }

    public void setProgramDbId(String programDbId) {
        this.programDbId = programDbId;
    }

    public String getTrialDbId() {
        return trialDbId;
    }

    public void setTrialDbId(String trialDbId) {
        this.trialDbId = trialDbId;
    }

    public String getTrialName() {
        return trialName;
    }

    public void setTrialName(String trialName) {
        this.trialName = trialName;
    }

    public static BrAPIv1ObservationUnitDTO fromModel(
            ScientificObjectModel model, 
            FacilityDAO facilityDAO, 
            AccountModel currentUser, 
            DataDAO dataDAO, 
            ExperimentModel experimentModel, 
            OntologyDAO ontologyDAO,
            MoveLogic moveEventLogic,
            GeospatialDAO geospatialDAO,
            GermplasmDAO germplasmDAO,
            SPARQLService sparql
    ) throws Exception {
        BrAPIv1ObservationUnitDTO observationUnit = new BrAPIv1ObservationUnitDTO();

        if (model.getUri() != null) {
            observationUnit.setObservationUnitDbId(model.getUri().toString());
        }

        if (!model.getTypeLabel().toString().isEmpty()){
            observationUnit.setObservationLevel(model.getTypeLabel().toString());
        }

        Set<SPARQLModelRelation> hosts = model.getRelations(Oeso.isHosted).collect(Collectors.toSet());
        if (hosts.size() == 1){
            for (SPARQLModelRelation host:hosts) {
                observationUnit.setLocationDbId(host.getValue());
                observationUnit.setLocationName(facilityDAO.get(new URI(host.getValue()), currentUser).getName());
            }
        }

        Set<SPARQLModelRelation> germplasms = model.getRelations(Oeso.hasGermplasm).collect(Collectors.toSet());
        if (germplasms.size() == 1){
            for (SPARQLModelRelation germplasm:germplasms) {
                GermplasmModel germplasmModel = germplasmDAO.get(new URI(germplasm.getValue()), currentUser, false);
                if (Objects.equals(germplasmModel.getType(), BrAPIv1AccessionWarning.ACCESSION_URI)) {
                    observationUnit.setGermplasmDbId(germplasmModel.getUri().toString());
                    observationUnit.setGermplasmName(germplasmModel.getName());
                }
            }
        }

        ListWithPagination<DataModel> objectData = dataDAO.search(
                currentUser,
                Collections.singletonList(experimentModel.getUri()),
                Collections.singletonList(model.getUri()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0
        );

        if (!objectData.getList().isEmpty()){
            List<BrAPIv1ObservationSummaryDTO> observations = new ArrayList<>();
            for (DataModel data : objectData.getList()) {
                observations.add(BrAPIv1ObservationSummaryDTO.fromModel(data, ontologyDAO, currentUser));
            }
            observationUnit.setObservations(observations);
        }


        // null isn't allowed for these so by default it is set to long/lat
        observationUnit.setPositionCoordinateXType(PositionType.LONGITUDE);
        observationUnit.setPositionCoordinateYType(PositionType.LATITUDE);

        GeospatialModel objectGeometryModel = geospatialDAO.getGeometryByURI(model.getUri(), experimentModel.getUri());
        //if the Object has a geometry take its centroid coordinates as long/lat
        if (objectGeometryModel != null && objectGeometryModel.getGeometry() != null && !objectGeometryModel.getGeometry().toString().isEmpty()) {
            org.locationtech.jts.geom.Geometry objectJtsGeometry = new GeoJsonReader().read(objectGeometryModel.getGeometry().toJson());

            if (!objectJtsGeometry.isEmpty()){

                Point centroid = objectJtsGeometry.getCentroid();
                observationUnit.setPositionCoordinateX(Double.toString(centroid.getX()));
                observationUnit.setPositionCoordinateY(Double.toString(centroid.getY()));
            }
        } else if (moveEventLogic.countForTarget(model.getUri()) >= 1){
            MoveModel moveModel = moveEventLogic.getLastMoveEvent(model.getUri());
            PositionModel movePosition = moveEventLogic.getPosition(model.getUri(), moveModel.getUri());

            if (Objects.nonNull(movePosition)){
                //if the Object has a move with a geometry take its centroid coordinates as long/lat
                if (Objects.nonNull(movePosition.getCoordinates())) {
                    Geometry moveJtsGeometry = new GeoJsonReader().read(movePosition.getCoordinates().toJson());
                    Point centroid = moveJtsGeometry.getCentroid();
                    observationUnit.setPositionCoordinateX(Double.toString(centroid.getX()));
                    observationUnit.setPositionCoordinateY(Double.toString(centroid.getY()));
                } else if (Objects.nonNull(movePosition.getX()) || Objects.nonNull(movePosition.getY())) {
                    //if the Object has a move with a position take its X/Y coordinates as grid coordinates
                    if (Objects.nonNull(movePosition.getX())) {
                        observationUnit.setPositionCoordinateX(movePosition.getX());
                    }
                    if (Objects.nonNull(movePosition.getY())) {
                        observationUnit.setPositionCoordinateY(movePosition.getY());
                    }
                    observationUnit.setPositionCoordinateXType(PositionType.GRID_ROW);
                    observationUnit.setPositionCoordinateYType(PositionType.GRID_COL);
                }
            } else if (Objects.nonNull(moveModel.getTo())) {
                //if the Object has a move with a destination facility take the centroid of that facility's geometry
                FacilityModel destinationFacility = moveModel.getTo();
                GeospatialModel destinationFacilityGeometryModel = geospatialDAO.getGeometryByURI(
                        destinationFacility.getUri(),
                        sparql.getDefaultGraphURI(FacilityModel.class)
                );
                Geometry facilityJtsGeometry =  new GeoJsonReader().read(destinationFacilityGeometryModel.getGeometry().toJson());
                if (Objects.nonNull(facilityJtsGeometry)) {
                    Point centroid = facilityJtsGeometry.getCentroid();
                    observationUnit.setPositionCoordinateX(Double.toString(centroid.getX()));
                    observationUnit.setPositionCoordinateY(Double.toString(centroid.getY()));
                }
            }
        }

        observationUnit.setStudyName(experimentModel.getName());
        observationUnit.setStudyDbId(experimentModel.getUri().toString());

        if (Objects.nonNull(model.getFactorLevels()) && model.getFactorLevels().size()>0){
            List<BrAPIv1ObservationUnitTreatmentDTO> unitTreatments = new ArrayList<>();
            List<FactorModel> experimentFactors = experimentModel.getFactors();
            List<URI> objectFactorLevels = model.getFactorLevels().stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());
            for (FactorModel factorModel : experimentFactors) {
                for (FactorLevelModel factorLevelModel : factorModel.getFactorLevels()) {
                    if (objectFactorLevels.contains(factorLevelModel.getUri())){
                        BrAPIv1ObservationUnitTreatmentDTO unitTreatment = new BrAPIv1ObservationUnitTreatmentDTO();
                        unitTreatment.setFactor(factorModel.getName());
                        unitTreatment.setModality(factorLevelModel.getName());
                        unitTreatments.add(unitTreatment);
                    }
                }
            }

            observationUnit.setTreatments(unitTreatments);
        }
        
        return observationUnit;
    }
}
