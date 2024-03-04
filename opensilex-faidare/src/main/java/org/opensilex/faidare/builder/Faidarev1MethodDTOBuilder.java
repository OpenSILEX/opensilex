package org.opensilex.faidare.builder;

import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.faidare.model.Faidarev1MethodDTO;

import java.util.Objects;

public class Faidarev1MethodDTOBuilder {

    public Faidarev1MethodDTOBuilder() {
    }

    public Faidarev1MethodDTO fromModel(MethodModel methodModel){
        Faidarev1MethodDTO dto = new Faidarev1MethodDTO();
        dto.setMethodDbId(methodModel.getUri().toString())
                .setName(methodModel.getName())
                .setDescription(Objects.toString(methodModel.getDescription(), null));

        return dto;
    }
}
