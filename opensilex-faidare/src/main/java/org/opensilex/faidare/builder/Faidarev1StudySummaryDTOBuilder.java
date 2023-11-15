package org.opensilex.faidare.builder;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.faidare.model.Faidarev1StudySummaryDTO;

public class Faidarev1StudySummaryDTOBuilder {

    public Faidarev1StudySummaryDTOBuilder() {
    }

    public Faidarev1StudySummaryDTO fromModel(ExperimentModel experimentModel){
        Faidarev1StudySummaryDTO dto = new Faidarev1StudySummaryDTO();
        dto.setStudyDbId(experimentModel.getUri().toString())
                .setStudyName(experimentModel.getName());
        return dto;
    }
}
