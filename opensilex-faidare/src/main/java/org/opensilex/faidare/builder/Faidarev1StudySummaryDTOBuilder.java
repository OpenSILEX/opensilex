package org.opensilex.faidare.builder;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.faidare.model.Faidarev1StudySummaryDTO;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.List;

public class Faidarev1StudySummaryDTOBuilder {

    public Faidarev1StudySummaryDTOBuilder() {
    }

    public Faidarev1StudySummaryDTO fromModel(ExperimentModel experimentModel){
        Faidarev1StudySummaryDTO dto = new Faidarev1StudySummaryDTO();
        dto.setStudyDbId(SPARQLDeserializers.getExpandedURI(experimentModel.getUri()))
                .setStudyName(experimentModel.getName());

        List<FacilityModel> facilitiesList = experimentModel.getFacilities();
        if (!facilitiesList.isEmpty()){
            FacilityModel facility = facilitiesList.get(0);
            dto.setLocationDbId(SPARQLDeserializers.getExpandedURI(facility.getUri()))
                    .setLocationName(facility.getName());
        }

        return dto;
    }
}
